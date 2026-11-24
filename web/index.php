<?php
// Dashboard — season picker, one summary card (revenue, kg, price, count), one monthly
// chart with a metric switch (revenue / volume / average price) + table view,
// and the latest sales.
require __DIR__ . '/app/bootstrap.php';

$error = null;
$seasons = array();
$season = null;
$sales = array();
$stats = SalesRepository::statistics(array());
$monthly = array();

try {
    $seasons = season_repo()->allOrInitialize();
    $season = pick_season($seasons);
    if ($season !== null) {
        $sales = sales_repo()->forSeason($season);
        $stats = SalesRepository::statistics($sales);
        $monthly = SalesRepository::monthlyPoints($season, $sales);
    }
} catch (Exception $e) {
    $error = error_message('ไม่สามารถโหลดข้อมูลได้', $e);
}

$recent = array_slice(array_reverse($sales), 0, 5);

$pageTitle = $season !== null ? 'ภาพรวม ' . $season->label() : 'ภาพรวม';
$srHeading = true;
$activeNav = 'dashboard';
$useCharts = !empty($monthly);
$fabUrl = 'sale_form.php';
$fabLabel = 'เพิ่มยอดขาย';
require APP_DIR . '/views/header.php';
?>
<?php if ($error !== null): ?>
  <div class="alert alert-error"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?> <a href="index.php">ลองใหม่</a></span></div>
<?php endif; ?>

<?php if ($season !== null): ?>
<?php $tabs = season_tabs($seasons, $season, 'index.php'); ?>
<section class="list-toolbar">
  <?php if ($tabs !== ''): ?>
    <?php echo $tabs; /* the highlighted link is the title — no separate heading repeating it */ ?>
  <?php else: ?>
    <p class="season-title"><?php echo h($season->label()); ?></p>
  <?php endif; ?>
</section>
<p class="season-meta"><?php echo season_chip($season); ?><span><?php echo h(season_range($season)); ?></span></p>

<section class="card list-summary">
  <div class="stat-label">ยอดขายรวม</div>
  <div class="list-summary-total"><?php echo money_hero($stats['totalAmount']); ?></div>
  <p class="list-summary-sub">
    <?php if ($stats['recordCount'] > 0): ?>
      <?php echo h(fmt_trim($stats['totalKg'])); ?> กก. · เฉลี่ย <?php echo h(fmt_number($stats['averageUnitPrice'])); ?> บาท/กก. · <?php echo (int)$stats['recordCount']; ?> รายการ
    <?php else: ?>
      ยังไม่มียอดขายในรอบนี้
    <?php endif; ?>
  </p>
</section>

<?php if (!empty($monthly)): ?>
<section class="card chart-card" data-chart-card>
  <div class="card-head">
    <div>
      <h2 class="card-title" data-chart-title>ยอดขายรายเดือน</h2>
      <p class="card-sub" data-chart-sub>หน่วย: บาท</p>
    </div>
    <div class="card-tools">
      <button type="button" class="icon-btn" data-view-toggle aria-pressed="false" aria-label="แสดงเป็นตาราง" title="แสดงเป็นตาราง"><?php echo icon('table', 20); ?></button>
      <button type="button" class="icon-btn" data-fullscreen aria-label="ขยายเต็มจอ" title="ขยายเต็มจอ"><?php echo icon('expand', 20); ?></button>
    </div>
  </div>

  <div class="segmented" role="group" aria-label="เลือกข้อมูลที่จะแสดง">
    <button type="button" data-metric="revenue" aria-pressed="true">ยอดขาย</button>
    <button type="button" data-metric="kg" aria-pressed="false">ปริมาณ</button>
    <button type="button" data-metric="price" aria-pressed="false">ราคาเฉลี่ย</button>
  </div>

  <div class="chart-box" data-chart-box>
    <canvas id="mainChart" role="img" aria-label="กราฟรายเดือนของ<?php echo h($season->label()); ?> — ดูตัวเลขทั้งหมดได้ในมุมมองตาราง"></canvas>
  </div>

  <div class="table-wrap" data-table-box hidden>
    <table class="data-table">
      <thead>
        <tr><th scope="col">เดือน</th><th scope="col">ยอดขาย<br>(บาท)</th><th scope="col">ปริมาณ<br>(กก.)</th><th scope="col">ราคาเฉลี่ย<br>(บาท/กก.)</th><th scope="col">รายการ</th></tr>
      </thead>
      <tbody>
      <?php foreach ($monthly as $p): ?>
        <tr<?php echo $p['future'] ? ' class="is-future"' : ''; ?>>
          <th scope="row"><abbr title="<?php echo h($p['title']); ?>"><?php echo h($p['label']); ?></abbr></th>
          <td><?php echo $p['count'] > 0 ? h(fmt_number($p['revenue'])) : '–'; ?></td>
          <td><?php echo $p['count'] > 0 ? h(fmt_trim($p['kg'])) : '–'; ?></td>
          <td><?php echo $p['price'] !== null ? h(fmt_number($p['price'])) : '–'; ?></td>
          <td><?php echo $p['count'] > 0 ? (int)$p['count'] : '–'; ?></td>
        </tr>
      <?php endforeach; ?>
      </tbody>
      <tfoot>
        <tr>
          <th scope="row">รวมทั้งรอบ</th>
          <td><?php echo h(fmt_number($stats['totalAmount'])); ?></td>
          <td><?php echo h(fmt_trim($stats['totalKg'])); ?></td>
          <td><?php echo h(fmt_number($stats['averageUnitPrice'])); ?></td>
          <td><?php echo (int)$stats['recordCount']; ?></td>
        </tr>
      </tfoot>
    </table>
  </div>
  <noscript><style>.chart-box,.segmented,.card-tools{display:none}.table-wrap[hidden]{display:block}</style></noscript>
  <script type="application/json" id="monthlyData"><?php echo json_for_html($monthly); ?></script>
</section>
<?php endif; ?>

<section class="card">
  <div class="card-head">
    <h2 class="card-title">รายการล่าสุด</h2>
    <?php if (!empty($recent)): ?>
      <a class="link-more" href="<?php echo h(sales_list_url()); ?>">ดูทั้งหมด <?php echo icon('chevron', 16); ?></a>
    <?php endif; ?>
  </div>
  <?php if (empty($recent)): ?>
    <div class="empty">
      <p>ยังไม่มีรายการขายในรอบนี้</p>
      <a class="btn btn-primary" href="sale_form.php"><?php echo icon('plus', 18); ?> เพิ่มยอดขาย</a>
    </div>
  <?php else: ?>
    <ul class="sale-list">
      <?php foreach ($recent as $r) { echo sale_row($r); } ?>
    </ul>
  <?php endif; ?>
</section>
<?php endif; ?>

<div class="modal" id="chartModal" hidden role="dialog" aria-modal="true" aria-labelledby="chartModalTitle">
  <div class="modal-inner">
    <div class="card-head">
      <h2 class="card-title" id="chartModalTitle"></h2>
      <button type="button" class="icon-btn" data-close-modal aria-label="ปิด" title="ปิด"><?php echo icon('close', 20); ?></button>
    </div>
    <div class="modal-chart"><canvas id="chartModalCanvas"></canvas></div>
  </div>
</div>
<?php require APP_DIR . '/views/footer.php'; ?>
