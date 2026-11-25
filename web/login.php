<?php
define('PUBLIC_PAGE', true);
require __DIR__ . '/app/bootstrap.php';

if (is_logged_in()) {
    redirect('index.php');
}

$hash = env('APP_PASSWORD_HASH');
$setupMode = ($hash === '');
$error = '';
$generated = null;

if (isset($_SERVER['REQUEST_METHOD']) && $_SERVER['REQUEST_METHOD'] === 'POST') {
    csrf_check();
    $password = isset($_POST['password']) ? (string)$_POST['password'] : '';

    if ($setupMode) {
        // First-time setup: turn a chosen password into a hash for .env.
        // Login stays disabled until APP_PASSWORD_HASH is set.
        if (strlen($password) < 8) {
            $error = 'รหัสผ่านต้องยาวอย่างน้อย 8 ตัวอักษร';
        } else {
            $generated = password_hash($password, PASSWORD_DEFAULT);
        }
    } elseif ($password !== '' && password_verify($password, $hash)) {
        session_regenerate_id(true);
        $_SESSION['auth'] = true;
        $_SESSION['auth_fp'] = auth_fingerprint();
        $_SESSION['csrf'] = random_hex(32);
        // Back to the page the user was on when the session ran out
        $back = isset($_SESSION['return_to']) ? $_SESSION['return_to'] : '';
        unset($_SESSION['return_to']);
        redirect(is_safe_return_url($back) ? $back : 'index.php');
    } else {
        sleep(1); // slow down password guessing
        $error = 'รหัสผ่านไม่ถูกต้อง';
    }
}

$pageTitle = 'เข้าสู่ระบบ';
$hideNav = true;
require APP_DIR . '/views/header.php';
?>
<section class="card login-card">
  <div class="login-mark"><?php echo icon('leaf', 28); ?></div>
  <h1 class="login-title">TimSales</h1>

  <?php if ($setupMode): ?>
    <p class="muted center">ตั้งค่าครั้งแรก: ยังไม่มี <code>APP_PASSWORD_HASH</code> ในไฟล์ <code>.env</code><br>
      พิมพ์รหัสผ่านที่ต้องการ แล้วคัดลอกค่าที่ได้ไปใส่ใน <code>.env</code></p>
  <?php else: ?>
    <p class="muted center">ระบบบันทึกยอดขายสินค้าเกษตร</p>
  <?php endif; ?>

  <?php if ($error !== ''): ?>
    <div class="alert alert-error" id="login-error" role="alert"><?php echo icon('alert', 18); ?><span><?php echo h($error); ?></span></div>
  <?php endif; ?>

  <?php if ($generated !== null): ?>
    <div class="alert alert-success">
      <?php echo icon('check', 18); ?>
      <span>เพิ่มบรรทัดนี้ในไฟล์ <code>.env</code> แล้วโหลดหน้านี้ใหม่:
        <pre class="hash-output">APP_PASSWORD_HASH=<?php echo h($generated); ?></pre></span>
    </div>
  <?php endif; ?>

  <form method="post" action="login.php" class="form">
    <?php echo csrf_field(); ?>
    <div class="field">
      <label class="field-label" for="password"><?php echo $setupMode ? 'รหัสผ่านใหม่' : 'รหัสผ่าน'; ?></label>
      <input type="password" id="password" name="password" required autofocus<?php echo $error !== '' ? ' aria-invalid="true" aria-describedby="login-error"' : ''; ?>
             autocomplete="<?php echo $setupMode ? 'new-password' : 'current-password'; ?>">
    </div>
    <button type="submit" class="btn btn-primary btn-block" data-busy-label="<?php echo $setupMode ? 'กำลังสร้าง…' : 'กำลังเข้าสู่ระบบ…'; ?>">
      <?php echo $setupMode ? 'สร้างค่า APP_PASSWORD_HASH' : 'เข้าสู่ระบบ'; ?>
    </button>
  </form>
</section>
<?php require APP_DIR . '/views/footer.php'; ?>
