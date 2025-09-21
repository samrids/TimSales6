# Git Commands Summary

## ไฟล์ที่พร้อม Commit:
```bash
git add .env.example
git add README.md
git add .gitignore
git add app/build.gradle.kts
git add app/src/main/java/com/example/timsales/config/AppConfig.kt
```

## Commit Message:
```bash
git commit -m "🔒 Security: Move sensitive config to .env file

- Add .env support with BuildConfig integration
- Move API_KEY, DOC_ID, TABLE_NAMES to environment variables
- Add .env to .gitignore for security
- Create .env.example template for team setup
- Update README with security guidelines
- Refactor AppConfig to use BuildConfig values

BREAKING CHANGE: Requires .env file setup before build"
```

## ตรวจสอบก่อน Push:
```bash
# ตรวจสอบว่าไฟล์ .env ไม่ถูก track
git status

# ควรเห็น .env ใน untracked หรือไม่เห็นเลย
# หาก .env ปรากฏใน staged ให้ unstage:
# git reset HEAD .env
```

## สำหรับทีม:
ส่งข้อความนี้ไปให้ทีม:
"หลังจาก pull โค้ดใหม่ ต้องทำตามขั้นตอน:
1. cp .env.example .env
2. แก้ไขค่าใน .env ให้ตรงกับ Grist ของคุณ
3. Build project"