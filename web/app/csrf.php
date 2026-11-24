<?php
// ============================================================
// csrf.php — per-session CSRF token for every POST form. PHP 5.6 safe.
// ============================================================

function random_hex($bytes) {
    if (function_exists('openssl_random_pseudo_bytes')) {
        $raw = openssl_random_pseudo_bytes($bytes, $strong);
        if ($raw !== false && $strong) {
            return bin2hex($raw);
        }
    }
    // Last resort (openssl extension missing) — not cryptographically strong
    return substr(sha1(uniqid(mt_rand(), true) . microtime()), 0, $bytes * 2);
}

function csrf_token() {
    if (empty($_SESSION['csrf'])) {
        $_SESSION['csrf'] = random_hex(32);
    }
    return $_SESSION['csrf'];
}

function csrf_field() {
    return '<input type="hidden" name="csrf" value="' . htmlspecialchars(csrf_token(), ENT_QUOTES, 'UTF-8') . '">';
}

/**
 * Stop the request unless the posted token matches the session token.
 * Nothing is saved; the user goes back to the page (with a fresh token and,
 * on the add/edit forms, what they typed) instead of a bare error page.
 */
function csrf_check() {
    $sent = isset($_POST['csrf']) ? (string)$_POST['csrf'] : '';
    if ($sent === '' || empty($_SESSION['csrf']) || !hash_equals($_SESSION['csrf'], $sent)) {
        flash('error', 'หน้านี้เปิดค้างไว้นานเกินไป จึงยังไม่ได้บันทึกการเปลี่ยนแปลง กรุณาลองอีกครั้ง');
        redirect(interrupted_post_url());
    }
}
