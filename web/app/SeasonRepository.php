<?php
// ============================================================
// SeasonRepository.php — CRUD for the seasons table.
// Port of data/repository/TimYearRepository.kt. PHP 5.6 safe.
// ============================================================

class SeasonRepository {
    private $db;

    public function __construct($db) {
        $this->db = $db;
    }

    /** All seasons, newest saleYear first */
    public function all() {
        $list = array();
        $rows = $this->db->query('SELECT * FROM seasons ORDER BY sale_year DESC')->fetchAll();
        foreach ($rows as $row) {
            $list[] = Season::fromRow($row);
        }
        return $list;
    }

    public function find($id) {
        $st = $this->db->prepare('SELECT * FROM seasons WHERE id = ?');
        $st->execute(array((int)$id));
        $row = $st->fetch();
        return $row ? Season::fromRow($row) : null;
    }

    public function findByYear($year) {
        $st = $this->db->prepare('SELECT * FROM seasons WHERE sale_year = ?');
        $st->execute(array((int)$year));
        $row = $st->fetch();
        return $row ? Season::fromRow($row) : null;
    }

    /** Find a season by its code ("2026/27") in a list */
    public static function findByCode($seasons, $code) {
        foreach ($seasons as $s) {
            if ($s->code() === $code) {
                return $s;
            }
        }
        return null;
    }

    /**
     * Add a season. Start/end default to 1 Feb → 31 Mar next year.
     * Throws AppException if the year already exists.
     * $createdAt/$updatedAt are only passed by the CSV importer.
     */
    public function create($saleYear, $startDate = null, $endDate = null, $createdAt = null, $updatedAt = null) {
        $saleYear = (int)$saleYear;
        if ($this->findByYear($saleYear) !== null) {
            throw new AppException('รอบ ' . Season::codeFor($saleYear) . ' มีอยู่ในระบบแล้ว');
        }
        $now = db_now();
        $st = $this->db->prepare('INSERT INTO seasons (sale_year, start_date, end_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?)');
        $st->execute(array(
            $saleYear,
            $startDate !== null ? $startDate : Season::defaultStart($saleYear),
            $endDate !== null ? $endDate : Season::defaultEnd($saleYear),
            $createdAt !== null ? $createdAt : $now,
            $updatedAt !== null ? $updatedAt : ($createdAt !== null ? $createdAt : $now),
        ));
        return (int)$this->db->lastInsertId();
    }

    public function update($id, $saleYear, $startDate, $endDate) {
        $other = $this->findByYear($saleYear);
        if ($other !== null && $other->id !== (int)$id) {
            throw new AppException('รอบ ' . Season::codeFor($saleYear) . ' มีอยู่ในระบบแล้ว');
        }
        $st = $this->db->prepare('UPDATE seasons SET sale_year = ?, start_date = ?, end_date = ?, updated_at = ? WHERE id = ?');
        $st->execute(array((int)$saleYear, $startDate, $endDate, db_now(), (int)$id));
        if ($st->rowCount() === 0) {
            throw new AppException('ไม่พบรอบการขายที่ต้องการแก้ไข');
        }
    }

    public function delete($id) {
        $st = $this->db->prepare('DELETE FROM seasons WHERE id = ?');
        $st->execute(array((int)$id));
        return $st->rowCount() > 0;
    }

    /**
     * Seasons list; if the table is empty, create the current year first
     * (same as TimYearRepository.initializeCurrentYearIfNeeded).
     */
    public function allOrInitialize() {
        $list = $this->all();
        if (empty($list)) {
            $this->create((int)date('Y'));
            $list = $this->all();
        }
        return $list;
    }

    /**
     * Pairs of seasons whose date ranges overlap, as [[older, newer], ...].
     * Sales in an overlap count toward the season that starts first.
     */
    public static function overlaps($seasons) {
        $out = array();
        $n = count($seasons);
        for ($i = 0; $i < $n; $i++) {
            for ($j = $i + 1; $j < $n; $j++) {
                $a = $seasons[$i];
                $b = $seasons[$j];
                if ($a->startDate <= $b->endDate && $b->startDate <= $a->endDate) {
                    $out[] = $a->startDate <= $b->startDate ? array($a, $b) : array($b, $a);
                }
            }
        }
        return $out;
    }
}
