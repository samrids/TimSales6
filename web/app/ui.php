<?php
// ============================================================
// ui.php — small HTML partials shared by several pages.
// ============================================================

/**
 * One sale in a list: date block · weight and price · amount. Tapping opens the edit form.
 * $showMonth = false inside a month group (the group header already names the month):
 * the date block then shows the weekday instead.
 */
function sale_row($r, $showMonth = true) {
    $ymd = $r['saleDate'];
    $day = (int)substr($ymd, 8, 2);
    $under = $showMonth ? thai_month_short(substr($ymd, 5, 2)) : thai_weekday_short($ymd);
    return '<li id="sale-' . (int)$r['id'] . '"><a class="sale-row" href="' . h(url('sale_form.php', array('id' => $r['id']))) . '">'
        . '<span class="sr-only">แก้ไขรายการ ' . h(fmt_date_th($ymd)) . ' </span>'
        . '<span class="date-block" aria-hidden="true"><span class="date-day">' . $day . '</span><span class="date-month">' . h($under) . '</span></span>'
        . '<span class="sale-main">'
        .   '<span class="sale-title">' . h(fmt_trim($r['kg'])) . ' กก.</span>'
        .   '<span class="sale-sub">' . h(fmt_number($r['unitPrice'])) . ' บาท/กก.</span>'
        . '</span>'
        . '<span class="sale-amount">' . h(fmt_number($r['amount'])) . '<span class="sr-only"> บาท</span></span>'
        . '</a></li>';
}

/**
 * Attributes tying an input to its error and hint (WCAG 3.3.1 / 1.3.1).
 * The first field with an error also gets autofocus, so the cursor lands on it.
 */
function field_attrs($errors, $name, $hintId = null) {
    $ids = array();
    if (isset($errors[$name])) {
        $ids[] = $name . '-error';
    }
    if ($hintId !== null) {
        $ids[] = $hintId;
    }
    $out = $ids ? ' aria-describedby="' . h(implode(' ', $ids)) . '"' : '';
    if (isset($errors[$name])) {
        $out .= ' aria-invalid="true"';
        reset($errors);
        if (key($errors) === $name) {
            $out .= ' autofocus';
        }
    }
    return $out;
}

/** The error line under an input (empty string when the field is fine) */
function field_error($errors, $name) {
    if (!isset($errors[$name])) {
        return '';
    }
    return '<p class="field-error" id="' . h($name) . '-error">' . icon('alert', 16) . '<span>' . h($errors[$name]) . '</span></p>';
}

/**
 * Season switcher: one link per season (one tap, no JS, arrow keys don't jump
 * between seasons like an auto-submitting <select> did).
 * $orphans: number of sales outside every season (adds a "no season" link when > 0)
 */
function season_tabs($seasons, $current, $page, $orphans = 0, $orphansActive = false) {
    $items = array();
    foreach ($seasons as $s) {
        $on = !$orphansActive && $current !== null && $s->code() === $current->code();
        $items[] = array(url($page, array('season' => $s->code())), $s->code(), $on);
    }
    if ($orphans > 0 || $orphansActive) {
        $items[] = array(url($page, array('season' => 'none')), 'ไม่อยู่ในรอบใด (' . (int)$orphans . ')', $orphansActive);
    }
    if (count($items) < 2) {
        return '';
    }
    $html = '<nav class="season-tabs" aria-label="เลือกรอบการขาย"><ul>';
    foreach ($items as $it) {
        $html .= '<li><a href="' . h($it[0]) . '"' . ($it[2] ? ' aria-current="page"' : '') . '>' . h($it[1]) . '</a></li>';
    }
    return $html . '</ul></nav>';
}

/** Status chip for a season relative to today */
function season_chip($season) {
    $labels = array('active' => 'กำลังดำเนินอยู่', 'ended' => 'สิ้นสุดแล้ว', 'upcoming' => 'ยังไม่เริ่ม');
    $st = $season->status();
    return '<span class="chip chip-' . $st . '">' . h($labels[$st]) . '</span>';
}

/** "1 ก.พ. 2026 – 31 มี.ค. 2027" */
function season_range($season) {
    return fmt_date_th($season->startDate) . ' – ' . fmt_date_th($season->endDate);
}

/** ฿452,300<small>.00</small> — whole baht large, satang small */
function money_hero($n) {
    $s = fmt_number($n);
    $dot = strrpos($s, '.');
    return '฿' . h(substr($s, 0, $dot)) . '<small>' . h(substr($s, $dot)) . '</small>';
}
