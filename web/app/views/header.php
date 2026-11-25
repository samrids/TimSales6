<?php
// Expects: $pageTitle (string)
// Optional: $srHeading (bool) — page has no visible <h1>; emit a screen-reader-only one
// Optional: $backUrl (string) — sub-page with a back arrow instead of the brand
//           $activeNav ('dashboard'|'sales'|'seasons'|'data') — highlights the nav item
//           $hideNav (bool) — no navigation at all (login)
$pageTitle = isset($pageTitle) ? $pageTitle : 'TimSales';
$backUrl = isset($backUrl) ? $backUrl : null;
$hideNav = isset($hideNav) ? $hideNav : false;
$activeNav = isset($activeNav) ? $activeNav : null;
$navItems = array(
    'dashboard' => array('index.php', 'home', 'ภาพรวม'),
    'sales' => array('sales.php', 'list', 'รายการขาย'),
    'seasons' => array('seasons.php', 'calendar', 'รอบการขาย'),
    'data' => array('data.php', 'database', 'ข้อมูล'),
);
?><!DOCTYPE html>
<html lang="th">
<head>
<meta charset="utf-8">
<?php /* interactive-widget: on Android the on-screen keyboard shrinks the layout, so the sticky
         save bar on forms stays above the keyboard instead of behind it */ ?>
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover, interactive-widget=resizes-content">
<?php /* = --bar-bg, so the phone's status bar blends into the dark top bar */ ?>
<meta name="theme-color" content="#0B2A20" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#08130F" media="(prefers-color-scheme: dark)">
<meta name="color-scheme" content="light dark">
<?php /* Add to home screen: opens full screen with its own icon, like an app */ ?>
<link rel="manifest" href="<?php echo h(asset('manifest.webmanifest')); ?>">
<link rel="icon" type="image/png" sizes="192x192" href="assets/icons/icon-192.png">
<link rel="apple-touch-icon" href="assets/icons/apple-touch-icon.png">
<meta name="mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="apple-mobile-web-app-title" content="TimSales">
<?php /* iPhone app mode: the dark top bar runs up under the status bar (it pads for the safe area) */ ?>
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent">
<meta name="robots" content="noindex, nofollow">
<title><?php echo h($pageTitle); ?> · TimSales</title>
<?php /* Self-hosted font (no Google round-trip). Preload the Thai file: nearly every page is Thai text. */ ?>
<link rel="preload" href="assets/fonts/noto-sans-thai-thai-v29.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="<?php echo h(asset('assets/css/style.css')); ?>">
<?php /* without JS nothing dismisses the toast: show it in the page flow instead */ ?>
<noscript><style>.toast-stack { position: static; transform: none; width: auto; margin-bottom: 14px; } .toast { animation: none; }</style></noscript>
</head>
<body class="<?php echo ($activeNav !== null && !$hideNav) ? 'has-tabbar' : ''; ?>">
<header class="topbar">
  <div class="topbar-inner">
    <?php if ($backUrl !== null): ?>
      <a class="icon-btn" href="<?php echo h($backUrl); ?>" aria-label="ย้อนกลับ" data-back><?php echo icon('back', 22); ?></a>
      <h1 class="topbar-title"><?php echo h($pageTitle); ?></h1>
    <?php else: ?>
      <a class="brand" href="index.php">
        <span class="brand-mark"><?php echo icon('leaf', 18); ?></span>
        <span class="brand-name">TimSales</span>
      </a>
      <?php if (!empty($srHeading)): ?><h1 class="sr-only"><?php echo h($pageTitle); ?></h1><?php endif; ?>
    <?php endif; ?>

    <?php if (!$hideNav && is_logged_in()): ?>
      <nav class="nav-desktop" aria-label="เมนูหลัก">
        <?php foreach ($navItems as $key => $item): ?>
          <a href="<?php echo h($item[0]); ?>"<?php echo $key === $activeNav ? ' class="active" aria-current="page"' : ''; ?>><?php echo h($item[2]); ?></a>
        <?php endforeach; ?>
      </nav>
      <a class="icon-btn topbar-logout" href="logout.php" aria-label="ออกจากระบบ" title="ออกจากระบบ"><?php echo icon('logout', 20); ?></a>
    <?php endif; ?>
  </div>
</header>
<main class="container">
<?php
// Success → toast pinned under the top bar (still visible when the page jumps to the saved row).
// Errors and info stay inline at the top: they explain something and shouldn't vanish.
$flashes = flash_take();
$toasts = array_filter($flashes, function ($f) { return $f['type'] === 'success'; });
?>
<?php if ($toasts): ?>
  <div class="toast-stack" data-toasts>
    <?php foreach ($toasts as $f): ?>
      <div class="toast" role="status"><?php echo icon('check', 18); ?><span><?php echo h($f['message']); ?></span></div>
    <?php endforeach; ?>
  </div>
<?php endif; ?>
<?php foreach ($flashes as $f): ?>
  <?php if ($f['type'] === 'success') { continue; } ?>
  <div class="alert alert-<?php echo h($f['type']); ?>" role="status">
    <?php echo icon($f['type'] === 'error' ? 'alert' : 'info', 18); ?>
    <span><?php echo h($f['message']); ?></span>
  </div>
<?php endforeach; ?>
