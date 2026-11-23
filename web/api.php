<?php
// ============================================================
// api.php — JSON API over the same SQLite data, for the Android app.
//
// Auth: API_TOKEN in .env, sent as  Authorization: Bearer <token>
//       or X-Api-Key: <token>  (some shared hosts strip Authorization).
//       Empty / short API_TOKEN = API switched off.
//
// Routes (?r=…):
//   GET    seasons                      → {records: [season…]}
//   POST   seasons    {saleYear, startDate?, endDate?}      → 201 {record}
//   PATCH  seasons&id=N {saleYear, startDate?, endDate?}    → {record}
//   DELETE seasons&id=N                 → {ok: true}
//   GET    sales[&season=2026/27]       → {records: [sale…]}
//   POST   sales      {saleDate, kg, unitPrice}             → 201 {record}
//   PATCH  sales&id=N {saleDate, kg, unitPrice}             → {record}
//   DELETE sales&id=N                   → {ok: true}
//   GET    dashboard[&season=2026/27]   → {season, statistics, monthly}
// Hosts that block PATCH/DELETE: send POST with X-HTTP-Method-Override.
// Field names match the Android models (saleDate, unitPrice, created_at, …).
// Errors: {error: "<Thai message>"} with an HTTP status code.
// PHP 5.6 safe.
// ============================================================

define('API_REQUEST', true);
require __DIR__ . '/app/bootstrap.php';

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

function api_send($status, $data) {
    http_response_code($status);
    echo json_encode($data, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function api_fail($status, $message) {
    api_send($status, array('error' => $message));
}

function api_token_sent() {
    $auth = '';
    foreach (array('HTTP_AUTHORIZATION', 'REDIRECT_HTTP_AUTHORIZATION') as $k) {
        if (!empty($_SERVER[$k])) {
            $auth = (string)$_SERVER[$k];
            break;
        }
    }
    if ($auth === '' && function_exists('apache_request_headers')) {
        foreach (apache_request_headers() as $name => $value) {
            if (strtolower($name) === 'authorization') {
                $auth = (string)$value;
            }
        }
    }
    if (preg_match('/^Bearer\s+(\S+)$/i', trim($auth), $m)) {
        return $m[1];
    }
    return isset($_SERVER['HTTP_X_API_KEY']) ? trim((string)$_SERVER['HTTP_X_API_KEY']) : '';
}

/** JSON request body as an array ({} when empty) */
function api_body() {
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return array();
    }
    $data = json_decode($raw, true);
    if (!is_array($data)) {
        api_fail(400, 'รูปแบบ JSON ไม่ถูกต้อง');
    }
    return $data;
}

function api_field($body, $key) {
    return isset($body[$key]) && !is_array($body[$key]) ? (string)$body[$key] : '';
}

function api_sale($r) {
    return array(
        'id' => $r['id'],
        'saleDate' => $r['saleDate'],
        'kg' => $r['kg'],
        'unitPrice' => $r['unitPrice'],
        'amount' => $r['amount'],
        'seasonCode' => $r['seasonCode'],
        'created_at' => $r['createdAt'],
        'updated_at' => $r['updatedAt'],
    );
}

/** ?season=<code> → Season, or the newest season when omitted */
function api_season_param() {
    $seasons = season_repo()->all();
    $code = isset($_GET['season']) ? (string)$_GET['season'] : '';
    if ($code === '') {
        return empty($seasons) ? null : $seasons[0];
    }
    $season = SeasonRepository::findByCode($seasons, $code);
    if ($season === null) {
        api_fail(404, 'ไม่พบรอบการขาย ' . $code);
    }
    return $season;
}

// ---------- Auth ----------
$token = env('API_TOKEN');
if (strlen($token) < 16) {
    api_fail(503, 'API ยังไม่เปิดใช้งาน — ตั้งค่า API_TOKEN (อย่างน้อย 16 ตัวอักษร) ในไฟล์ .env');
}
$sent = api_token_sent();
if ($sent === '' || !hash_equals($token, $sent)) {
    header('WWW-Authenticate: Bearer');
    api_fail(401, 'token ไม่ถูกต้อง');
}

// ---------- Routing ----------
$method = isset($_SERVER['REQUEST_METHOD']) ? strtoupper($_SERVER['REQUEST_METHOD']) : 'GET';
if ($method === 'POST' && !empty($_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE'])) {
    $method = strtoupper((string)$_SERVER['HTTP_X_HTTP_METHOD_OVERRIDE']);
}
if ($method === 'PUT') {
    $method = 'PATCH';
}
$route = isset($_GET['r']) ? (string)$_GET['r'] : '';
$id = isset($_GET['id']) ? (int)$_GET['id'] : 0;

try {
    if ($route === 'seasons') {
        if ($method === 'GET') {
            $out = array();
            foreach (season_repo()->all() as $s) {
                $out[] = $s->toApi();
            }
            api_send(200, array('records' => $out));
        }
        if ($method === 'POST' || $method === 'PATCH') {
            $b = api_body();
            $v = validate_season(api_field($b, 'saleYear'), api_field($b, 'startDate'), api_field($b, 'endDate'));
            if ($v['error'] !== null) {
                api_fail(400, $v['error']);
            }
            if ($method === 'POST') {
                $newId = season_repo()->create($v['saleYear'], $v['startDate'], $v['endDate']);
                api_send(201, array('record' => season_repo()->find($newId)->toApi()));
            }
            if ($id <= 0 || season_repo()->find($id) === null) {
                api_fail(404, 'ไม่พบรอบการขายที่ต้องการแก้ไข');
            }
            season_repo()->update($id, $v['saleYear'], $v['startDate'], $v['endDate']);
            api_send(200, array('record' => season_repo()->find($id)->toApi()));
        }
        if ($method === 'DELETE') {
            if ($id <= 0 || !season_repo()->delete($id)) {
                api_fail(404, 'ไม่พบรอบการขายที่ต้องการลบ');
            }
            api_send(200, array('ok' => true));
        }
    } elseif ($route === 'sales') {
        if ($method === 'GET') {
            $records = isset($_GET['season']) ? sales_repo()->forSeason(api_season_param()) : sales_repo()->all();
            api_send(200, array('records' => array_map('api_sale', $records)));
        }
        if ($method === 'POST' || $method === 'PATCH') {
            $b = api_body();
            $v = validate_sale(api_field($b, 'saleDate'), api_field($b, 'kg'), api_field($b, 'unitPrice'));
            if ($v['error'] !== null) {
                api_fail(400, $v['error']);
            }
            if ($method === 'POST') {
                $newId = sales_repo()->create($v['saleDate'], $v['kg'], $v['unitPrice']);
                api_send(201, array('record' => api_sale(sales_repo()->find($newId))));
            }
            if ($id <= 0 || sales_repo()->find($id) === null) {
                api_fail(404, 'ไม่พบรายการที่ต้องการแก้ไข');
            }
            sales_repo()->update($id, $v['saleDate'], $v['kg'], $v['unitPrice']);
            api_send(200, array('record' => api_sale(sales_repo()->find($id))));
        }
        if ($method === 'DELETE') {
            if ($id <= 0 || !sales_repo()->delete($id)) {
                api_fail(404, 'ไม่พบรายการที่ต้องการลบ');
            }
            api_send(200, array('ok' => true));
        }
    } elseif ($route === 'dashboard') {
        if ($method === 'GET') {
            $season = api_season_param();
            if ($season === null) {
                api_fail(404, 'ยังไม่มีรอบการขาย');
            }
            $sales = sales_repo()->forSeason($season);
            api_send(200, array(
                'season' => $season->toApi(),
                'statistics' => SalesRepository::statistics($sales),
                'monthly' => SalesRepository::monthlyPoints($season, $sales),
            ));
        }
    } else {
        api_fail(404, 'ไม่รู้จัก route — ใช้ ?r=seasons, ?r=sales หรือ ?r=dashboard');
    }
    api_fail(405, 'ไม่รองรับ method ' . $method . ' สำหรับ ' . $route);
} catch (AppException $e) {
    api_fail(409, $e->getMessage());
} catch (Exception $e) {
    error_log('TimSales API: ' . $e->getMessage());
    api_fail(500, 'เกิดข้อผิดพลาดภายในเซิร์ฟเวอร์');
}
