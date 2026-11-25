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
$fabLabel = 'เพิ่มรายการขาย';
require APP_DIR . '/views/header.php';
?>
<?php
// One row: the season links double as the page title (the current one is highlighted),
// so there is no separate heading repeating it. CSV export is a quiet icon at the end.
$tabs = season_tabs($seasons, $season, 'sales.php', count($orphans), $showOrphans);
$csvUrl = url('export.php', array('type' => 'sales', 'season' => $showOrphans ? 'none' : ($season !== null ? $season->code() : null)));
?>
<section class="list-toolbar">
  <?php if ($tabs !== ''): ?>
    <?php echo $tabs; ?>
  <?php else: ?>
    <p class="season-title"><?php echo $showOrphans ? 'ไม่อยู่ในรอบใด' : ($season !== null ? h($season->label()) : 'ทั้งหมด'); ?></p>
  <?php endif; ?>
  <a class="icon-btn" href="<?php echo h($csvUrl); ?>" aria-label="ดาวน์โหลด CSV" title="ดาวน์โหลด CSV"><?php echo icon('download', 20); ?></a>
</section>

<?php if (!empty($orphans) && !$showOrphans): ?>
  <div class="alert alert-warn">
    <?php echo icon('alert', 18); ?>
    <span>มี <?php echo count($orphans); ?> รายการที่วันที่ไม่อยู่ในรอบการขายใดเลย จึงไม่แสดงในภาพรวม —
      <a href="sales.php?season=none">ดูรายการ</a> หรือ <a href="seasons.php">ปรับช่วงวันที่ของรอบ</a></span>
  </div>
<?php endif; ?>

<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?> <a href="<?php echo h(sales_list_url()); ?>">ลองใหม่</a></span></div>
<?php elseif (empty($records)): ?>
  <div class="card empty">
    <div class="empty-icon"><?php echo icon('list', 28); ?></div>
    <p>ยังไม่มีรายการขาย<?php echo $season !== null ? 'ใน' . h($season->label()) : ''; ?></p>
    <a class="btn btn-primary" href="sale_form.php"><?php echo icon('plus', 18); ?> เพิ่มรายการขาย</a>
  </div>
<?php else: ?>
  <?php /* the total only when there is something to add up: after an error ฿0.00 would be wrong */ ?>
  <section class="card list-summary">
    <div class="stat-label">ยอดขายรวม</div>
    <div class="list-summary-total"><?php echo money_hero($stats['totalAmount']); ?></div>
    <p class="list-summary-sub"><?php echo h(fmt_trim($stats['totalKg'])); ?> กก. · <?php echo (int)$stats['recordCount']; ?> รายการ</p>
  </section>
  <?php foreach ($groups as $g): ?>
    <section class="month-group">
      <header class="month-head">
        <h2><?php echo h($g['title']); ?></h2>
        <span><?php echo count($g['rows']); ?> รายการ · <strong><?php echo h(fmt_number($g['amount'])); ?><span class="sr-only"> บาท</span></strong></span>
      </header>
      <ul class="sale-list card">
        <?php foreach ($g['rows'] as $r) { echo sale_row($r, false); } ?>
      </ul>
    </section>
  <?php endforeach; ?>
<?php endif; ?>
<?php require APP_DIR . '/views/footer.php'; ?>
