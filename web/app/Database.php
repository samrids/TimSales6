<?php
// ============================================================
// Database.php — SQLite connection + schema (replaces GristClient.php).
// One file holds everything: data/timsales.sqlite by default, or DB_PATH
// in .env. The schema is created/upgraded automatically on first use.
// PHP 5.6 safe — avoids SQLite features newer than 3.8 (no UPSERT,
// no window functions, no RETURNING).
// ============================================================

/** Error whose message is safe and meant to be shown to the user. */
class AppException extends Exception {}

/** Absolute path of the SQLite file. */
function db_path() {
    $p = env('DB_PATH');
    if ($p === '') {
        return APP_ROOT . '/data/timsales.sqlite';
    }
    // Relative paths are relative to the web/ folder
    $isAbsolute = $p[0] === '/' || $p[0] === '\\' || preg_match('/^[A-Za-z]:[\\\\\/]/', $p);
    return $isAbsolute ? $p : APP_ROOT . '/' . $p;
}

/** Shared PDO connection (created on first call). */
function db() {
    static $pdo = null;
    if ($pdo !== null) {
        return $pdo;
    }
    if (!class_exists('PDO') || !in_array('sqlite', PDO::getAvailableDrivers(), true)) {
        throw new AppException('เซิร์ฟเวอร์ไม่มีส่วนขยาย pdo_sqlite — กรุณาเปิดใช้งานใน PHP');
    }

    $path = db_path();
    $dir = dirname($path);
    if (!is_dir($dir) && !@mkdir($dir, 0775, true)) {
        throw new AppException('สร้างโฟลเดอร์ฐานข้อมูลไม่ได้: ' . basename($dir) . '/');
    }
    // SQLite writes a journal file next to the database, so the folder must be writable
    if (!is_writable($dir) || (is_file($path) && !is_writable($path))) {
        throw new AppException('โฟลเดอร์ฐานข้อมูลเขียนไม่ได้: ' . basename($dir) . '/ — ตั้งสิทธิ์เป็น 775 (หรือ 777 บน shared host)');
    }

    $pdo = new PDO('sqlite:' . $path);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->setAttribute(PDO::ATTR_TIMEOUT, 5); // wait up to 5 s if another request holds the write lock
    db_migrate($pdo);
    return $pdo;
}

/**
 * Create or upgrade the schema. PRAGMA user_version records the version,
 * so each step runs once. Add new steps at the end; never edit old ones.
 */
function db_migrate($pdo) {
    $version = (int)$pdo->query('PRAGMA user_version')->fetchColumn();

    if ($version < 1) {
        $pdo->beginTransaction();
        // seasons — one row per sales season (was the Grist TimYear table)
        $pdo->exec('CREATE TABLE IF NOT EXISTS seasons (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            sale_year   INTEGER NOT NULL UNIQUE,   -- 2026 = season 2026/27
            start_date  TEXT    NOT NULL,          -- Y-m-d, inclusive
            end_date    TEXT    NOT NULL,          -- Y-m-d, inclusive
            created_at  TEXT    NOT NULL,          -- Y-m-d H:i:s, Asia/Bangkok
            updated_at  TEXT    NOT NULL
        )');
        // sales — one row per sale (was the Grist TimSales table).
        // amount (kg × unitPrice) and the season are derived, not stored.
        $pdo->exec('CREATE TABLE IF NOT EXISTS sales (
            id          INTEGER PRIMARY KEY AUTOINCREMENT,
            sale_date   TEXT    NOT NULL,          -- Y-m-d
            kg          REAL    NOT NULL,
            unit_price  REAL    NOT NULL,
            created_at  TEXT    NOT NULL,
            updated_at  TEXT    NOT NULL
        )');
        $pdo->exec('CREATE INDEX IF NOT EXISTS idx_sales_date ON sales (sale_date)');
        $pdo->exec('PRAGMA user_version = 1');
        $pdo->commit();
    }
}

/** Current local time for created_at / updated_at. */
function db_now() {
    return date('Y-m-d H:i:s');
}
