<?php
// Data — CSV import (sales or seasons, e.g. a Grist export), CSV export and
// a download of the whole SQLite file as a backup.
require __DIR__ . '/app/bootstrap.php';

define('MAX_UPLOAD_BYTES', 2 * 1024 * 1024);

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    csrf_check();
    $f = isset($_FILES['csv']) ? $_FILES['csv'] : null;
    try {
        if ($f === null || $f['error'] === UPLOAD_ERR_NO_FILE) {
            throw new AppException('กรุณาเลือกไฟล์ CSV');
        }
        if ($f['error'] === UPLOAD_ERR_INI_SIZE || $f['error'] === UPLOAD_ERR_FORM_SIZE || $f['size'] > MAX_UPLOAD_BYTES) {
            throw new AppException('ไฟล์ใหญ่เกิน 2 MB');
        }
        if ($f['error'] !== UPLOAD_ERR_OK || !is_uploaded_file($f['tmp_name'])) {
            throw new AppException('อัปโหลดไฟล์ไม่สำเร็จ (รหัส ' . (int)$f['error'] . ')');
        }
        $r = CsvImporter::import($f['tmp_name'], sales_repo(), season_repo());
        $r['file'] = basename((string)$f['name']);
        $_SESSION['import_result'] = $r;
    } catch (Exception $e) {
        flash('error', error_message('นำเข้าไม่สำเร็จ', $e));
    }
    redirect('data.php');
}

$result = isset($_SESSION['import_result']) ? $_SESSION['import_result'] : null;
unset($_SESSION['import_result']);

$error = null;
$salesCount = 0;
$seasonCount = 0;
$dbSize = 0;
try {
    $salesCount = sales_repo()->count();
    $seasonCount = count(season_repo()->all());
    $dbSize = is_file(db_path()) ? filesize(db_path()) : 0;
} catch (Exception $e) {
    $error = error_message('เปิดฐานข้อมูลไม่ได้', $e);
}

$pageTitle = 'ข้อมูล';
$activeNav = 'data';
require APP_DIR . '/views/header.php';
?>
<section class="page-head">
  <div>
    <h1 class="page-title">ข้อมูล</h1>
    <p class="muted">นำเข้า ส่งออก และสำรองฐานข้อมูล</p>
  </div>
</section>

<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?></span></div>
<?php endif; ?>

<?php if ($result !== null): ?>
  <div class="alert <?php echo empty($result['errors']) ? 'alert-success' : 'alert-warn'; ?>">
    <?php echo icon(empty($result['errors']) ? 'check' : 'alert', 18); ?>
    <span>
      นำเข้า<?php echo $result['type'] === 'sales' ? 'รายการขาย' : 'รอบการขาย'; ?>จาก <?php echo h($result['file']); ?>:
      เพิ่ม <strong><?php echo (int)$result['added']; ?></strong> รายการ,
      ข้ามรายการซ้ำ <?php echo (int)$result['skipped']; ?> รายการ<?php echo empty($result['errors']) ? '' : ', ผิดพลาด ' . count($result['errors']) . ' แถว'; ?>
      <?php if (!empty($result['errors'])): ?>
        <ul class="error-lines"><?php foreach ($result['errors'] as $line): ?><li><?php echo h($line); ?></li><?php endforeach; ?></ul>
      <?php endif; ?>
    </span>
  </div>
<?php endif; ?>

<section class="stats stats-2">
  <div class="card stat"><div class="stat-label">รายการขาย</div><div class="stat-value"><?php echo (int)$salesCount; ?></div></div>
  <div class="card stat"><div class="stat-label">รอบการขาย</div><div class="stat-value"><?php echo (int)$seasonCount; ?></div></div>
</section>

<section class="card">
  <div class="card-head"><h2 class="card-title">ส่งออกและสำรองข้อมูล</h2></div>
  <ul class="action-list">
    <li>
      <a href="export.php?type=db">
        <?php echo icon('database', 20); ?>
        <span><strong>สำรองข้อมูลทั้งหมด</strong><small>ไฟล์ .sqlite ไฟล์เดียว · <?php echo h(number_format($dbSize / 1024, 1)); ?> KB</small></span>
        <?php echo icon('download', 18); ?>
      </a>
    </li>
    <li>
      <a href="export.php?type=sales">
        <?php echo icon('table', 20); ?>
        <span><strong>รายการขายทั้งหมด</strong><small>CSV · นำกลับเข้ามาได้</small></span>
        <?php echo icon('download', 18); ?>
      </a>
    </li>
    <li>
      <a href="export.php?type=seasons">
        <?php echo icon('table', 20); ?>
        <span><strong>รอบการขาย</strong><small>CSV · นำกลับเข้ามาได้</small></span>
        <?php echo icon('download', 18); ?>
      </a>
    </li>
  </ul>
</section>

<?php /* phones: the only logout (the top-bar icon shows on wide screens only) */ ?>
<section class="card logout-card">
  <ul class="action-list">
    <li>
      <a href="logout.php">
        <?php echo icon('logout', 20); ?>
        <span><strong>ออกจากระบบ</strong></span>
      </a>
    </li>
  </ul>
</section>

<section class="card">
  <div class="card-head"><h2 class="card-title">นำเข้าจาก CSV</h2></div>
  <p class="muted">ไฟล์ที่ส่งออกจากหน้านี้หรือจาก Grist · รายการที่มีอยู่แล้วจะถูกข้าม นำเข้าไฟล์เดิมซ้ำได้อย่างปลอดภัย</p>
  <details class="details">
    <summary>รูปแบบไฟล์ที่รองรับ</summary>
    <p class="muted small">ระบบดูจากแถวแรกว่าเป็นไฟล์ชนิดใด:</p>
    <ul class="plain-list muted small">
      <li><strong>รายการขาย</strong> — คอลัมน์ <code>saleDate, kg, unitPrice</code> (มี <code>created_at, updated_at</code> ได้)</li>
      <li><strong>รอบการขาย</strong> — คอลัมน์ <code>saleYear, startDate, endDate</code></li>
    </ul>
    <p class="muted small">รายการที่ถือว่าซ้ำ: วันที่ น้ำหนัก และราคาตรงกัน หรือปีของรอบซ้ำกัน</p>
  </details>
  <form method="post" action="data.php" enctype="multipart/form-data" class="upload-form">
    <?php echo csrf_field(); ?>
    <input type="hidden" name="MAX_FILE_SIZE" value="<?php echo MAX_UPLOAD_BYTES; ?>">
    <label class="file-drop">
      <?php echo icon('upload', 22); ?>
      <span data-file-name>เลือกไฟล์ .csv</span>
      <input type="file" name="csv" accept=".csv,text/csv" required>
    </label>
    <button type="submit" class="btn btn-primary" data-busy-label="กำลังนำเข้า…"><?php echo icon('upload', 18); ?> นำเข้า</button>
  </form>
</section>
<?php require APP_DIR . '/views/footer.php'; ?>
