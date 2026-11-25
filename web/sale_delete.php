<?php
require __DIR__ . '/app/bootstrap.php';
require_post();
csrf_check();

$id = isset($_POST['id']) ? (int)$_POST['id'] : 0;
try {
    if ($id > 0 && sales_repo()->delete($id)) {
        flash('success', 'ลบรายการขายแล้ว');
    } else {
        flash('error', 'ไม่พบรายการที่ต้องการลบ');
    }
} catch (Exception $e) {
    flash('error', error_message('ไม่สามารถลบข้อมูลได้', $e));
}
redirect(sales_list_url());
