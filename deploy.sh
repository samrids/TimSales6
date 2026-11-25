#!/usr/bin/env bash
#
# deploy.sh — incremental FTP/FTPS deploy for the Run 2000km app.
#
# The production host (ftp.awarasoft.com) is FTP-only shared hosting: port 22 is
# closed, port 21 speaks FTP with explicit TLS on an untrusted certificate. There
# is no remote shell, so releases/symlinks/rsync are not available — this script
# uploads changed files in place and keeps a local backup of whatever it
# overwrites so a deploy can be undone.
#
# Only files whose SHA-1 differs from the last successful deploy are uploaded.
# Nothing is ever deleted on the server.
#
# Usage: ./deploy.sh { --test | --dry-run | --deploy | --rollback | --list-backups
#                     | --encrypt-password }

set -euo pipefail

# ---------------------------------------------------------
# 1. Constants & UI
# ---------------------------------------------------------
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
step()  { echo -e "${YELLOW}[STEP]${NC} $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }
die()   { error "$*"; exit 1; }

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

ENV_FILE=".env_deploy"
MANIFEST=".deploy-manifest"
BACKUP_ROOT=".deploy-backups"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"

# Never upload these, whatever EXCLUDE_LIST says: secrets, VCS internals,
# dependency trees and live databases. Stack-agnostic — safe for any project.
readonly PROTECTED_EXCLUDES=(
    .git .gitignore .gitattributes .claude .vscode .idea
    ".env" ".env.*" ".env_deploy*"
    "${MANIFEST}*" "$BACKUP_ROOT"
    deploy.sh deploy.bat
    node_modules __pycache__ .venv venv vendor
    "*.log" "*.db" "*.db-journal" "*.db-wal" "*.db-shm"
    "*.sqlite" "*.sqlite3" "*.py[cod]"
    "sess_*"
    Thumbs.db .DS_Store Desktop.ini "*.swp" "*.bak" "*.tmp"
)

# Dev material that rarely belongs on a live server. Generic across stacks;
# add project-specific paths via EXCLUDE_LIST in .env_deploy.
readonly DEFAULT_EXCLUDES=(
    AGENTS.md CLAUDE.md
    .github .gitlab-ci.yml .editorconfig
    tests test __tests__ .pytest_cache
    package-lock.json yarn.lock composer.lock
    Dockerfile docker-compose.yml Makefile
)

# ---------------------------------------------------------
# 2. Load & validate configuration
# ---------------------------------------------------------
[ -f "$ENV_FILE" ] || die "Configuration file '${ENV_FILE}' not found."

# shellcheck disable=SC1090
set -a; . "./$ENV_FILE"; set +a

# Accept the legacy SSH_* names from the old rsync-based script.
FTP_PASSWORD="${FTP_PASSWORD:-${SSH_PASSWORD:-}}"
FTP_PORT="${FTP_PORT:-${SSH_PORT:-21}}"
FTP_TLS="${FTP_TLS:-yes}"           # explicit AUTH TLS (FTPS)
FTP_INSECURE="${FTP_INSECURE:-yes}" # this host serves an untrusted certificate
CONNECT_TIMEOUT="${CONNECT_TIMEOUT:-20}"
MAX_TIME="${MAX_TIME:-300}"
HEALTH_URL="${HEALTH_URL:-}"

# --- Project-agnostic build/check settings -----------------
# SOURCE_DIR     what to upload: "." for a plain PHP/static site, or a build
#                output directory such as dist / build / public
# PRE_DEPLOY_CMD any shell command that must succeed first (npm run build,
#                npm test, pytest, composer install --no-dev, ...)
# PHP_LINT       auto = lint .php files when php is available; yes = require
#                php and fail without it; no = never lint
# PHP56_CHECK    yes = reject PHP 7+ syntax (?? and random_bytes). Only for
#                projects on a PHP 5.6 host — off by default.
SOURCE_DIR="${SOURCE_DIR:-.}"
PRE_DEPLOY_CMD="${PRE_DEPLOY_CMD:-}"
PHP_LINT="${PHP_LINT:-auto}"
PHP56_CHECK="${PHP56_CHECK:-no}"

# STAMP_FILE     after a deploy that uploads anything, write today's date
#                (YYYY-MM-DD) to this path on the server, for an app that shows
#                when it was last updated. Empty = off. The file is generated,
#                never taken from the working copy, and rollback restores it.
STAMP_FILE="${STAMP_FILE:-}"

# --- Encrypted password (Windows DPAPI) -------------------
# FTP_PASSWORD_DPAPI holds the password encrypted with Windows DPAPI, so
# .env_deploy carries no plaintext. Only the Windows user who encrypted it, on
# the same PC, can decrypt it; a copied or leaked file is useless elsewhere.
# Needs powershell.exe, so WSL or Git Bash on Windows. Create it with
# ./deploy.sh --encrypt-password. A plain FTP_PASSWORD, if set, wins.
#
# Both directions pass data over stdin as base64: never on a command line,
# where the process list would show it, and safe for non-ASCII passwords.
FTP_PASSWORD_DPAPI="${FTP_PASSWORD_DPAPI:-}"

_ps() { powershell.exe -NoProfile -NonInteractive -Command "$1" 2>/dev/null | tr -d '\r\n'; }

dpapi_encrypt() {
    local enc
    enc="$(printf '%s' "$1" | base64 -w0 | _ps '$ErrorActionPreference="Stop"; $b = [Console]::In.ReadToEnd().Trim(); $p = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b)); ConvertFrom-SecureString -SecureString (ConvertTo-SecureString -String $p -AsPlainText -Force)')" || return 1
    [[ "$enc" =~ ^[0-9a-fA-F]+$ ]] || return 1
    printf '%s' "$enc"
}

dpapi_decrypt() {
    local b64
    b64="$(printf '%s' "$1" | _ps '$ErrorActionPreference="Stop"; $e = [Console]::In.ReadToEnd().Trim(); $s = ConvertTo-SecureString -String $e; $m = [Runtime.InteropServices.Marshal]; $b = $m::SecureStringToBSTR($s); try { $p = $m::PtrToStringBSTR($b) } finally { $m::ZeroFreeBSTR($b) }; [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($p))')" || return 1
    [ -n "$b64" ] || return 1
    printf '%s' "$b64" | base64 -d
}

# Replaces any plaintext password line in .env_deploy with FTP_PASSWORD_DPAPI.
# Offers to encrypt the password already in the file, so nothing is retyped;
# otherwise asks for it twice, without echo. The file is only rewritten after
# the new value has been decrypted back and matched.
encrypt_password() {
    command -v powershell.exe >/dev/null 2>&1 \
        || die "powershell.exe not found — DPAPI needs Windows (WSL or Git Bash)."
    [ -t 0 ] || die "--encrypt-password asks for input; run it in a terminal."

    local pw="" pw2="" answer="" enc tmp
    if [ -n "$FTP_PASSWORD" ] && grep -qE '^(export )?(FTP|SSH)_PASSWORD=' "$ENV_FILE"; then
        read -r -p "Encrypt the password already in ${ENV_FILE}? [Y/n] " answer
        case "$answer" in [nN]*) ;; *) pw="$FTP_PASSWORD" ;; esac
    fi
    if [ -z "$pw" ]; then
        read -r -s -p "FTP password: " pw; echo
        read -r -s -p "Same password again: " pw2; echo
        [ -n "$pw" ] || die "Empty password — ${ENV_FILE} was not changed."
        [ "$pw" = "$pw2" ] || die "The two entries differ — ${ENV_FILE} was not changed."
    fi

    step "Encrypting with Windows DPAPI..."
    enc="$(dpapi_encrypt "$pw")" || die "Encryption failed — ${ENV_FILE} was not changed."
    [ "$(dpapi_decrypt "$enc")" = "$pw" ] \
        || die "Encrypted value did not decrypt back — ${ENV_FILE} was not changed."

    tmp="${ENV_FILE}.tmp.$$"
    (
        umask 077
        {
            grep -vE '^(export )?(FTP|SSH)_PASSWORD(_DPAPI)?=|^# FTP password, encrypted with Windows DPAPI|^# Only this Windows user on this PC' "$ENV_FILE" || true
            printf '# FTP password, encrypted with Windows DPAPI by ./deploy.sh --encrypt-password.\n'
            printf '# Only this Windows user on this PC can decrypt it. To change it, run that again.\n'
            printf 'FTP_PASSWORD_DPAPI="%s"\n' "$enc"
        } > "$tmp"
    )
    mv "$tmp" "$ENV_FILE"
    info "Saved FTP_PASSWORD_DPAPI to ${ENV_FILE}; no plaintext password is left in it."
    info "Check it with: ./deploy.sh --dry-run"
}

if [ "${1:-}" = "--encrypt-password" ]; then
    encrypt_password
    exit 0
fi

if [ -z "$FTP_PASSWORD" ] && [ -n "$FTP_PASSWORD_DPAPI" ]; then
    command -v powershell.exe >/dev/null 2>&1 \
        || die "FTP_PASSWORD_DPAPI is set, but powershell.exe is not available to decrypt it."
    FTP_PASSWORD="$(dpapi_decrypt "$FTP_PASSWORD_DPAPI")" \
        || die "Could not decrypt FTP_PASSWORD_DPAPI — it only works for the Windows user and PC that created it. Run ./deploy.sh --encrypt-password again."
fi

for var in SERVER_USER FTP_PASSWORD SERVER_IP APP_DIR; do
    [ -n "${!var:-}" ] || die "Required setting '${var}' is missing or empty in ${ENV_FILE}."
done

SOURCE_DIR="${SOURCE_DIR%/}"
[ -n "$SOURCE_DIR" ] || SOURCE_DIR="."
APP_DIR="${APP_DIR%/}"
TARGET="${SERVER_IP}:${FTP_PORT}${APP_DIR}"

command -v curl >/dev/null 2>&1 || die "curl is required but not installed."

if [ "$FTP_TLS" != "yes" ]; then
    warn "FTP_TLS=no — credentials and files will be sent in cleartext."
fi

# Build the exclude set: protected + defaults + user list (whitespace separated).
EXCLUDES=("${PROTECTED_EXCLUDES[@]}" "${DEFAULT_EXCLUDES[@]}")
if [ -n "${EXCLUDE_LIST:-}" ]; then
    # Deliberately unquoted: EXCLUDE_LIST is a space-separated list in .env_deploy.
    for item in $EXCLUDE_LIST; do
        EXCLUDES+=("$item")
    done
fi
# A local file of the same name must not overwrite the generated stamp.
[ -n "$STAMP_FILE" ] && EXCLUDES+=("$STAMP_FILE")

# ---------------------------------------------------------
# 3. Credentials & cleanup
# ---------------------------------------------------------
# Credentials go in a 0600 curl config file rather than on the command line,
# where they would be visible in the process list.
umask 077
CURL_CFG="$(mktemp "${TMPDIR:-/tmp}/deploy-curl.XXXXXX")"
DL_TMP="$(mktemp "${TMPDIR:-/tmp}/deploy-dl.XXXXXX")"

cleanup() {
    local code=$?
    rm -f "$CURL_CFG" "$DL_TMP"
    # 141 = SIGPIPE, which just means the output was piped into head/grep.
    [ $code -ne 0 ] && [ $code -ne 141 ] && error "Deployment aborted (exit code ${code})."
    return $code
}
trap cleanup EXIT

_esc() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g'; }
{
    printf 'user = "%s:%s"\n' "$(_esc "$SERVER_USER")" "$(_esc "$FTP_PASSWORD")"
    printf 'connect-timeout = %s\n' "$CONNECT_TIMEOUT"
    printf 'max-time = %s\n' "$MAX_TIME"
    printf 'silent\nshow-error\nfail\n'
    [ "$FTP_TLS" = "yes" ] && printf 'ssl-reqd\n'
    [ "$FTP_INSECURE" = "yes" ] && printf 'insecure\n'
} > "$CURL_CFG"

ftp_curl() { curl --config "$CURL_CFG" "$@"; }
remote_url() { printf 'ftp://%s:%s%s/%s' "$SERVER_IP" "$FTP_PORT" "$APP_DIR" "$1"; }

# ---------------------------------------------------------
# 4. File selection
# ---------------------------------------------------------
is_excluded() {
    local rel="$1" pat
    for pat in "${EXCLUDES[@]}"; do
        # shellcheck disable=SC2053
        [[ "$rel" == "$pat" || "$rel" == "$pat"/* || "${rel##*/}" == $pat || "$rel" == $pat ]] && return 0
    done
    return 1
}

# Paths are stored and sent relative to SOURCE_DIR, so a build directory maps
# onto the server root rather than recreating "dist/" remotely.
local_path() { printf '%s/%s' "$SOURCE_DIR" "$1"; }

# Prints every deployable file, one path relative to SOURCE_DIR per line.
collect_files() {
    local f rel
    while IFS= read -r f; do
        rel="${f#"$SOURCE_DIR"/}"
        is_excluded "$rel" || printf '%s\n' "$rel"
    done < <(find "$SOURCE_DIR" -type f -print | LC_ALL=C sort)
}

hash_file() { sha1sum "$1" | cut -d' ' -f1; }

# Loads the previous manifest into MANIFEST_MAP, but only if it belongs to this
# target — a changed host or path means the local record says nothing useful.
declare -A MANIFEST_MAP=()
MANIFEST_LOADED=0
load_manifest() {
    MANIFEST_MAP=()
    MANIFEST_LOADED=0
    [ -f "$MANIFEST" ] || return 0
    local header sha rel
    IFS= read -r header < "$MANIFEST" || return 0
    if [ "$header" != "# target: ${TARGET}" ]; then
        warn "Manifest belongs to a different target — treating every file as changed."
        return 0
    fi
    while IFS=$'\t' read -r sha rel; do
        [ -n "${rel:-}" ] && MANIFEST_MAP["$rel"]="$sha"
    done < <(tail -n +2 "$MANIFEST")
    MANIFEST_LOADED=1
}

save_manifest() {
    local rel
    {
        printf '# target: %s\n' "$TARGET"
        for rel in "${!MANIFEST_MAP[@]}"; do
            printf '%s\t%s\n' "${MANIFEST_MAP[$rel]}" "$rel"
        done | LC_ALL=C sort -k2
    } > "$MANIFEST"
}

# Fills CHANGED with files whose hash differs from the manifest.
declare -a CHANGED=()
compute_changed() {
    local rel sha
    CHANGED=()
    load_manifest
    while IFS= read -r rel; do
        sha="$(hash_file "$(local_path "$rel")")"
        [ "${MANIFEST_MAP[$rel]:-}" = "$sha" ] || CHANGED+=("$rel")
    done < <(collect_files)
}

# ---------------------------------------------------------
# 5. Steps
# ---------------------------------------------------------
find_php() {
    # XAMPP's php is usually not on PATH, and the path differs between Git Bash
    # (/c/...) and WSL (/mnt/c/...). Set PHP_BIN in .env_deploy to override.
    local candidate
    if [ -n "${PHP_BIN:-}" ] && [ -x "${PHP_BIN}" ]; then
        printf '%s' "$PHP_BIN"; return 0
    fi
    if command -v php >/dev/null 2>&1; then printf 'php'; return 0; fi
    for candidate in /c/xampp/php/php.exe /c/xampp/php/php \
                     /mnt/c/xampp/php/php.exe /usr/bin/php; do
        [ -x "$candidate" ] && { printf '%s' "$candidate"; return 0; }
    done
    return 1
}

# Rejects PHP 7+ syntax. Only runs when PHP56_CHECK=yes, since it is a
# host-specific policy, not a general correctness rule. Comments are stripped
# first — a codebase that documents this rule would otherwise match it.
check_php56() {
    local f hits=""
    for f in "$@"; do
        hits+="$(sed -e 's#://#:@@#g' -e 's#//.*##' -e 's/^[[:space:]]*[*#].*//' "$f" \
                 | grep -nE '\?\?|random_bytes\(' | sed "s|^|${f}:|" || true)"
    done
    if [ -n "$hits" ]; then
        error "PHP 7+ syntax found, but PHP56_CHECK=yes:"
        echo "$hits" >&2
        die "Fix the above before deploying."
    fi
    info "PHP 5.6 compatibility OK."
}

run_tests() {
    step "Running pre-deployment checks..."

    bash -n "$0" || die "deploy.sh has a syntax error."

    if [ -n "$PRE_DEPLOY_CMD" ]; then
        step "PRE_DEPLOY_CMD: ${PRE_DEPLOY_CMD}"
        # Subshell: a build command must not be able to cd, exit, or otherwise
        # alter the deploy script's own state.
        ( eval "$PRE_DEPLOY_CMD" ) || die "PRE_DEPLOY_CMD failed — nothing was uploaded."
        info "Build/test command succeeded."
    fi

    local rel php_bin=""
    local -a php_files=()
    while IFS= read -r rel; do
        php_files+=("$(local_path "$rel")")
    done < <(collect_files | grep '\.php$' || true)

    if [ ${#php_files[@]} -eq 0 ] || [ "$PHP_LINT" = "no" ]; then
        info "No PHP lint required."
    elif php_bin="$(find_php)"; then
        for rel in "${php_files[@]}"; do
            "$php_bin" -l "$rel" >/dev/null 2>&1 || die "PHP syntax error in ${rel}"
        done
        info "PHP syntax OK (${php_bin}, ${#php_files[@]} files)."
    elif [ "$PHP_LINT" = "yes" ]; then
        die "PHP_LINT=yes but no php binary found. Set PHP_BIN in ${ENV_FILE}."
    else
        warn "php not found — PHP SYNTAX WAS NOT CHECKED."
        warn "Set PHP_BIN in ${ENV_FILE} (e.g. /mnt/c/xampp/php/php.exe under WSL)."
    fi

    if [ "$PHP56_CHECK" = "yes" ] && [ ${#php_files[@]} -gt 0 ]; then
        check_php56 "${php_files[@]}"
    fi

    echo -e "${GREEN}[OK]${NC} All checks passed."
}

# Offline: shows exactly what the exclude rules select. Useful when adopting
# this script into a new project, before any credentials are involved.
list_files() {
    local count
    count="$(collect_files | wc -l | tr -d ' ')"
    echo -e "${BLUE}${count} file(s) would be deployed from '${SOURCE_DIR}':${NC}"
    collect_files | sed 's/^/  /'
}

check_connection() {
    step "Verifying connection to ${SERVER_IP}..."
    ftp_curl --list-only "$(remote_url '')" >/dev/null \
        || die "Cannot reach ${APP_DIR} on ${SERVER_IP} — check credentials, APP_DIR and FTP_TLS."
    info "Connected; ${APP_DIR} is reachable."
}

deploy_dry_run() {
    check_connection
    step "Dry run: comparing local files against the last deploy..."
    compute_changed

    local total
    total="$(collect_files | wc -l | tr -d ' ')"
    if [ ${#CHANGED[@]} -eq 0 ]; then
        info "Server is up to date — ${total} files tracked, nothing to upload."
        return 0
    fi

    echo -e "${BLUE}The following ${#CHANGED[@]} of ${total} files would be uploaded:${NC}"
    printf '  %s\n' "${CHANGED[@]}"
    if [ "$MANIFEST_LOADED" -eq 0 ]; then
        warn "No deploy baseline exists yet, so every file counts as changed."
        warn "If you uploaded some of these by hand, run './deploy.sh --adopt' first"
        warn "to compare against the server and skip files that already match."
    fi
    [ -n "$STAMP_FILE" ] && info "Would stamp ${STAMP_FILE} with $(date +%Y-%m-%d)."
    info "Dry run complete. No files were changed."
}

# Rebuilds the manifest from what is actually on the server. Needed after a
# manual FTP upload: the manifest only records files this script sent, so
# without this everything looks changed.
adopt_remote() {
    check_connection
    step "Comparing every local file against the server..."

    MANIFEST_MAP=()
    local rel local_sha same=0 differs=0 absent=0
    local -a to_upload=()

    while IFS= read -r rel; do
        local_sha="$(hash_file "$(local_path "$rel")")"
        if ! ftp_curl --output "$DL_TMP" "$(remote_url "$rel")" 2>/dev/null; then
            absent=$((absent + 1)); to_upload+=("$rel (not on server)")
        elif [ "$(hash_file "$DL_TMP")" = "$local_sha" ]; then
            MANIFEST_MAP["$rel"]="$local_sha"
            same=$((same + 1))
        else
            differs=$((differs + 1)); to_upload+=("$rel (differs)")
        fi
    done < <(collect_files)

    save_manifest
    info "Identical to server: ${same}   differs: ${differs}   missing on server: ${absent}"

    if [ ${#to_upload[@]} -gt 0 ]; then
        echo -e "${BLUE}The next --deploy will upload:${NC}"
        printf '  %s\n' "${to_upload[@]}"
    else
        info "Server matches your working copy exactly."
    fi
    echo -e "${GREEN}[OK] Baseline recorded in ${MANIFEST}${NC}"
}

# Writes today's date to STAMP_FILE on the server. The old copy goes into the
# deploy's backup first, so --rollback puts the previous date back too. A
# failure here only warns: the files are already live, only the date is stale.
write_stamp() {
    local backup_dir="$1" today
    [ -n "$STAMP_FILE" ] || return 0
    today="$(date +%Y-%m-%d)"

    mkdir -p "${backup_dir}/$(dirname "$STAMP_FILE")"
    if ! ftp_curl --output "${backup_dir}/${STAMP_FILE}" "$(remote_url "$STAMP_FILE")" 2>/dev/null; then
        rm -f "${backup_dir}/${STAMP_FILE}"
        printf '%s\n' "$STAMP_FILE" >> "${backup_dir}/.created"
    fi

    printf '%s\n' "$today" > "$DL_TMP"
    if ftp_curl --ftp-create-dirs --upload-file "$DL_TMP" "$(remote_url "$STAMP_FILE")"; then
        info "Stamped ${STAMP_FILE} with ${today}."
    else
        warn "Could not write ${STAMP_FILE} — files are live, but the date shown is stale."
    fi
}

deploy_live() {
    check_connection
    step "Starting deployment to ${TARGET}..."
    compute_changed

    if [ ${#CHANGED[@]} -eq 0 ]; then
        info "Server is already up to date — nothing to do."
        return 0
    fi

    local backup_dir="${BACKUP_ROOT}/${TIMESTAMP}"
    mkdir -p "$backup_dir"
    local created_list="${backup_dir}/.created"
    : > "$created_list"

    echo "-> Backing up ${#CHANGED[@]} remote file(s) to ${backup_dir}"
    local rel
    for rel in "${CHANGED[@]}"; do
        mkdir -p "${backup_dir}/$(dirname "$rel")"
        if ! ftp_curl --output "${backup_dir}/${rel}" "$(remote_url "$rel")" 2>/dev/null; then
            # Not on the server yet: record it as new so rollback can report it.
            rm -f "${backup_dir}/${rel}"
            printf '%s\n' "$rel" >> "$created_list"
        fi
    done

    echo "-> Uploading"
    local uploaded=0
    for rel in "${CHANGED[@]}"; do
        printf '   %s ... ' "$rel"
        if ftp_curl --ftp-create-dirs --upload-file "$(local_path "$rel")" "$(remote_url "$rel")"; then
            echo -e "${GREEN}ok${NC}"
            MANIFEST_MAP["$rel"]="$(hash_file "$(local_path "$rel")")"
            uploaded=$((uploaded + 1))
            # Persist after each file so an interrupted deploy resumes correctly.
            save_manifest
        else
            echo -e "${RED}failed${NC}"
            error "Upload failed on ${rel}. ${uploaded} file(s) were uploaded."
            error "Run './deploy.sh --rollback' to restore, or --deploy to retry the rest."
            exit 1
        fi
    done

    info "Uploaded ${uploaded} file(s). Backup kept at ${backup_dir}"
    write_stamp "$backup_dir"

    if [ -n "$HEALTH_URL" ]; then
        step "Health check: ${HEALTH_URL}"
        local code
        code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 30 "$HEALTH_URL" || echo 000)"
        if [ "$code" = "200" ]; then
            info "Health check OK (HTTP 200)."
        else
            warn "Health check returned HTTP ${code} — verify the site manually."
        fi
    fi

    echo -e "${GREEN}[OK] Deployment successful! 🚀${NC}"
}

list_backups() {
    if [ ! -d "$BACKUP_ROOT" ] || [ -z "$(ls -A "$BACKUP_ROOT" 2>/dev/null)" ]; then
        info "No backups available."
        return 0
    fi
    echo -e "${BLUE}Available backups (newest last):${NC}"
    local d
    for d in "$BACKUP_ROOT"/*/; do
        [ -d "$d" ] || continue
        printf '  %s  (%s files)\n' "$(basename "$d")" \
            "$(find "$d" -type f -not -name '.created' | wc -l | tr -d ' ')"
    done
}

rollback() {
    [ -d "$BACKUP_ROOT" ] || die "No backups found — nothing to roll back to."

    local backup_dir
    backup_dir="$(find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d | LC_ALL=C sort | tail -n 1)"
    [ -n "$backup_dir" ] || die "No backups found — nothing to roll back to."

    check_connection
    step "Rolling back from ${backup_dir}..."

    local rel restored=0
    while IFS= read -r f; do
        rel="${f#"$backup_dir"/}"
        printf '   %s ... ' "$rel"
        if ftp_curl --ftp-create-dirs --upload-file "$f" "$(remote_url "$rel")"; then
            echo -e "${GREEN}ok${NC}"
            restored=$((restored + 1))
        else
            echo -e "${RED}failed${NC}"
            die "Rollback failed on ${rel} — the server is in a mixed state."
        fi
    done < <(find "$backup_dir" -type f -not -name '.created' | LC_ALL=C sort)

    # The manifest no longer describes the server; drop it so the next deploy
    # re-syncs everything rather than skipping files it thinks are current.
    rm -f "$MANIFEST"
    info "Restored ${restored} file(s)."

    if [ -s "${backup_dir}/.created" ]; then
        warn "These files were created by that deploy and still exist on the server:"
        sed 's/^/     /' "${backup_dir}/.created"
        warn "Delete them manually if they should not be live."
    fi

    echo -e "${GREEN}[OK] Rollback complete.${NC}"
}

usage() {
    cat <<EOF
Usage: $0 <command>

  --test           Run pre-deployment checks only (syntax + PHP 5.6 compatibility)
  --dry-run        Run checks, connect, and list the files that would be uploaded
  --deploy         Run checks and upload changed files (backs up what it overwrites)
  --adopt          Hash every remote file and record what already matches, so a
                   manual upload is not re-sent (read-only; run before --deploy)
  --files          List the files the exclude rules select (offline, no login)
  --rollback       Restore the most recent backup
  --list-backups   Show available backups
  --encrypt-password
                   Replace the plaintext password in ${ENV_FILE} with a Windows
                   DPAPI-encrypted FTP_PASSWORD_DPAPI (WSL / Git Bash on Windows)

Configuration is read from ${ENV_FILE}.
EOF
}

# ---------------------------------------------------------
# 6. CLI
# ---------------------------------------------------------
case "${1:-}" in
    --test)         run_tests ;;
    --dry-run)      run_tests; deploy_dry_run ;;
    --deploy)       run_tests; deploy_live ;;
    --adopt)        adopt_remote ;;
    --files)        list_files ;;
    --rollback)     rollback ;;
    --list-backups) list_backups ;;
    -h|--help)      usage ;;
    "")             usage; exit 1 ;;
    *)              error "Invalid option: $1"; usage; exit 1 ;;
esac
