<?php
// Sales list for one season, newest first, grouped by month with subtotals.
// ?season=none lists sales whose date falls in no season (they appear on no dashboard).
require __DIR__ . '/app/bootstrap.php';

$error = null;
$seasons = array();
$season = null;
$records = array();
$orphans = array();
$showOrphans = isset($_GET['season']) && $_GET['season'] === 'none';

try {
    $seasons = season_repo()->all();
    $orphans = sales_repo()->withoutSeason();
    if ($showOrphans) {
        $records = $orphans;
    } else {
        $season = pick_season($seasons);
        $records = $season !== null ? sales_repo()->forSeason($season) : $orphans;
    }
} catch (Exception $e) {
    $error = error_message('ไม่สามารถโหลดรายการได้', $e);
}

$stats = SalesRepository::statistics($records);

// Group newest-first by month: ym => ['title', 'amount', 'kg', 'rows' => [...]]
$groups = array();
foreach (array_reverse($records) as $r) {
    $ym = substr($r['saleDate'], 0, 7);
    if (!isset($groups[$ym])) {
        $groups[$ym] = array('title' => month_title($ym), 'amount' => 0.0, 'kg' => 0.0, 'rows' => array());
    }
    $groups[$ym]['amount'] += $r['amount'];
    $groups[$ym]['kg'] += $r['kg'];
    $groups[$ym]['rows'][] = $r;
}

$pageTitle = 'รายการขาย';
$srHeading = true;
$activeNav = 'sales';
$fabUrl = 'sale_form.php';
$fabLabel = 'เพิ่มยอดขาย';
require APP_DIR . '/views/header.php';
?>
<section class="page-head">
  <p class="season-title"><?php echo $showOrphans ? 'ไม่อยู่ในรอบใด' : ($season !== null ? h($season->label()) : 'ทั้งหมด'); ?></p>
  <a class="btn btn-quiet btn-sm" href="<?php echo h(url('export.php', array('type' => 'sales', 'season' => $showOrphans ? 'none' : ($season !== null ? $season->code() : null)))); ?>">
    <?php echo icon('download', 16); ?> CSV
  </a>
  <?php echo season_tabs($seasons, $season, 'sales.php', count($orphans), $showOrphans); ?>
</section>

<?php if (!empty($orphans) && !$showOrphans): ?>
  <div class="alert alert-warn">
    <?php echo icon('alert', 18); ?>
    <span>มี <?php echo count($orphans); ?> รายการที่วันที่ไม่อยู่ในรอบการขายใดเลย จึงไม่แสดงในภาพรวม —
      <a href="sales.php?season=none">ดูรายการ</a> หรือ <a href="seasons.php">ปรับช่วงวันที่ของรอบ</a></span>
  </div>
<?php endif; ?>

<section class="stats">
  <div class="card stat"><div class="stat-label">ยอดขาย</div><div class="stat-value"><?php echo h(fmt_number($stats['totalAmount'])); ?> <small>บาท</small></div></div>
  <div class="card stat"><div class="stat-label">ปริมาณ</div><div class="stat-value"><?php echo h(fmt_trim($stats['totalKg'])); ?> <small>กก.</small></div></div>
  <div class="card stat"><div class="stat-label">รายการ</div><div class="stat-value"><?php echo (int)$stats['recordCount']; ?> <small>รายการ</small></div></div>
</section>

<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?> <a href="<?php echo h(sales_list_url()); ?>">ลองใหม่</a></span></div>
<?php elseif (empty($records)): ?>
  <div class="card empty">
    <div class="empty-icon"><?php echo icon('list', 28); ?></div>
    <p>ยังไม่มีรายการขาย<?php echo $season !== null ? 'ใน' . h($season->label()) : ''; ?></p>
    <a class="btn btn-primary" href="sale_form.php"><?php echo icon('plus', 18); ?> เพิ่มยอดขาย</a>
  </div>
<?php else: ?>
  <?php foreach ($groups as $g): ?>
    <section class="card month-group">
      <header class="month-head">
        <h2><?php echo h($g['title']); ?></h2>
        <span><?php echo count($g['rows']); ?> รายการ · <strong><?php echo h(fmt_number($g['amount'])); ?></strong> บาท</span>
      </header>
      <ul class="sale-list">
        <?php foreach ($g['rows'] as $r) { echo sale_row($r); } ?>
      </ul>
    </section>
  <?php endforeach; ?>
<?php endif; ?>
<?php require APP_DIR . '/views/footer.php'; ?>
