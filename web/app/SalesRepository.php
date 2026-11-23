<?php
// ============================================================
// SalesRepository.php — sales records + season dashboard.
// Port of data/repository/SalesRepository.kt. PHP 5.6 safe.
//
// A sale is a plain array:
//   id, saleDate ('Y-m-d'), kg, unitPrice, amount, seasonCode,
//   createdAt, updatedAt
//
// Season membership is derived from the date ranges in the seasons table
// (what the Grist seasonCode formula used to do). When ranges overlap, the
// season that starts first wins — the same result the Grist formula gave.
// ============================================================

class SalesRepository {
    /** Start year of the season a sale belongs to (NULL when no season covers its date) */
    const SEASON_YEAR_SQL = '(SELECT s.sale_year FROM seasons s
        WHERE sales.sale_date BETWEEN s.start_date AND s.end_date
        ORDER BY s.start_date, s.id LIMIT 1)';

    private $db;

    public function __construct($db) {
        $this->db = $db;
    }

    private function selectSql() {
        return 'SELECT id, sale_date, kg, unit_price, created_at, updated_at, '
            . self::SEASON_YEAR_SQL . ' AS season_year FROM sales';
    }

    public static function fromRow($row) {
        $kg = (float)$row['kg'];
        $price = (float)$row['unit_price'];
        return array(
            'id' => (int)$row['id'],
            'saleDate' => (string)$row['sale_date'],
            'kg' => $kg,
            'unitPrice' => $price,
            'amount' => round($kg * $price, 2),
            'seasonCode' => $row['season_year'] !== null ? Season::codeFor($row['season_year']) : null,
            'createdAt' => $row['created_at'],
            'updatedAt' => $row['updated_at'],
        );
    }

    private function fetchList($sql, $params) {
        $st = $this->db->prepare($sql);
        $st->execute($params);
        $list = array();
        foreach ($st->fetchAll() as $row) {
            $list[] = self::fromRow($row);
        }
        return $list;
    }

    /** All sales, ordered by sale date then id */
    public function all() {
        return $this->fetchList($this->selectSql() . ' ORDER BY sale_date, id', array());
    }

    /** Sales that belong to a season, ordered by sale date then id */
    public function forSeason($season) {
        return $this->fetchList(
            'SELECT * FROM (' . $this->selectSql() . ') WHERE season_year = ? ORDER BY sale_date, id',
            array($season->saleYear)
        );
    }

    /** Sales whose date falls in no season (they don't appear on any dashboard) */
    public function withoutSeason() {
        return $this->fetchList(
            'SELECT * FROM (' . $this->selectSql() . ') WHERE season_year IS NULL ORDER BY sale_date, id',
            array()
        );
    }

    public function find($id) {
        $list = $this->fetchList($this->selectSql() . ' WHERE id = ?', array((int)$id));
        return empty($list) ? null : $list[0];
    }

    /** $createdAt/$updatedAt are only passed by the CSV importer */
    public function create($saleDate, $kg, $unitPrice, $createdAt = null, $updatedAt = null) {
        $now = db_now();
        $st = $this->db->prepare('INSERT INTO sales (sale_date, kg, unit_price, created_at, updated_at) VALUES (?, ?, ?, ?, ?)');
        $st->execute(array(
            $saleDate,
            (float)$kg,
            (float)$unitPrice,
            $createdAt !== null ? $createdAt : $now,
            $updatedAt !== null ? $updatedAt : ($createdAt !== null ? $createdAt : $now),
        ));
        return (int)$this->db->lastInsertId();
    }

    public function update($id, $saleDate, $kg, $unitPrice) {
        $st = $this->db->prepare('UPDATE sales SET sale_date = ?, kg = ?, unit_price = ?, updated_at = ? WHERE id = ?');
        $st->execute(array($saleDate, (float)$kg, (float)$unitPrice, db_now(), (int)$id));
        if ($st->rowCount() === 0) {
            throw new AppException('ไม่พบรายการที่ต้องการแก้ไข');
        }
    }

    public function delete($id) {
        $st = $this->db->prepare('DELETE FROM sales WHERE id = ?');
        $st->execute(array((int)$id));
        return $st->rowCount() > 0;
    }

    /** True if a sale with the same date, kg and price exists (CSV import de-duplication) */
    public function exists($saleDate, $kg, $unitPrice) {
        $st = $this->db->prepare('SELECT 1 FROM sales WHERE sale_date = ? AND ABS(kg - ?) < 0.0001 AND ABS(unit_price - ?) < 0.0001 LIMIT 1');
        $st->execute(array($saleDate, (float)$kg, (float)$unitPrice));
        return (bool)$st->fetchColumn();
    }

    /** Sale count and revenue per season start year: [year => ['count' => n, 'amount' => x]] */
    public function totalsBySeason() {
        $sql = 'SELECT season_year, COUNT(*) AS n, SUM(kg * unit_price) AS amount FROM ('
            . $this->selectSql() . ') WHERE season_year IS NOT NULL GROUP BY season_year';
        $out = array();
        foreach ($this->db->query($sql)->fetchAll() as $row) {
            $out[(int)$row['season_year']] = array('count' => (int)$row['n'], 'amount' => (float)$row['amount']);
        }
        return $out;
    }

    public function count() {
        return (int)$this->db->query('SELECT COUNT(*) FROM sales')->fetchColumn();
    }

    /** Kept for callers written against the Grist version */
    public static function filterBySeason($records, $season) {
        $out = array();
        foreach ($records as $r) {
            if ($r['seasonCode'] === $season->code()) {
                $out[] = $r;
            }
        }
        return $out;
    }

    /** totalAmount, totalKg, averageUnitPrice (weighted = amount / kg), recordCount */
    public static function statistics($records) {
        $amount = 0.0;
        $kg = 0.0;
        foreach ($records as $r) {
            $amount += $r['amount'];
            $kg += $r['kg'];
        }
        return array(
            'totalAmount' => round($amount, 2),
            'totalKg' => round($kg, 2),
            'averageUnitPrice' => $kg > 0 ? round($amount / $kg, 2) : 0.0,
            'recordCount' => count($records),
        );
    }

    /** 'YYYY-MM' list from start month to end month inclusive (max 60) */
    public static function enumerateMonths($start, $end) {
        $out = array();
        if (!preg_match('/^(\d{4})-(\d{2})/', $start, $s) || !preg_match('/^(\d{4})-(\d{2})/', $end, $e)) {
            return $out;
        }
        $y = (int)$s[1];
        $m = (int)$s[2];
        $endY = (int)$e[1];
        $endM = (int)$e[2];
        $guard = 0;
        while (($y < $endY || ($y === $endY && $m <= $endM)) && $guard < 60) {
            $out[] = sprintf('%04d-%02d', $y, $m);
            $m++;
            if ($m > 12) {
                $m = 1;
                $y++;
            }
            $guard++;
        }
        return $out;
    }

    /**
     * One point per month across the whole season. Months without sales are 0;
     * price is the weighted average for the month (null when nothing was sold);
     * future is true for months after the current one.
     */
    public static function monthlyPoints($season, $records) {
        $byMonth = array();
        foreach ($records as $r) {
            $ym = substr($r['saleDate'], 0, 7);
            if (!isset($byMonth[$ym])) {
                $byMonth[$ym] = array('revenue' => 0.0, 'kg' => 0.0, 'count' => 0);
            }
            $byMonth[$ym]['revenue'] += $r['amount'];
            $byMonth[$ym]['kg'] += $r['kg'];
            $byMonth[$ym]['count']++;
        }
        $nowYm = date('Y-m');
        $points = array();
        foreach (self::enumerateMonths($season->startDate, $season->endDate) as $ym) {
            $b = isset($byMonth[$ym]) ? $byMonth[$ym] : array('revenue' => 0.0, 'kg' => 0.0, 'count' => 0);
            $points[] = array(
                'yearMonth' => $ym,
                'label' => month_label($ym),
                'title' => month_title($ym),
                'revenue' => round($b['revenue'], 2),
                'kg' => round($b['kg'], 2),
                'price' => $b['kg'] > 0 ? round($b['revenue'] / $b['kg'], 2) : null,
                'count' => $b['count'],
                'future' => $ym > $nowYm,
            );
        }
        return $points;
    }
}
