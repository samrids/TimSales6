<?php
// ============================================================
// bootstrap.php — included at the top of every page.
// Loads .env, opens the SQLite database lazily, starts the session,
// enforces login, wires helpers.
// PHP 5.6 safe — no ??, no scalar type hints, no return types.
//
// Constants a page may define before including this file:
//   PUBLIC_PAGE  — no login required (login.php, logout.php)
//   API_REQUEST  — JSON API: no session, no login (api.php checks its own token)
// ============================================================

error_reporting(E_ALL);
ini_set('display_errors', '0');
header('X-Frame-Options: DENY');
header('X-Content-Type-Options: nosniff');
header('Referrer-Policy: same-origin');
date_default_timezone_set('Asia/Bangkok');

define('APP_ROOT', dirname(__DIR__));
define('APP_DIR', __DIR__);

require APP_DIR . '/env.php';
require APP_DIR . '/format.php';
require APP_DIR . '/csrf.php';
require APP_DIR . '/flash.php';
require APP_DIR . '/icons.php';
require APP_DIR . '/ui.php';
require APP_DIR . '/Database.php';
require APP_DIR . '/Season.php';
require APP_DIR . '/SeasonRepository.php';
require APP_DIR . '/SalesRepository.php';
require APP_DIR . '/validation.php';
require APP_DIR . '/CsvImporter.php';

$GLOBALS['APP_ENV'] = loadEnv(APP_ROOT . '/.env');

/** Read a value from .env (server-side only — never echo secrets). */
function env($key, $default = '') {
    return isset($GLOBALS['APP_ENV'][$key]) && $GLOBALS['APP_ENV'][$key] !== ''
        ? $GLOBALS['APP_ENV'][$key]
        : $default;
}

// Validation limits — same as AppConfig.kt
define('MIN_WEIGHT', 0.01);
define('MAX_WEIGHT', 10000.0);
define('MIN_PRICE', 0.01);
define('MAX_PRICE', 100000.0);

function h($s) {
    return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8');
}

/** Asset URL with automatic cache busting (?v=<file mtime>) — no manual ?v=N bumps */
function asset($path) {
    $file = APP_ROOT . '/' . $path;
    return is_file($file) ? $path . '?v=' . filemtime($file) : $path;
}

function redirect($url) {
    header('Location: ' . $url);
    exit;
}

/** Build a relative URL with a query string, e.g. url('sales.php', ['season' => '2026/27']) */
function url($path, $params = array()) {
    $params = array_filter($params, function ($v) { return $v !== null && $v !== ''; });
    return $params ? $path . '?' . http_build_query($params) : $path;
}

function is_logged_in() {
    return !empty($_SESSION['auth']);
}

function require_login() {
    if (!is_logged_in()) {
        $isPost = isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'POST';
        $_SESSION['return_to'] = $isPost ? interrupted_post_url() : current_page_url();
        if ($isPost || !empty($GLOBALS['sessionExpired'])) {
            flash('info', 'หมดเวลาใช้งาน กรุณาเข้าสู่ระบบอีกครั้ง');
        }
        redirect('login.php');
    }
}

// ---------- Coming back after an interruption (session timeout, stale CSRF token) ----------

/** Pages whose POST can be put back into the form so nothing typed is lost */
function resumable_pages() {
    return array('sale_form.php', 'season_form.php');
}

/** This request's page as a local GET URL, e.g. "sale_form.php?id=3" */
function current_page_url() {
    $page = basename(isset($_SERVER['SCRIPT_NAME']) ? (string)$_SERVER['SCRIPT_NAME'] : 'index.php');
    $qs = isset($_SERVER['QUERY_STRING']) ? (string)$_SERVER['QUERY_STRING'] : '';
    return $qs !== '' ? $page . '?' . $qs : $page;
}

/** Only one of our own pages ("name.php?query") — never another host, never login/logout */
function is_safe_return_url($u) {
    return is_string($u) && preg_match('/^[a-z_]+\.php(\?[^\s#]*)?$/', $u) === 1
        && strpos($u, 'login.php') !== 0 && strpos($u, 'logout.php') !== 0;
}

/** False when the browser reports (Origin/Referer) that the request came from another site */
function same_origin_request() {
    $from = isset($_SERVER['HTTP_ORIGIN']) ? (string)$_SERVER['HTTP_ORIGIN']
        : (isset($_SERVER['HTTP_REFERER']) ? (string)$_SERVER['HTTP_REFERER'] : '');
    if ($from === '') {
        return true;
    }
    $host = parse_url($from, PHP_URL_HOST);
    $port = parse_url($from, PHP_URL_PORT);
    $here = isset($_SERVER['HTTP_HOST']) ? strtolower((string)$_SERVER['HTTP_HOST']) : '';
    return $host !== null && $host !== false && strtolower($host . ($port ? ':' . $port : '')) === $here;
}

/**
 * A POST that couldn't be processed. For the add/edit forms, keep what was typed
 * (scalar fields only, never the CSRF token) so the form can show it again —
 * shown only, never saved without the user pressing save. Returns the page to go back to.
 */
function interrupted_post_url() {
    $page = basename(isset($_SERVER['SCRIPT_NAME']) ? (string)$_SERVER['SCRIPT_NAME'] : '');
    if (in_array($page, resumable_pages(), true) && same_origin_request()) {
        $post = array();
        foreach ($_POST as $k => $v) {
            if ($k !== 'csrf' && is_string($v)) {
                $post[(string)$k] = substr($v, 0, 200);
            }
        }
        $_SESSION['resume'] = array('url' => current_page_url(), 'post' => $post);
        return current_page_url();
    }
    $postOnly = array('sale_delete.php' => 'sales.php', 'season_delete.php' => 'seasons.php');
    return isset($postOnly[$page]) ? $postOnly[$page] : current_page_url();
}

/** Values kept by interrupted_post_url() for this page, or null. One use. */
function resume_take() {
    if (empty($_SESSION['resume']) || $_SESSION['resume']['url'] !== current_page_url()) {
        return null;
    }
    $post = $_SESSION['resume']['post'];
    unset($_SESSION['resume']);
    return $post;
}

function require_post() {
    if (!isset($_SERVER['REQUEST_METHOD']) || $_SERVER['REQUEST_METHOD'] !== 'POST') {
        http_response_code(405);
        exit('Method Not Allowed');
    }
}

function season_repo() {
    static $repo = null;
    if ($repo === null) {
        $repo = new SeasonRepository(db());
    }
    return $repo;
}

function sales_repo() {
    static $repo = null;
    if ($repo === null) {
        $repo = new SalesRepository(db());
    }
    return $repo;
}

/**
 * Pick the season to show: ?season=<code> if valid, else the one remembered
 * in the session, else default_season(). Remembers the choice for other pages.
 */
function pick_season($seasons) {
    $wanted = isset($_GET['season']) ? (string)$_GET['season']
        : (isset($_SESSION['season']) ? $_SESSION['season'] : '');
    $season = $wanted !== '' ? SeasonRepository::findByCode($seasons, $wanted) : null;
    if ($season === null) {
        $season = default_season($seasons);
    }
    if ($season !== null) {
        $_SESSION['season'] = $season->code();
    }
    return $season;
}

/**
 * Season to open when none is chosen: the one today's sales count toward
 * (earliest start wins on overlap, same rule as the SQL), else the latest one
 * that has already started, else the newest. Avoids landing on an empty
 * upcoming season that was created early.
 */
function default_season($seasons) {
    $today = date('Y-m-d');
    $current = null;
    $started = null;
    foreach ($seasons as $s) {
        if ($s->containsDate($today) && ($current === null || $s->startDate < $current->startDate)) {
            $current = $s;
        }
        if ($s->startDate <= $today && ($started === null || $s->startDate > $started->startDate)) {
            $started = $s;
        }
    }
    if ($current !== null) {
        return $current;
    }
    if ($started !== null) {
        return $started;
    }
    return empty($seasons) ? null : $seasons[0];
}

/** Sales list showing one record: its own season (or the no-season list), scrolled to its row */
function sale_list_url_for($record) {
    $season = $record['seasonCode'] !== null ? $record['seasonCode'] : 'none';
    return url('sales.php', array('season' => $season)) . '#sale-' . (int)$record['id'];
}

/** Sales list URL for the remembered season */
function sales_list_url() {
    return url('sales.php', array('season' => isset($_SESSION['season']) ? $_SESSION['season'] : null));
}

/** JSON safe to embed inside <script type="application/json"> */
function json_for_html($data) {
    return json_encode($data, JSON_UNESCAPED_UNICODE | JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT);
}

/** User-facing message for an exception (AppException text as-is, others prefixed) */
function error_message($prefix, $e) {
    return $e instanceof AppException ? $e->getMessage() : $prefix . ': ' . $e->getMessage();
}

if (defined('API_REQUEST')) {
    return; // api.php handles auth and output itself
}

header('Content-Type: text/html; charset=utf-8');

// ---------- Session ----------
$isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
    || (isset($_SERVER['SERVER_PORT']) && (int)$_SERVER['SERVER_PORT'] === 443);
session_name('TIMSALES');
session_set_cookie_params(0, '/', '', $isHttps, true);
session_start();

// Idle timeout: 8 hours without activity logs the user out
define('SESSION_IDLE_SECONDS', 8 * 3600);
if (!empty($_SESSION['auth']) && isset($_SESSION['last_seen'])
    && time() - $_SESSION['last_seen'] > SESSION_IDLE_SECONDS) {
    $_SESSION = array();
    session_regenerate_id(true);
    $sessionExpired = true; // require_login() tells the user why they are on the login page
}
$_SESSION['last_seen'] = time();

// Every page except login requires a session
if (!defined('PUBLIC_PAGE')) {
    require_login();
}
