<?php
// ============================================================
// validation.php — input rules shared by the web forms, the CSV importer
// and the JSON API. Same rules and messages as AddSalesActivity /
// AddEditTimYearActivity + AppConfig limits. PHP 5.6 safe.
// ============================================================

/**
 * Validate a sale. Inputs are raw strings (or numbers from JSON).
 * Returns array('error' => string|null, 'errors' => array(field => message),
 *               'saleDate' => 'Y-m-d', 'kg' => float, 'unitPrice' => float)
 * 'error' is the first message (field order: saleDate, kg, unitPrice) for callers
 * that show one line (API, CSV import); the web form uses 'errors' per field.
 */
function validate_sale($saleDate, $kgText, $priceText) {
    $out = array('error' => null, 'errors' => array(), 'saleDate' => null, 'kg' => null, 'unitPrice' => null);
    $errors = array();
    $saleDate = trim((string)$saleDate);
    $kgText = trim((string)$kgText);
    $priceText = trim((string)$priceText);
    $kg = parse_decimal($kgText);
    $price = parse_decimal($priceText);

    if ($saleDate === '') {
        $errors['saleDate'] = 'กรุณาเลือกวันที่';
    } elseif (!preg_match('/^(\d{4})-(\d{2})-(\d{2})$/', $saleDate, $m) || !checkdate((int)$m[2], (int)$m[3], (int)$m[1])) {
        $errors['saleDate'] = 'รูปแบบวันที่ไม่ถูกต้อง (ต้องเป็น ปปปป-ดด-วว)';
    }
    if ($kgText === '') {
        $errors['kg'] = 'กรุณาใส่น้ำหนัก';
    } elseif ($kg === null || $kg <= 0) {
        $errors['kg'] = 'น้ำหนักต้องเป็นตัวเลขที่มากกว่า 0';
    } elseif ($kg < MIN_WEIGHT || $kg > MAX_WEIGHT) {
        $errors['kg'] = 'น้ำหนักต้องอยู่ระหว่าง ' . fmt_trim(MIN_WEIGHT) . ' ถึง ' . fmt_trim(MAX_WEIGHT) . ' กก.';
    }
    if ($priceText === '') {
        $errors['unitPrice'] = 'กรุณาใส่ราคาต่อหน่วย';
    } elseif ($price === null || $price <= 0) {
        $errors['unitPrice'] = 'ราคาต่อหน่วยต้องเป็นตัวเลขที่มากกว่า 0';
    } elseif ($price < MIN_PRICE || $price > MAX_PRICE) {
        $errors['unitPrice'] = 'ราคาต่อหน่วยต้องอยู่ระหว่าง ' . fmt_trim(MIN_PRICE) . ' ถึง ' . fmt_trim(MAX_PRICE) . ' บาท/กก.';
    }

    if ($errors) {
        $out['errors'] = $errors;
        $out['error'] = reset($errors);
    } else {
        $out['saleDate'] = $saleDate;
        $out['kg'] = $kg;
        $out['unitPrice'] = $price;
    }
    return $out;
}

/**
 * Validate a season. Empty start/end fall back to the defaults for the year.
 * Returns array('error' => string|null, 'errors' => array(field => message),
 *               'saleYear' => int, 'startDate' => 'Y-m-d', 'endDate' => 'Y-m-d')
 * ('error' = first message, as in validate_sale)
 */
function validate_season($yearText, $startDate, $endDate) {
    $out = array('error' => null, 'errors' => array(), 'saleYear' => null, 'startDate' => null, 'endDate' => null);
    $yearText = trim((string)$yearText);

    // The default dates depend on the year, so a bad year stops here
    $yearError = null;
    if ($yearText === '') {
        $yearError = 'กรุณาใส่ปี';
    } elseif (!preg_match('/^\d+$/', $yearText)) {
        $yearError = 'กรุณาใส่ปีเป็นตัวเลข';
    } elseif ((int)$yearText < 1900 || (int)$yearText > 2100) {
        $yearError = 'ปีต้องอยู่ระหว่าง 1900 - 2100';
    }
    if ($yearError !== null) {
        $out['error'] = $yearError;
        $out['errors'] = array('saleYear' => $yearError);
        return $out;
    }
    $year = (int)$yearText;

    $errors = array();
    $start = trim((string)$startDate) === '' ? Season::defaultStart($year) : to_ymd($startDate);
    $end = trim((string)$endDate) === '' ? Season::defaultEnd($year) : to_ymd($endDate);
    if ($start === null) {
        $errors['startDate'] = 'วันเริ่มรอบไม่ถูกต้อง';
    }
    if ($end === null) {
        $errors['endDate'] = 'วันสิ้นสุดรอบไม่ถูกต้อง';
    } elseif ($start !== null && $start > $end) {
        $errors['endDate'] = 'วันเริ่มรอบต้องไม่อยู่หลังวันสิ้นสุดรอบ';
    } elseif ($start !== null && count(SalesRepository::enumerateMonths($start, $end)) > 36) {
        $errors['endDate'] = 'รอบการขายยาวได้ไม่เกิน 36 เดือน';
    }

    if ($errors) {
        $out['errors'] = $errors;
        $out['error'] = reset($errors);
    } else {
        $out['saleYear'] = $year;
        $out['startDate'] = $start;
        $out['endDate'] = $end;
    }
    return $out;
}
