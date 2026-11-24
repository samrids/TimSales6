<?php
// ============================================================
// format.php — date/number helpers (port of the formatting functions in
// SalesRecord.kt, TimYear.kt and SalesStatistics.kt). PHP 5.6 safe.
// ============================================================

/**
 * Normalise a date value to 'Y-m-d', or null.
 * Accepts '2026-02-15', '2026-02-15T..', '2026-02-15 ..', '15/02/2026'
 * and unix timestamps (Grist exports).
 */
function to_ymd($raw) {
    if ($raw === null || $raw === '' || $raw === false) {
        return null;
    }
    if (is_int($raw) || is_float($raw) || (is_string($raw) && preg_match('/^-?\d+(\.\d+)?$/', $raw))) {
        return gmdate('Y-m-d', (int)$raw);
    }
    $s = trim((string)$raw);
    if (preg_match('#^(\d{1,2})/(\d{1,2})/(\d{4})$#', $s, $m)) {
        $s = sprintf('%04d-%02d-%02d', $m[3], $m[2], $m[1]);
    }
    $parts = preg_split('/[T ]/', $s);
    if (preg_match('/^(\d{4})-(\d{2})-(\d{2})/', $parts[0], $m) && checkdate((int)$m[2], (int)$m[3], (int)$m[1])) {
        return substr($parts[0], 0, 10);
    }
    return null;
}

/** Parse a timestamp from any common format into 'Y-m-d H:i:s' (local time), or null */
function to_datetime($raw) {
    if ($raw === null || trim((string)$raw) === '') {
        return null;
    }
    $s = trim((string)$raw);
    if (preg_match('/^\d+(\.\d+)?$/', $s)) {
        return date('Y-m-d H:i:s', (int)$s);
    }
    $t = strtotime($s);
    return $t === false ? null : date('Y-m-d H:i:s', $t);
}

/** 'Y-m-d' → 'dd/mm/yyyy' (returns input unchanged if not a date) */
function fmt_date($ymd) {
    if (!is_string($ymd) || !preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $ymd, $m)) {
        return (string)$ymd;
    }
    return $m[3] . '/' . $m[2] . '/' . $m[1];
}

function thai_month_short($m) {
    $names = array('ม.ค.', 'ก.พ.', 'มี.ค.', 'เม.ย.', 'พ.ค.', 'มิ.ย.', 'ก.ค.', 'ส.ค.', 'ก.ย.', 'ต.ค.', 'พ.ย.', 'ธ.ค.');
    return isset($names[(int)$m - 1]) ? $names[(int)$m - 1] : '';
}

function thai_month_long($m) {
    $names = array('มกราคม', 'กุมภาพันธ์', 'มีนาคม', 'เมษายน', 'พฤษภาคม', 'มิถุนายน',
        'กรกฎาคม', 'สิงหาคม', 'กันยายน', 'ตุลาคม', 'พฤศจิกายน', 'ธันวาคม');
    return isset($names[(int)$m - 1]) ? $names[(int)$m - 1] : '';
}

/** 'Y-m-d' → '18 พ.ย. 2026' */
function fmt_date_th($ymd) {
    if (!is_string($ymd) || !preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $ymd, $m)) {
        return (string)$ymd;
    }
    return (int)$m[3] . ' ' . thai_month_short($m[2]) . ' ' . $m[1];
}

/** Short Thai weekday for a 'Y-m-d' date: 'จ.', 'อ.', … */
function thai_weekday_short($ymd) {
    $names = array('อา.', 'จ.', 'อ.', 'พ.', 'พฤ.', 'ศ.', 'ส.');
    $t = strtotime($ymd . ' 12:00:00');
    return $t === false ? '' : $names[(int)date('w', $t)];
}

/** Format a stored timestamp ('Y-m-d H:i:s') as '18 พ.ย. 2026 10:28' */
function fmt_timestamp($raw) {
    if ($raw === null || $raw === '' || $raw === false) {
        return 'ไม่ระบุ';
    }
    $s = to_datetime($raw);
    if ($s === null) {
        return (string)$raw;
    }
    return fmt_date_th(substr($s, 0, 10)) . ' ' . substr($s, 11, 5);
}

function fmt_number($n) {
    return number_format((float)$n, 2, '.', ',');
}

/** Number without trailing zeros: 377.50 → '377.5', 529.00 → '529' (with thousands separators) */
function fmt_trim($n) {
    $s = number_format((float)$n, 2, '.', ',');
    return rtrim(rtrim($s, '0'), '.');
}

function fmt_money($n) {          // 17,754.00 บาท
    return fmt_number($n) . ' บาท';
}

function fmt_kg($n) {             // 529.00 กก.
    return fmt_number($n) . ' กก.';
}

function fmt_price($n) {          // 32.00 บาท/กก.
    return fmt_number($n) . ' บาท/กก.';
}

/** Value for an <input>: 529 → '529', 377.5 → '377.5' (no thousands separator) */
function input_number($n) {
    return rtrim(rtrim(number_format((float)$n, 2, '.', ''), '0'), '.');
}

/** 'YYYY-MM' → 'ก.พ. 26' (chart axis) */
function month_label($ym) {
    if (!preg_match('/^(\d{4})-(\d{2})$/', $ym, $m)) {
        return $ym;
    }
    return thai_month_short($m[2]) . ' ' . substr($m[1], 2, 2);
}

/** 'YYYY-MM' → 'กุมภาพันธ์ 2026' (tooltips, list headers) */
function month_title($ym) {
    if (!preg_match('/^(\d{4})-(\d{2})$/', $ym, $m)) {
        return $ym;
    }
    return thai_month_long($m[2]) . ' ' . $m[1];
}

/** Parse a user-entered decimal ("1,234.5") → float, or null if not a number */
function parse_decimal($s) {
    $s = str_replace(array(',', ' '), '', trim((string)$s));
    if ($s === '' || !is_numeric($s)) {
        return null;
    }
    return (float)$s;
}
