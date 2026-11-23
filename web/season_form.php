<?php
// Add / edit a season — port of AddEditTimYearActivity.
// Start and end dates are now stored (Grist used to compute them with formulas).
require __DIR__ . '/app/bootstrap.php';

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$isEdit = $id > 0;
$error = null;
$season = null;
$year = (int)date('Y');
$form = array('saleYear' => (string)$year, 'startDate' => Season::defaultStart($year), 'endDate' => Season::defaultEnd($year));
$count = 0;

if ($isEdit) {
    try {
        $season = season_repo()->find($id);
        if ($season === null) {
            flash('error', 'ไม่พบรอบการขายที่ต้องการแก้ไข');
            redirect('seasons.php');
        }
        $form = array('saleYear' => (string)$season->saleYear, 'startDate' => $season->startDate, 'endDate' => $season->endDate);
        $count = count(sales_repo()->forSeason($season));
    } catch (Exception $e) {
        $error = error_message('ไม่สามารถโหลดข้อมูลได้', $e);
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $error === null) {
    csrf_check();
    $form = array(
        'saleYear' => isset($_POST['saleYear']) ? trim((string)$_POST['saleYear']) : '',
        'startDate' => isset($_POST['startDate']) ? trim((string)$_POST['startDate']) : '',
        'endDate' => isset($_POST['endDate']) ? trim((string)$_POST['endDate']) : '',
    );
    $v = validate_season($form['saleYear'], $form['startDate'], $form['endDate']);
    $error = $v['error'];
    if ($error === null) {
        try {
            if ($isEdit) {
                season_repo()->update($id, $v['saleYear'], $v['startDate'], $v['endDate']);
                flash('success', 'แก้ไขรอบ ' . Season::codeFor($v['saleYear']) . ' เรียบร้อยแล้ว');
            } else {
                season_repo()->create($v['saleYear'], $v['startDate'], $v['endDate']);
                flash('success', 'เพิ่มรอบ ' . Season::codeFor($v['saleYear']) . ' เรียบร้อยแล้ว');
            }
            redirect('seasons.php');
        } catch (Exception $e) {
            $error = error_message('เกิดข้อผิดพลาด', $e);
        }
    }
}

$pageTitle = $isEdit ? 'แก้ไขรอบการขาย' : 'เพิ่มรอบการขาย';
$backUrl = 'seasons.php';
require APP_DIR . '/views/header.php';
?>
<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?></span></div>
<?php endif; ?>

<form method="post" action="<?php echo h(url('season_form.php', array('id' => $isEdit ? $id : null))); ?>" class="card form" data-season-form novalidate>
  <?php echo csrf_field(); ?>
  <div class="field">
    <label class="field-label" for="saleYear">ปีเริ่มรอบ (ค.ศ.)</label>
    <input type="text" id="saleYear" name="saleYear" inputmode="numeric" maxlength="4" value="<?php echo h($form['saleYear']); ?>" required>
    <p class="field-hint">รหัสรอบ: <strong data-season-code></strong></p>
  </div>
  <div class="field-row">
    <div class="field">
      <label class="field-label" for="startDate">วันเริ่มรอบ</label>
      <input type="date" id="startDate" name="startDate" value="<?php echo h($form['startDate']); ?>" required>
    </div>
    <div class="field">
      <label class="field-label" for="endDate">วันสิ้นสุดรอบ</label>
      <input type="date" id="endDate" name="endDate" value="<?php echo h($form['endDate']); ?>" required>
    </div>
  </div>
  <p class="field-hint">เปลี่ยนปีแล้ววันที่จะปรับเป็นค่าเริ่มต้น (1 ก.พ. – 31 มี.ค. ปีถัดไป) จนกว่าจะแก้วันที่เอง</p>
  <div class="form-buttons">
    <a class="btn btn-quiet" href="seasons.php">ยกเลิก</a>
    <button type="submit" class="btn btn-primary"><?php echo icon('check', 18); ?> บันทึก</button>
  </div>
</form>

<?php if ($isEdit && $season !== null): ?>
  <p class="meta-line">สร้างเมื่อ <?php echo h(fmt_timestamp($season->createdAt)); ?> · แก้ไขล่าสุด <?php echo h(fmt_timestamp($season->updatedAt)); ?></p>
  <form method="post" action="season_delete.php" class="danger-zone"
        data-confirm="ลบ<?php echo h($season->label()); ?> ใช่หรือไม่?&#10;&#10;รายการขาย <?php echo (int)$count; ?> รายการในรอบนี้จะไม่ถูกลบ แต่จะไม่แสดงในภาพรวมจนกว่าจะมีรอบที่ครอบคลุมวันที่นั้น">
    <?php echo csrf_field(); ?>
    <input type="hidden" name="id" value="<?php echo (int)$season->id; ?>">
    <button type="submit" class="btn btn-danger-quiet"><?php echo icon('trash', 18); ?> ลบรอบนี้</button>
  </form>
<?php endif; ?>
<?php require APP_DIR . '/views/footer.php'; ?>
