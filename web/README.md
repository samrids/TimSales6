# TimSales Web (PHP 5.6 + SQLite)

Web version of the TimSales6 app. It runs on PHP 5.6 shared hosting and keeps all data in **one SQLite file** — no Grist, no monthly API-call limit, no database server.

- No Composer or framework: plain PHP + Chart.js (bundled locally)
- One login password. Secrets live only in `.env` on the server
- `api.php` serves the same data as JSON so the Android app can switch from Grist later

## Files

| Path | What it does |
|---|---|
| `index.php` | Dashboard: season picker, total revenue, 3 stat tiles with trend lines, one monthly chart (revenue / volume / average price, table view, fullscreen), latest sales |
| `sales.php` | Sales in a season, newest first, grouped by month with subtotals. Warns about sales whose date is in no season |
| `sale_form.php` | Add or edit a sale (live total, shows which season the date counts toward). Delete is on the edit page |
| `seasons.php`, `season_form.php` | Seasons with start/end dates, sales count and revenue per season |
| `data.php`, `export.php` | CSV import (sales or seasons), CSV export, SQLite backup download |
| `api.php` | JSON API for the Android app (token auth) |
| `login.php`, `logout.php` | Log in and out. A login lasts 30 days from the last visit (logout is on **ข้อมูล** on phones) |
| `app/` | Server-side PHP (database, repositories, validation, views). Blocked by `app/.htaccess` |
| `data/` | The SQLite file `timsales.sqlite` (created on first use) and `sessions/` (login sessions). Blocked by `data/.htaccess` |
| `assets/` | CSS, JS, Chart.js, home-screen icons |
| `manifest.webmanifest` | Lets phones add TimSales to the home screen and open it full screen |
| `.env` | Settings and secrets. Blocked by `.htaccess`, never uploaded by `deploy.sh` |

## Data model

Two tables, created automatically on first request (`app/Database.php`):

- `seasons` — `sale_year` (2026 = รอบ 2026/27), `start_date`, `end_date`, timestamps
- `sales` — `sale_date`, `kg`, `unit_price`, timestamps

`amount` (= kg × unitPrice) and a sale's season are **derived, not stored**. A sale belongs to the season whose date range contains its date; where ranges overlap, the season that starts first wins. This reproduces the old Grist `seasonCode` formula exactly — checked against all 72 rows of the Grist export.

## Run locally (XAMPP PHP 8.2)

```bash
cp web/.env.example web/.env      # set APP_PASSWORD_HASH (see below)
/c/xampp/php/php.exe -S localhost:8080 -t web
```
Open http://localhost:8080/login.php

Local XAMPP runs PHP 8.2, the server runs 5.6. Don't use `??`, `<=>`, type hints, return types, `random_bytes()`, or SQLite features newer than 3.8 (UPSERT, window functions, `RETURNING`).

## Set the password (first time)

1. Leave `APP_PASSWORD_HASH=` empty in `.env`
2. Open `login.php`. It shows a form for generating the hash
3. Enter the password you want (at least 8 characters), then copy the `APP_PASSWORD_HASH=...` line into `.env`
4. Reload and log in

To log out every phone at once (lost phone), set a new password: changing `APP_PASSWORD_HASH` ends all existing logins.

## Add to the home screen

Needs HTTPS. Android Chrome: menu ⋮ → **Add to Home screen** / **Install app**. iPhone Safari: Share → **Add to Home Screen**. It opens full screen with the leaf icon; the login lasts 30 days, so it doesn't ask for the password each time.

## Move the data from Grist (once)

1. In Grist, export both tables as CSV (or use `docs/myProject-TimSales.csv` and `docs/myProject-TimYear.csv`)
2. Log in → **ข้อมูล** → import the **seasons** CSV first, then the **sales** CSV
3. Check the dashboard totals against Grist / the Android app

Re-importing is safe: rows already present are skipped.

## Deploy to the shared host (FTP)

```bash
cd /c/Users/samrid.so/AndroidStudioProjects/TimSales6
./deploy.sh --dry-run     # show what changed
./deploy.sh --deploy      # upload (PHP lint + PHP 5.6 check first)
```

`deploy.sh` never uploads `.env` or any `*.sqlite` file, so a deploy can't overwrite the live database. After the first deploy:

1. Upload `web/.env` to `APP_DIR/.env` yourself (FileZilla). Remove the old `GRIST_*` lines; add `API_TOKEN` when the Android app is ready to use the API
2. Make `APP_DIR/data/` writable by PHP (`chmod 775`, or `777` if the host needs it) — SQLite also writes a journal file next to the database
3. Open the site, log in, and import the CSVs (above) — or upload a local `data/timsales.sqlite` with FileZilla instead

Then check:
- `https://<site>/<APP_DIR>/.env` returns 403
- `https://<site>/<APP_DIR>/data/timsales.sqlite` returns 403 (if not, the host ignores `.htaccess`: set `DB_PATH` in `.env` to a folder outside `public_html`)
- `https://<site>/<APP_DIR>/app/env.php` returns 403

**Backups:** **ข้อมูล → ไฟล์ฐานข้อมูล SQLite** downloads the whole database as one file. Do it regularly — the server file is the only copy.

## JSON API (for the Android app)

Set `API_TOKEN` in `.env` (16+ random characters; empty = API off). Send it as `Authorization: Bearer <token>` or `X-Api-Key: <token>`.

| Method | URL | Body | Result |
|---|---|---|---|
| GET | `api.php?r=seasons` | | `{records:[…]}` |
| POST | `api.php?r=seasons` | `{saleYear, startDate?, endDate?}` | `201 {record}` |
| PATCH | `api.php?r=seasons&id=N` | same | `{record}` |
| DELETE | `api.php?r=seasons&id=N` | | `{ok:true}` |
| GET | `api.php?r=sales[&season=2026/27]` | | `{records:[…]}` |
| POST | `api.php?r=sales` | `{saleDate, kg, unitPrice}` | `201 {record}` |
| PATCH | `api.php?r=sales&id=N` | same | `{record}` |
| DELETE | `api.php?r=sales&id=N` | | `{ok:true}` |
| GET | `api.php?r=dashboard[&season=2026/27]` | | `{season, statistics, monthly}` |

Field names match the Android models (`saleDate`, `unitPrice`, `seasonCode`, `created_at`, …). Errors come back as `{error: "<Thai message>"}` with a 400/401/404/409 status. If the host blocks PATCH/DELETE, send POST with `X-HTTP-Method-Override: PATCH` (or `DELETE`).

## Troubleshooting

| Symptom | Fix |
|---|---|
| "เซิร์ฟเวอร์ไม่มีส่วนขยาย pdo_sqlite" ("The server lacks the pdo_sqlite extension") | Enable `pdo_sqlite` in the host's PHP settings |
| "โฟลเดอร์ฐานข้อมูลเขียนไม่ได้" ("The database folder isn't writable") | `chmod 775 data/` (or 777), or point `DB_PATH` at a writable folder |
| API returns 401 with the right token | The host strips `Authorization`; send `X-Api-Key` instead |
| Blank white page | Check the host's PHP error log. It is usually PHP 7+ syntax |
