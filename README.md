# جدول جادویی فارسی (Persian Crossword Adventure)
طراحی و سازنده: مرضیه برقی

بازی جدول کلمات متقاطع فارسی برای اندروید (Kotlin + Jetpack Compose، آفلاین).

## گرفتن APK
1. در GitHub یک مخزن جدید بساز (مثلاً `persian-crossword`).
2. همه‌ی فایل‌های این پوشه (از جمله پوشه‌ی `.github`) را در مخزن آپلود کن (Add file ← Upload files).
3. به تب **Actions** برو؛ ساخت «Build APK» خودکار اجرا می‌شود (حدود ۵ تا ۸ دقیقه).
4. پس از پایان، پایین صفحه‌ی همان اجرا، در بخش **Artifacts** فایل `PersianCrossword-APK` را دانلود کن؛ داخلش `PersianCrossword-release.apk` است.
   برای ساخت Release رسمی، یک Tag با نام `v1.0` بساز؛ APK در بخش Releases قرار می‌گیرد.

## افزودن سؤال
فایل `app/src/main/assets/questions.txt` را باز کن و خط جدید با این قالب اضافه کن:
`شناسه‌ی_یکتا|دسته|سطح(۱،۲،۳)|پاسخ_بدون_فاصله|متن_سؤال`
