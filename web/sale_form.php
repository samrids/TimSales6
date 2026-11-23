<?php
// Add / edit a sale — port of AddSalesActivity + EditSalesActivity.
// Edit mode also holds the delete button (moved off the list to keep it clean).
require __DIR__ . '/app/bootstrap.php';

$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
$isEdit = $id > 0;
$error = null;
$record = null;
$seasons = array();

// Form values (strings, so the user's input is redisplayed as typed)
$form = array('saleDate' => date('Y-m-d'), 'kg' => '', 'unitPrice' => '');

try {
    $seasons = season_repo()->all();
    if ($isEdit) {
        $record = sales_repo()->find($id);
        if ($record === null) {
            flash('error', 'ไม่พบรายการที่ต้องการแก้ไข');
            redirect(sales_list_url());
        }
        $form = array(
            'saleDate' => $record['saleDate'],
            'kg' => input_number($record['kg']),
            'unitPrice' => input_number($record['unitPrice']),
        );
    }
} catch (Exception $e) {
    $error = error_message('ไม่สามารถโหลดข้อมูลได้', $e);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $error === null) {
    csrf_check();
    $form = array(
        'saleDate' => isset($_POST['saleDate']) ? trim((string)$_POST['saleDate']) : '',
        'kg' => isset($_POST['kg']) ? trim((string)$_POST['kg']) : '',
        'unitPrice' => isset($_POST['unitPrice']) ? trim((string)$_POST['unitPrice']) : '',
    );
    $v = validate_sale($form['saleDate'], $form['kg'], $form['unitPrice']);
    $error = $v['error'];

    if ($error === null) {
        try {
            if ($isEdit) {
                sales_repo()->update($id, $v['saleDate'], $v['kg'], $v['unitPrice']);
                flash('success', 'แก้ไขข้อมูลเรียบร้อยแล้ว');
            } else {
                sales_repo()->create($v['saleDate'], $v['kg'], $v['unitPrice']);
                flash('success', 'บันทึกข้อมูลเรียบร้อยแล้ว');
            }
            redirect(sales_list_url());
        } catch (Exception $e) {
            $error = error_message('เกิดข้อผิดพลาด', $e);
        }
    }
}

// Season ranges for the live "this date belongs to …" hint (earliest start wins, as in SQL)
$ranges = array();
foreach ($seasons as $s) {
    $ranges[] = array('code' => $s->code(), 'start' => $s->startDate, 'end' => $s->endDate);
}
usort($ranges, function ($a, $b) { return strcmp($a['start'], $b['start']); });

$pageTitle = $isEdit ? 'แก้ไขยอดขาย' : 'เพิ่มยอดขาย';
$backUrl = sales_list_url();
require APP_DIR . '/views/header.php';
?>
<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?></span></div>
<?php endif; ?>

<form method="post" action="<?php echo h(url('sale_form.php', array('id' => $isEdit ? $id : null))); ?>" class="card form" data-sale-form novalidate>
  <?php echo csrf_field(); ?>
  <div class="field">
    <label class="field-label" for="saleDate">วันที่ขาย</label>
    <input type="date" id="saleDate" name="saleDate" value="<?php echo h($form['saleDate']); ?>" required>
    <p class="field-hint" data-season-hint aria-live="polite"></p>
  </div>
  <div class="field-row">
    <div class="field">
      <label class="field-label" for="kg">น้ำหนัก</label>
      <div class="input-affix">
        <input type="text" id="kg" name="kg" inputmode="decimal" placeholder="0" value="<?php echo h($form['kg']); ?>" required autocomplete="off">
        <span>กก.</span>
      </div>
    </div>
    <div class="field">
      <label class="field-label" for="unitPrice">ราคาต่อกิโลกรัม</label>
      <div class="input-affix">
        <input type="text" id="unitPrice" name="unitPrice" inputmode="decimal" placeholder="0.00" value="<?php echo h($form['unitPrice']); ?>" required autocomplete="off">
        <span>บาท</span>
      </div>
    </div>
  </div>
  <div class="total-box">
    <span>ยอดรวม</span>
    <strong><span data-total>0.00</span> <small>บาท</small></strong>
  </div>
  <div class="form-buttons">
    <a class="btn btn-quiet" href="<?php echo h($backUrl); ?>">ยกเลิก</a>
    <button type="submit" class="btn btn-primary"><?php echo icon('check', 18); ?> บันทึก</button>
  </div>
  <script type="application/json" id="seasonRanges"><?php echo json_for_html($ranges); ?></script>
</form>

<?php if ($isEdit && $record !== null): ?>
  <p class="meta-line">สร้างเมื่อ <?php echo h(fmt_timestamp($record['createdAt'])); ?> · แก้ไขล่าสุด <?php echo h(fmt_timestamp($record['updatedAt'])); ?></p>
  <form method="post" action="sale_delete.php" class="danger-zone"
        data-confirm="ลบรายการขายวันที่ <?php echo h(fmt_date_th($record['saleDate'])); ?> ยอด <?php echo h(fmt_money($record['amount'])); ?> ใช่หรือไม่?&#10;&#10;ลบแล้วกู้คืนไม่ได้">
    <?php echo csrf_field(); ?>
    <input type="hidden" name="id" value="<?php echo (int)$record['id']; ?>">
    <button type="submit" class="btn btn-danger-quiet"><?php echo icon('trash', 18); ?> ลบรายการนี้</button>
  </form>
<?php endif; ?>
<?php require APP_DIR . '/views/footer.php'; ?>
