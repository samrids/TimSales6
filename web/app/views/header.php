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
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<meta name="theme-color" content="#F6F8F7" media="(prefers-color-scheme: light)">
<meta name="theme-color" content="#0E1412" media="(prefers-color-scheme: dark)">
<meta name="color-scheme" content="light dark">
<meta name="robots" content="noindex, nofollow">
<title><?php echo h($pageTitle); ?> · TimSales</title>
<?php /* Self-hosted font (no Google round-trip). Preload the Thai file: nearly every page is Thai text. */ ?>
<link rel="preload" href="assets/fonts/noto-sans-thai-thai-v29.woff2" as="font" type="font/woff2" crossorigin>
<link rel="stylesheet" href="<?php echo h(asset('assets/css/style.css')); ?>">
</head>
<body class="<?php echo ($activeNav !== null && !$hideNav) ? 'has-tabbar' : ''; ?>">
<header class="topbar">
  <div class="topbar-inner">
    <?php if ($backUrl !== null): ?>
      <a class="icon-btn" href="<?php echo h($backUrl); ?>" aria-label="ย้อนกลับ"><?php echo icon('back', 22); ?></a>
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
      <a class="icon-btn" href="logout.php" aria-label="ออกจากระบบ" title="ออกจากระบบ"><?php echo icon('logout', 20); ?></a>
    <?php endif; ?>
  </div>
</header>
<main class="container">
<?php foreach (flash_take() as $f): ?>
  <div class="alert alert-<?php echo h($f['type']); ?>" role="status">
    <?php echo icon($f['type'] === 'error' ? 'alert' : ($f['type'] === 'info' ? 'info' : 'check'), 18); ?>
    <span><?php echo h($f['message']); ?></span>
  </div>
<?php endforeach; ?>
