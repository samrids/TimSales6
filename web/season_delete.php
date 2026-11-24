<?php
require __DIR__ . '/app/bootstrap.php';
require_post();
csrf_check();

$id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
try {
    if ($id > 0 && season_repo()->delete($id)) {
        flash('success', 'ลบรอบการขายเรียบร้อยแล้ว');
    } else {
        flash('error', 'ไม่พบรอบการขายที่ต้องการลบ');
    }
} catch (Exception $e) {
    flash('error', error_message('ไม่สามารถลบรอบการขายได้', $e));
}
redirect('seasons.php');
