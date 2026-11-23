<?php
// ============================================================
// flash.php — one-time messages shown after a redirect (replaces Toast).
// ============================================================

/** $type: 'success' | 'error' | 'info' */
function flash($type, $message) {
    if (!isset($_SESSION['flash'])) {
        $_SESSION['flash'] = array();
    }
    $_SESSION['flash'][] = array('type' => $type, 'message' => $message);
}

function flash_take() {
    $list = isset($_SESSION['flash']) ? $_SESSION['flash'] : array();
    unset($_SESSION['flash']);
    return $list;
}
