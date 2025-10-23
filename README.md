# ReceiptApp (Tama Ideas Rent)
Aplikasi Android sederhana (Jetpack Compose) untuk membuat receipt perjalanan: simpan PDF ke Downloads, share WA/Email, nomor invoice otomatis, jatuh tempo otomatis.

## Cara build (GitHub Actions dari HP)
- Upload repo ini ke GitHub (struktur sudah siap).
- Tab Actions -> Android CI -> jalankan (auto jalan saat push).
- Unduh artifact `app-debug.apk` -> install.

## Sesuaikan
- Ganti logo: `app/src/main/res/drawable/ic_tama_logo.png`
- (Opsional) QRIS: `app/src/main/res/drawable/ic_qris.png`
- Tarif & konfigurasi: lihat konstanta di `MainActivity.kt` (TARIF_PER_KM, BASE_TARIF, DEFAULT_WA_NUMBER, DEFAULT_BRANCH_CODE).
