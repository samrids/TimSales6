<?php
// Downloads: ?type=sales[&season=<code>|none] · ?type=seasons · ?type=db
// CSV columns match the Grist export, so files round-trip through data.php.
require __DIR__ . '/app/bootstrap.php';

$type = isset($_GET['type']) ? (string)$_GET['type'] : '';
$stamp = date('Ymd-His');

try {
    if ($type === 'db') {
        $path = db_path();
        db(); // make sure the file exists and the schema is current
        header('Content-Type: application/vnd.sqlite3');
        header('Content-Disposition: attachment; filename="timsales-' . $stamp . '.sqlite"');
        header('Content-Length: ' . filesize($path));
        header('Cache-Control: no-store');
        readfile($path);
        exit;
    }

    if ($type === 'seasons') {
        $rows = array(array('saleYear', 'startDate', 'endDate', 'created_at', 'updated_at', 'seasonCode'));
        foreach (season_repo()->all() as $s) {
            $rows[] = array($s->saleYear, $s->startDate, $s->endDate, $s->createdAt, $s->updatedAt, $s->code());
        }
        $name = 'timsales-seasons-' . $stamp . '.csv';
    } elseif ($type === 'sales') {
        $code = isset($_GET['season']) ? (string)$_GET['season'] : '';
        if ($code === 'none') {
            $records = sales_repo()->withoutSeason();
            $suffix = '-no-season';
        } elseif ($code !== '') {
            $season = SeasonRepository::findByCode(season_repo()->all(), $code);
            if ($season === null) {
                throw new AppException('ไม่พบรอบการขาย ' . $code);
            }
            $records = sales_repo()->forSeason($season);
            $suffix = '-' . str_replace('/', '-', $code);
        } else {
            $records = sales_repo()->all();
            $suffix = '';
        }
        $rows = array(array('saleDate', 'kg', 'unitPrice', 'amount', 'created_at', 'updated_at', 'seasonCode'));
        foreach ($records as $r) {
            $rows[] = array($r['saleDate'], input_number($r['kg']), input_number($r['unitPrice']),
                input_number($r['amount']), $r['createdAt'], $r['updatedAt'], $r['seasonCode']);
        }
        $name = 'timsales-sales' . $suffix . '-' . $stamp . '.csv';
    } else {
        throw new AppException('ไม่รู้จักชนิดไฟล์ที่ต้องการส่งออก');
    }
} catch (Exception $e) {
    flash('error', error_message('ส่งออกไม่สำเร็จ', $e));
    redirect('data.php');
}

header('Content-Type: text/csv; charset=utf-8');
header('Content-Disposition: attachment; filename="' . $name . '"');
header('Cache-Control: no-store');
$out = fopen('php://output', 'w');
foreach ($rows as $row) {
    fputcsv($out, $row);
}
fclose($out);
