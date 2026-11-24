<?php
// Season management — port of TimYearManagementActivity + TimYearAdapter
require __DIR__ . '/app/bootstrap.php';

$error = null;
$seasons = array();
$totals = array();
try {
    $seasons = season_repo()->all();
    $totals = sales_repo()->totalsBySeason();
} catch (Exception $e) {
    $error = error_message('ไม่สามารถโหลดรอบการขายได้', $e);
}
$overlaps = SeasonRepository::overlaps($seasons);

$pageTitle = 'รอบการขาย';
$activeNav = 'seasons';
$fabUrl = 'season_form.php';
$fabLabel = 'เพิ่มรอบการขาย';
require APP_DIR . '/views/header.php';
?>
<section class="page-head">
  <div>
    <h1 class="page-title">รอบการขาย</h1>
    <p class="muted">รายการขายนับเข้ารอบตามวันที่ขาย — รอบใหม่เริ่มต้นที่ 1 ก.พ. ถึง 31 มี.ค. ปีถัดไป (แก้ไขได้)</p>
  </div>
</section>

<?php if (!empty($overlaps)): ?>
  <div class="alert alert-info">
    <?php echo icon('info', 18); ?>
    <span>
      <?php foreach ($overlaps as $i => $pair): ?>
        <?php echo $i > 0 ? '<br>' : ''; ?>
        <?php echo h($pair[0]->label()); ?> และ <?php echo h($pair[1]->label()); ?> มีช่วงวันที่ทับกัน — รายการในช่วงที่ทับจะนับเข้า <?php echo h($pair[0]->label()); ?> (รอบที่เริ่มก่อน)
      <?php endforeach; ?>
    </span>
  </div>
<?php endif; ?>

<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?> <a href="seasons.php">ลองใหม่</a></span></div>
<?php elseif (empty($seasons)): ?>
  <div class="card empty">
    <div class="empty-icon"><?php echo icon('calendar', 28); ?></div>
    <p>ยังไม่มีรอบการขาย</p>
    <a class="btn btn-primary" href="season_form.php"><?php echo icon('plus', 18); ?> เพิ่มรอบการขาย</a>
  </div>
<?php else: ?>
  <ul class="season-list">
  <?php foreach ($seasons as $s):
      $t = isset($totals[$s->saleYear]) ? $totals[$s->saleYear] : array('count' => 0, 'amount' => 0.0); ?>
    <li>
      <a class="card season-card" href="<?php echo h(url('season_form.php', array('id' => $s->id))); ?>">
        <span class="sr-only">แก้ไข</span>
        <div class="season-card-head">
          <h2><?php echo h($s->label()); ?></h2>
          <?php echo season_chip($s); ?>
        </div>
        <p class="season-card-range"><?php echo icon('calendar', 16); ?> <?php echo h(season_range($s)); ?></p>
        <div class="season-card-stats">
          <span><strong><?php echo h(fmt_number($t['amount'])); ?></strong> บาท</span>
          <span><strong><?php echo (int)$t['count']; ?></strong> รายการ</span>
        </div>
      </a>
    </li>
  <?php endforeach; ?>
  </ul>
<?php endif; ?>
<?php require APP_DIR . '/views/footer.php'; ?>
