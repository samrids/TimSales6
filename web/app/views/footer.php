<?php
// Optional: $fabUrl / $fabLabel — floating "add" button
//           $useCharts (bool) — load Chart.js
?>
</main>
<?php if (!empty($fabUrl)): ?>
  <a class="fab" href="<?php echo h($fabUrl); ?>" aria-label="<?php echo h($fabLabel); ?>">
    <?php echo icon('plus', 22); ?><span class="fab-label"><?php echo h($fabLabel); ?></span>
  </a>
<?php endif; ?>
<?php if ($activeNav !== null && !$hideNav && is_logged_in()): ?>
  <nav class="tabbar" aria-label="เมนูหลัก">
    <?php foreach ($navItems as $key => $item): ?>
      <a href="<?php echo h($item[0]); ?>"<?php echo $key === $activeNav ? ' class="active" aria-current="page"' : ''; ?>>
        <span class="tab-icon"><?php echo icon($item[1], 22); ?></span><span><?php echo h($item[2]); ?></span>
      </a>
    <?php endforeach; ?>
  </nav>
<?php endif; ?>
<?php if (!empty($useCharts)): ?>
<script src="<?php echo h(asset('assets/js/vendor/chart.min.js')); ?>"></script>
<?php endif; ?>
<script src="<?php echo h(asset('assets/js/app.js')); ?>"></script>
</body>
</html>
