<?php
// ============================================================
// ui.php — small HTML partials shared by several pages.
// ============================================================

/** One sale in a list: date block · weight and price · amount. Tapping opens the edit form. */
function sale_row($r) {
    $ymd = $r['saleDate'];
    $day = (int)substr($ymd, 8, 2);
    $month = thai_month_short(substr($ymd, 5, 2));
    return '<li><a class="sale-row" href="' . h(url('sale_form.php', array('id' => $r['id']))) . '">'
        . '<span class="sr-only">แก้ไขรายการ ' . h(fmt_date_th($ymd)) . ' </span>'
        . '<span class="date-block" aria-hidden="true"><span class="date-day">' . $day . '</span><span class="date-month">' . h($month) . '</span></span>'
        . '<span class="sale-main">'
        .   '<span class="sale-title">' . h(fmt_trim($r['kg'])) . ' กก.</span>'
        .   '<span class="sale-sub">' . h(thai_weekday_short($ymd)) . ' · ' . h(fmt_number($r['unitPrice'])) . ' บาท/กก.</span>'
        . '</span>'
        . '<span class="sale-amount">' . h(fmt_number($r['amount'])) . '<small>บาท</small></span>'
        . '</a></li>';
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
