<?php
// ============================================================
// validation.php — input rules shared by the web forms, the CSV importer
// and the JSON API. Same rules and messages as AddSalesActivity /
// AddEditTimYearActivity + AppConfig limits. PHP 5.6 safe.
// ============================================================

/**
 * Validate a sale. Inputs are raw strings (or numbers from JSON).
 * Returns array('error' => string|null, 'saleDate' => 'Y-m-d', 'kg' => float, 'unitPrice' => float)
 */
function validate_sale($saleDate, $kgText, $priceText) {
    $out = array('error' => null, 'saleDate' => null, 'kg' => null, 'unitPrice' => null);
    $saleDate = trim((string)$saleDate);
    $kgText = trim((string)$kgText);
    $priceText = trim((string)$priceText);
    $kg = parse_decimal($kgText);
    $price = parse_decimal($priceText);

    if ($saleDate === '') {
        $out['error'] = 'กรุณาเลือกวันที่';
    } elseif (!preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $saleDate, $m) || !checkdate((int)$m[2], (int)$m[3], (int)$m[1])) {
        $out['error'] = 'รูปแบบวันที่ไม่ถูกต้อง (ต้องเป็น ปปปป-ดด-วว)';
    } elseif ($kgText === '') {
        $out['error'] = 'กรุณาใส่น้ำหนัก';
    } elseif ($kg === null || $kg <= 0) {
        $out['error'] = 'น้ำหนักต้องเป็นตัวเลขที่มากกว่า 0';
    } elseif ($kg < MIN_WEIGHT || $kg > MAX_WEIGHT) {
        $out['error'] = 'น้ำหนักต้องอยู่ระหว่าง ' . fmt_trim(MIN_WEIGHT) . ' ถึง ' . fmt_trim(MAX_WEIGHT) . ' กก.';
    } elseif ($priceText === '') {
        $out['error'] = 'กรุณาใส่ราคาต่อหน่วย';
    } elseif ($price === null || $price <= 0) {
        $out['error'] = 'ราคาต่อหน่วยต้องเป็นตัวเลขที่มากกว่า 0';
    } elseif ($price < MIN_PRICE || $price > MAX_PRICE) {
        $out['error'] = 'ราคาต่อหน่วยต้องอยู่ระหว่าง ' . fmt_trim(MIN_PRICE) . ' ถึง ' . fmt_trim(MAX_PRICE) . ' บาท/กก.';
    }

    if ($out['error'] === null) {
        $out['saleDate'] = $saleDate;
        $out['kg'] = $kg;
        $out['unitPrice'] = $price;
    }
    return $out;
}

/**
 * Validate a season. Empty start/end fall back to the defaults for the year.
 * Returns array('error' => string|null, 'saleYear' => int, 'startDate' => 'Y-m-d', 'endDate' => 'Y-m-d')
 */
function validate_season($yearText, $startDate, $endDate) {
    $out = array('error' => null, 'saleYear' => null, 'startDate' => null, 'endDate' => null);
    $yearText = trim((string)$yearText);

    if ($yearText === '') {
        $out['error'] = 'กรุณาใส่ปี';
        return $out;
    }
    if (!preg_match('/^\d+$/', $yearText)) {
        $out['error'] = 'กรุณาใส่ปีเป็นตัวเลข';
        return $out;
    }
    $year = (int)$yearText;
    if ($year < 1900 || $year > 2100) {
        $out['error'] = 'ปีต้องอยู่ระหว่าง 1900 - 2100';
        return $out;
    }

    $start = trim((string)$startDate) === '' ? Season::defaultStart($year) : to_ymd($startDate);
    $end = trim((string)$endDate) === '' ? Season::defaultEnd($year) : to_ymd($endDate);
    if ($start === null) {
        $out['error'] = 'วันเริ่มรอบไม่ถูกต้อง';
    } elseif ($end === null) {
        $out['error'] = 'วันสิ้นสุดรอบไม่ถูกต้อง';
    } elseif ($start > $end) {
        $out['error'] = 'วันเริ่มรอบต้องไม่อยู่หลังวันสิ้นสุดรอบ';
    } elseif (count(SalesRepository::enumerateMonths($start, $end)) > 36) {
        $out['error'] = 'รอบการขายยาวได้ไม่เกิน 36 เดือน';
    }

    if ($out['error'] === null) {
        $out['saleYear'] = $year;
        $out['startDate'] = $start;
        $out['endDate'] = $end;
    }
    return $out;
}
