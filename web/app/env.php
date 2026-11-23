<?php
// ============================================================
// env.php — reads a simple KEY=VALUE .env file (same format as the
// Android project's .env). Taken from the Run project's js/config.php.
// PHP 5.6 safe — no ??, no type hints.
// ============================================================

function loadEnv($path) {
    $vars = array();
    if (!is_file($path)) {
        return $vars;
    }
    $lines = file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    if ($lines === false) {
        return $vars;
    }
    foreach ($lines as $line) {
        $line = trim($line);
        if ($line === '' || $line[0] === '#') {
            continue;
        }
        $pos = strpos($line, '=');
        if ($pos === false) {
            continue;
        }
        $key = trim(substr($line, 0, $pos));
        $val = trim(substr($line, $pos + 1));
        // Strip surrounding quotes, if any
        $len = strlen($val);
        if ($len >= 2) {
            $first = $val[0];
            $last = $val[$len - 1];
            if (($first === '"' && $last === '"') || ($first === "'" && $last === "'")) {
                $val = substr($val, 1, $len - 2);
            }
        }
        $vars[$key] = $val;
    }
    return $vars;
}
