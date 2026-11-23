<?php
// Dashboard — season picker, headline revenue, 3 stat tiles, one monthly
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

// Sparkline series: months up to now only (future months would draw a fake drop to 0)
$spark = array('revenue' => array(), 'kg' => array(), 'price' => array(), 'count' => array());
$peak = null;
foreach ($monthly as $p) {
    if ($p['future']) {
        continue;
    }
    $spark['revenue'][] = $p['revenue'];
    $spark['kg'][] = $p['kg'];
    $spark['price'][] = $p['price'];
    $spark['count'][] = $p['count'];
    if ($p['revenue'] > 0 && ($peak === null || $p['revenue'] > $peak['revenue'])) {
        $peak = $p;
    }
}
$recent = array_slice(array_reverse($sales), 0, 5);

$pageTitle = $season !== null ? 'ภาพรวม ' . $season->label() : 'ภาพรวม';
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
<section class="page-head">
  <form method="get" action="index.php" class="season-switch">
    <label for="season" class="sr-only">รอบการขาย</label>
    <select id="season" name="season" data-autosubmit>
      <?php foreach ($seasons as $s): ?>
        <option value="<?php echo h($s->code()); ?>"<?php echo $s->code() === $season->code() ? ' selected' : ''; ?>><?php echo h($s->label()); ?></option>
      <?php endforeach; ?>
    </select>
    <noscript><button type="submit" class="btn btn-quiet">แสดง</button></noscript>
  </form>
  <p class="season-meta"><?php echo season_chip($season); ?><span><?php echo h(season_range($season)); ?></span></p>
</section>

<section class="card hero">
  <div>
    <div class="stat-label">ยอดขายรวม</div>
    <div class="hero-value"><?php echo money_hero($stats['totalAmount']); ?></div>
    <p class="hero-sub">
      <?php if ($peak !== null): ?>
        เดือนที่ขายได้มากที่สุด <strong><?php echo h($peak['title']); ?></strong> · <?php echo h(fmt_money($peak['revenue'])); ?>
      <?php else: ?>
        ยังไม่มียอดขายในรอบนี้
      <?php endif; ?>
    </p>
  </div>
  <div class="hero-spark"><?php echo sparkline($spark['revenue']); ?></div>
</section>

<section class="stats">
  <div class="card stat">
    <div class="stat-label">ปริมาณรวม</div>
    <div class="stat-value"><?php echo h(fmt_trim($stats['totalKg'])); ?> <small>กก.</small></div>
    <?php echo sparkline($spark['kg']); ?>
  </div>
  <div class="card stat">
    <div class="stat-label">ราคาเฉลี่ย</div>
    <div class="stat-value"><?php echo h(fmt_number($stats['averageUnitPrice'])); ?> <small>บาท/กก.</small></div>
    <?php echo sparkline($spark['price'], false); ?>
  </div>
  <div class="card stat">
    <div class="stat-label">จำนวนรายการ</div>
    <div class="stat-value"><?php echo (int)$stats['recordCount']; ?> <small>รายการ</small></div>
    <?php echo sparkline($spark['count']); ?>
  </div>
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
