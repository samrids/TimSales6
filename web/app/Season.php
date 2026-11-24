<?php
// ============================================================
// Season.php — one sales season (row in the seasons table).
// Port of data/model/TimYear.kt. PHP 5.6 safe.
// ============================================================

class Season {
    public $id = 0;
    public $saleYear = 0;     // first year of the season, e.g. 2026 = season 2026/27
    public $startDate = '';   // 'Y-m-d', inclusive
    public $endDate = '';     // 'Y-m-d', inclusive
    public $createdAt = null; // 'Y-m-d H:i:s'
    public $updatedAt = null;

    /** Build from a seasons table row */
    public static function fromRow($row) {
        $s = new Season();
        $s->id = (int)$row['id'];
        $s->saleYear = (int)$row['sale_year'];
        $s->startDate = (string)$row['start_date'];
        $s->endDate = (string)$row['end_date'];
        $s->createdAt = $row['created_at'];
        $s->updatedAt = $row['updated_at'];
        return $s;
    }

    /** "2026/27" for a start year */
    public static function codeFor($year) {
        return sprintf('%d/%02d', (int)$year, ((int)$year + 1) % 100);
    }

    /** Default range for a new season: 1 Feb of the year → 31 Mar of the next year */
    public static function defaultStart($year) {
        return sprintf('%04d-02-01', (int)$year);
    }

    public static function defaultEnd($year) {
        return sprintf('%04d-03-31', (int)$year + 1);
    }

    /** "2026/27" */
    public function code() {
        return self::codeFor($this->saleYear);
    }

    /** "รอบ 2026/27" */
    public function label() {
        return 'รอบ ' . $this->code();
    }

    /** Kept for callers written against the Grist version */
    public function effectiveStart() {
        return $this->startDate;
    }

    public function effectiveEnd() {
        return $this->endDate;
    }

    public function containsDate($ymd) {
        return $ymd !== null && $ymd >= $this->startDate && $ymd <= $this->endDate;
    }

    /** 'active' | 'ended' | 'upcoming' relative to today */
    public function status() {
        $today = date('Y-m-d');
        if ($today < $this->startDate) {
            return 'upcoming';
        }
        return $today > $this->endDate ? 'ended' : 'active';
    }

    /** Shape used by the JSON API (same field names as the Android model) */
    public function toApi() {
        return array(
            'id' => $this->id,
            'saleYear' => $this->saleYear,
            'startDate' => $this->startDate,
            'endDate' => $this->endDate,
            'seasonCode' => $this->code(),
            'created_at' => $this->createdAt,
            'updated_at' => $this->updatedAt,
        );
    }
}
