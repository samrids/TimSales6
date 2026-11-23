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

/** Abort the request unless the posted token matches the session token. */
function csrf_check() {
    $sent = isset($_POST['csrf']) ? (string)$_POST['csrf'] : '';
    if ($sent === '' || empty($_SESSION['csrf']) || !hash_equals($_SESSION['csrf'], $sent)) {
        http_response_code(400);
        exit('คำขอไม่ถูกต้อง (CSRF) — กรุณาโหลดหน้าใหม่แล้วลองอีกครั้ง');
    }
}
