<?php
// Dashboard — season picker; "this month" card (vs last month), a big add button and the
// season total; one monthly
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

// "How am I doing now": this calendar month, or the latest month with sales once the
// season has ended; compared with the month before it.
$now = null;
$prev = null;
$nowYm = date('Y-m');
$isCurrent = false;
foreach ($monthly as $i => $p) {
    if ($p['yearMonth'] === $nowYm) {
        $now = $p;
        $prev = $i > 0 ? $monthly[$i - 1] : null;
        $isCurrent = true;
        break;
    }
}
if ($now === null) {
    for ($i = count($monthly) - 1; $i >= 0; $i--) {
        if ($monthly[$i]['count'] > 0) {
            $now = $monthly[$i];
            $prev = $i > 0 ? $monthly[$i - 1] : null;
            break;
        }
    }
}
$delta = null; // percent change in revenue vs the month before
$deltaLabel = 'จากเดือนก่อน';
if ($now !== null && $prev !== null) {
    $prevRevenue = $prev['revenue'];
    $today = (int)date('j');
    if ($isCurrent && $today < (int)date('t')) {
        // This month isn't over: compare like with like — the same days of last month (1st–today)
        $prevRevenue = 0.0;
        foreach ($sales as $r) {
            if (substr($r['saleDate'], 0, 7) === $prev['yearMonth'] && (int)substr($r['saleDate'], 8, 2) <= $today) {
                $prevRevenue += $r['amount'];
            }
        }
        $deltaLabel = 'จาก ' . ($today > 1 ? '1–' : '') . $today . ' ' . thai_month_short((int)substr($prev['yearMonth'], 5, 2));
    }
    if ($prevRevenue > 0) {
        $delta = ($now['revenue'] - $prevRevenue) / $prevRevenue * 100;
    }
}
// A season with no sales yet: one empty card instead of zeros, an empty chart and an empty list
$isEmpty = $season !== null && $error === null && (int)$stats['recordCount'] === 0;

$pageTitle = $season !== null ? 'ภาพรวม ' . $season->label() : 'ภาพรวม';
$srHeading = true;
$activeNav = 'dashboard';
$useCharts = !empty($monthly) && !$isEmpty;
// Floating add button: hidden while the big "เพิ่มรายการขาย" button under the month card is on
// screen (app.js), shown once it scrolls away
if ($season !== null && !$isEmpty) {
    $fabUrl = 'sale_form.php';
    $fabLabel = 'เพิ่มรายการขาย';
}
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
<?php if ($isEmpty): ?>
<section class="card empty">
  <div class="empty-icon"><?php echo icon('list', 28); ?></div>
  <p>ยังไม่มีรายการขายใน<?php echo h($season->label()); ?><br>
    <span class="small"><?php echo h(season_range($season)); ?></span></p>
  <a class="btn btn-primary" href="sale_form.php"><?php echo icon('plus', 18); ?> เพิ่มรายการขาย</a>
</section>
<?php else: ?>
<div class="dash-top">
  <section class="now-card" aria-labelledby="now-title">
    <div class="now-head">
      <h2 class="now-label" id="now-title"><?php echo $isCurrent ? 'เดือนนี้' : 'เดือนล่าสุด'; ?> · <?php echo h($now !== null ? $now['title'] : $season->label()); ?></h2>
      <?php if ($delta !== null): ?>
        <span class="delta <?php echo $delta >= 0 ? 'is-up' : 'is-down'; ?>">
          <span aria-hidden="true"><?php echo $delta >= 0 ? '▲' : '▼'; ?></span>
          <span class="sr-only"><?php echo $delta >= 0 ? 'เพิ่มขึ้น' : 'ลดลง'; ?></span>
          <?php echo h(number_format(abs($delta), 0)); ?>% <?php echo h($deltaLabel); ?>
        </span>
      <?php endif; ?>
    </div>
    <div class="now-value"><?php echo money_hero($now !== null ? $now['revenue'] : 0); ?></div>
    <?php if ($now !== null && $now['count'] > 0): ?>
      <dl class="now-stats">
        <div><dt>ปริมาณ</dt><dd><?php echo h(fmt_trim($now['kg'])); ?> <small>กก.</small></dd></div>
        <div><dt>ราคาเฉลี่ย</dt><dd><?php echo h(fmt_number($now['price'])); ?> <small>บาท/กก.</small></dd></div>
        <div><dt>รายการ</dt><dd><?php echo (int)$now['count']; ?></dd></div>
      </dl>
    <?php else: ?>
      <p class="now-empty"><?php echo $isCurrent ? 'ยังไม่มียอดขายเดือนนี้' : 'ยังไม่มียอดขายในรอบนี้'; ?></p>
    <?php endif; ?>
  </section>

  <a class="btn btn-primary btn-xl" href="sale_form.php" data-fab-anchor><?php echo icon('plus', 22); ?> เพิ่มรายการขาย</a>

  <section class="season-strip" aria-label="สรุปทั้งรอบ">
    <div class="season-strip-head">
      <span class="season-strip-title">ทั้ง<?php echo h($season->label()); ?></span>
      <?php echo season_chip($season); ?>
    </div>
    <div class="season-strip-total"><?php echo money_hero($stats['totalAmount']); ?></div>
    <p class="season-strip-sub">
      <?php if ($stats['recordCount'] > 0): /* 0 here only after a load error */ ?>
        <?php echo h(fmt_trim($stats['totalKg'])); ?> กก. · เฉลี่ย <?php echo h(fmt_number($stats['averageUnitPrice'])); ?> บาท/กก. · <?php echo (int)$stats['recordCount']; ?> รายการ
      <?php endif; ?>
    </p>
    <p class="season-strip-range"><?php echo h(season_range($season)); ?></p>
  </section>
</div>

<?php if (!empty($monthly)): ?>
<?php /* data-now-month: the month the top card shows — its bar is drawn in the full colour */ ?>
<section class="card chart-card" data-chart-card data-now-month="<?php echo $now !== null ? h($now['yearMonth']) : ''; ?>">
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
  <ul class="sale-list">
    <?php foreach ($recent as $r) { echo sale_row($r); } ?>
  </ul>
</section>
<?php endif; /* $isEmpty */ ?>
<?php endif; /* $season */ ?>

<div class="modal" id="chartModal" hidden role="dialog" aria-modal="true" aria-labelledby="chartModalTitle">
  <div class="modal-inner">
    <div class="card-head">
      <h2 class="card-title" id="chartModalTitle"></h2>
      <button type="button" class="icon-btn" data-close-modal aria-label="ปิด" title="ปิด"><?php echo icon('close', 20); ?></button>
    </div>
    <div class="modal-chart"><canvas id="chartModalCanvas" role="img"></canvas></div>
  </div>
</div>
<?php require APP_DIR . '/views/footer.php'; ?>
