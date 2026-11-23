<?php
// ============================================================
// CsvImporter.php — import sales or seasons from CSV (e.g. a Grist export).
//
// The file type is detected from the header row:
//   sales:   saleDate, kg, unitPrice [, created_at, updated_at, …]
//   seasons: saleYear [, startDate, endDate, created_at, updated_at, …]
// Other columns (amount, seasonCode, …) are ignored — they are derived.
// Rows already in the database are skipped, so re-importing the same file
// is safe. All rows go in one transaction: an unexpected error imports nothing.
// PHP 5.6 safe.
// ============================================================

class CsvImporter {
    const MAX_ERRORS = 20;

    /**
     * Returns array('type' => 'sales'|'seasons', 'added' => n, 'skipped' => n, 'errors' => [msg, …])
     * Throws AppException if the file can't be read or the header isn't recognised.
     */
    public static function import($path, $salesRepo, $seasonRepo) {
        $fh = @fopen($path, 'r');
        if ($fh === false) {
            throw new AppException('เปิดไฟล์ CSV ไม่ได้');
        }
        $header = fgetcsv($fh);
        if ($header === false || $header === array(null)) {
            fclose($fh);
            throw new AppException('ไฟล์ CSV ว่างเปล่า');
        }
        $cols = self::mapHeader($header);

        if (isset($cols['saledate'], $cols['kg'], $cols['unitprice'])) {
            $type = 'sales';
        } elseif (isset($cols['saleyear'])) {
            $type = 'seasons';
        } else {
            fclose($fh);
            throw new AppException('ไม่รู้จักรูปแบบไฟล์ — แถวแรกต้องมีคอลัมน์ saleDate, kg, unitPrice (รายการขาย) หรือ saleYear (รอบการขาย)');
        }

        $result = array('type' => $type, 'added' => 0, 'skipped' => 0, 'errors' => array());
        $db = db();
        $db->beginTransaction();
        try {
            $line = 1;
            while (($row = fgetcsv($fh)) !== false) {
                $line++;
                if ($row === array(null) || trim(implode('', $row)) === '') {
                    continue; // blank line
                }
                $error = $type === 'sales'
                    ? self::importSale($row, $cols, $salesRepo, $result)
                    : self::importSeason($row, $cols, $seasonRepo, $result);
                if ($error !== null && count($result['errors']) < self::MAX_ERRORS) {
                    $result['errors'][] = 'แถว ' . $line . ': ' . $error;
                }
            }
            $db->commit();
        } catch (Exception $e) {
            $db->rollBack();
            fclose($fh);
            throw $e;
        }
        fclose($fh);
        return $result;
    }

    /** lower-cased, underscore-free column name => index ("unit_price" and "unitPrice" both → "unitprice") */
    private static function mapHeader($header) {
        $cols = array();
        foreach ($header as $i => $name) {
            $name = (string)$name;
            if ($i === 0) {
                $name = preg_replace('/^\xEF\xBB\xBF/', '', $name); // UTF-8 BOM (Excel)
            }
            $key = str_replace(array('_', ' ', '-'), '', strtolower(trim($name)));
            if ($key !== '' && !isset($cols[$key])) {
                $cols[$key] = $i;
            }
        }
        return $cols;
    }

    private static function cell($row, $cols, $key) {
        return (isset($cols[$key]) && isset($row[$cols[$key]])) ? trim((string)$row[$cols[$key]]) : '';
    }

    /** Returns an error message, or null when the row was added or skipped as a duplicate */
    private static function importSale($row, $cols, $repo, &$result) {
        $date = to_ymd(self::cell($row, $cols, 'saledate'));
        $v = validate_sale(
            $date !== null ? $date : self::cell($row, $cols, 'saledate'),
            self::cell($row, $cols, 'kg'),
            self::cell($row, $cols, 'unitprice')
        );
        if ($v['error'] !== null) {
            return $v['error'];
        }
        if ($repo->exists($v['saleDate'], $v['kg'], $v['unitPrice'])) {
            $result['skipped']++;
            return null;
        }
        $repo->create(
            $v['saleDate'], $v['kg'], $v['unitPrice'],
            to_datetime(self::cell($row, $cols, 'createdat')),
            to_datetime(self::cell($row, $cols, 'updatedat'))
        );
        $result['added']++;
        return null;
    }

    private static function importSeason($row, $cols, $repo, &$result) {
        $v = validate_season(
            self::cell($row, $cols, 'saleyear'),
            self::cell($row, $cols, 'startdate'),
            self::cell($row, $cols, 'enddate')
        );
        if ($v['error'] !== null) {
            return $v['error'];
        }
        if ($repo->findByYear($v['saleYear']) !== null) {
            $result['skipped']++;
            return null;
        }
        $repo->create(
            $v['saleYear'], $v['startDate'], $v['endDate'],
            to_datetime(self::cell($row, $cols, 'createdat')),
            to_datetime(self::cell($row, $cols, 'updatedat'))
        );
        $result['added']++;
        return null;
    }
}
