# TOKOKUH — Professional Offline Point of Sale (POS) Android

Produk Resmi dari: [www.tokokuh.com](https://www.tokokuh.com)  
Developer: **Logis Denis Prabowo**  
Jabatan: **Prompt Developer**  
Lokasi: **Indramayu, Jawa Barat**  

---

## 1. Deskripsi Aplikasi
**TOKOKUH** adalah aplikasi kasir (Point of Sale) Android native profesional yang dirancang khusus untuk pelaku UMKM, toko ritel, minimarket, cafe, warung makan, dan aneka usaha dagang lainnya.

Aplikasi ini beroperasi **100% secara offline** tanpa ketergantungan pada koneksi internet, backend API, Firebase, Supabase, ataupun database server pihak ketiga. Seluruh data transaksi, katalog produk, riwayat shift, pengeluaran, dan audit log tersimpan dengan aman secara lokal pada penyimpanan internal perangkat pengguna.

---

## 2. Fitur Utama
1. **Dashboard & Analitik Harian:**
   - Omzet penjualan, total transaksi, estimasi laba kotor, dan laba bersih hari ini.
   - Peringatan dini stok produk menipis (*low stock alert*).
   - Indikator status shift aktif dan kasir bertugas.
   - Ringkasan transaksi terkini.

2. **Modul Kasir / POS (Point of Sale):**
   - Katalog produk cepat dengan filter kategori dan pencarian instan.
   - Pemindai barcode via Kamera bawaan (CameraX + ZXing offline) dan dukungan Barcode Scanner Bluetooth eksternal (HID keyboard).
   - Keranjang belanja interaktif (+/- kuantitas, diskon per item, diskon keseluruhan transaksi).
   - Perhitungan pajak otomatis (PPN/PB1, mendukung mode inklusif dan eksklusif).
   - Metode pembayaran Tunai (CASH) dengan kalkulator kembalian otomatis & tombol nominal cepat (Uang Pas, 10k, 50k, 100k).
   - Metode pembayaran Non-Tunai (QRIS & Transfer Bank) dengan pencatatan nomor referensi manual.
   - Fitur **Tahan Transaksi** (*Parked Sale*) dan **Lanjutkan Transaksi**.

3. **Manajemen Produk & Inventori:**
   - Tambah, edit, nonaktifkan, dan hapus produk.
   - Snapshot Harga Pokok Penjualan (HPP / Harga Modal) pada setiap transaksi agar laba masa lalu tetap akurat.
   - Pengaturan stok terbatas vs stok tak terbatas, satuan barang, dan ambang batas peringatan stok minimum.
   - Penyesuaian stok manual (+/-) dengan alasan audit otomatis.
   - Upload foto produk lokal menggunakan Android Photo Picker (zero-permission).

4. **Kategori Produk:**
   - Pengelompokan produk berdasarkan kategori. Menghapus kategori tidak menghapus produk (otomatis menjadi tanpa kategori).

5. **Riwayat Transaksi, Retur & Pembatalan:**
   - Detail riwayat transaksi lengkap dengan pencarian dan filter tanggal/status.
   - Cetak ulang struk via printer thermal Bluetooth.
   - **Retur Barang Sebagian atau Seluruhnya:** Menghitung nilai pengembalian, mencatat alasan retur, dan otomatis mengembalikan stok ke gudang.
   - **Pembatalan Transaksi:** Mengembalikan seluruh stok barang dan mencatat audit log pembatalan.

6. **Pencetakan Struk Thermal ESC/POS (58mm):**
   - Mendukung Printer Thermal Bluetooth Classic (SPP RFCOMM).
   - Format struk standar 58mm (32 karakter/baris).
   - **Logo Credit Resmi TOKOKUH** (`logo-credit-di-bawah-struk.png`) wajib dicetak di bagian paling bawah setiap struk transaksi.
   - Fitur uji cetak (*Test Print*) dan pencetakan ulang (*Reprint*).

7. **Buka & Tutup Shift Kasir:**
   - Buka shift: catat kasir dan modal awal laci (*cash float*).
   - Selama shift: catat kas masuk tambahan dan kas keluar (tarik uang).
   - Tutup shift: hitung kas seharusnya (`Modal Awal + Penjualan Tunai - Pengeluaran Tunai + Kas Masuk - Kas Keluar`), input uang fisik aktual, dan hitung selisih kas.

8. **Pencatatan Biaya Pengeluaran:**
   - Catat pengeluaran operasional (listrik, transportasi, kemasan, dll). Terhubung langsung dengan perhitungan Laba Bersih di laporan keuangan.

9. **Laporan Keuangan & Ekspor CSV:**
   - Filter periode: Hari Ini, Kemarin, Minggu Ini, Bulan Ini, dan Semua.
   - Rincian: Omzet Bersih, Total HPP, Laba Kotor, Pengeluaran, Laba Bersih, Penjualan Tunai vs Non-Tunai, dan Peringkat Produk Terlaris.
   - **Ekspor Laporan CSV** langsung ke penyimpanan perangkat via Android Storage Access Framework (SAF).

10. **Cadangan & Pemulihan Lengkap (JSON Backup & Restore):**
    - Ekspor seluruh tabel database, pengaturan, dan foto produk ke dalam satu file `.json`.
    - Pemulihan data terverifikasi yang dieksekusi secara aman di dalam *Room Database Transaction* dengan rollback otomatis jika terjadi kegagalan format.

11. **Pusat Bantuan & Customer Service:**
    - Tombol langsung terhubung ke WhatsApp resmi (+62 823-7947-4173).
    - Tombol kirim email ke `tokokuh.com@gmail.com`.

---

## 3. Teknologi yang Digunakan
- **Bahasa:** Kotlin 2.x
- **UI Framework:** Jetpack Compose (Material Design 3 - Dark Theme #292929, Accent #FEC107)
- **Database Lokal:** Android Room SQLite (dengan KSP compiler)
- **Asinkron & Reaktif:** Kotlin Coroutines & StateFlow
- **Arsitektur:** MVVM (Model-View-ViewModel) + Repository Pattern
- **Pencitraan & Gambar:** Coil Compose & ImageMagick
- **Pemindai Barcode:** CameraX + ZXing Core (100% On-Device Offline)
- **Pencetakan Thermal:** ESC/POS Command Generator + Android Bluetooth Classic Socket (RFCOMM)
- **Penyimpanan Dokumen:** Android Storage Access Framework (SAF)

---

## 4. Struktur Proyek
```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── TokokuhApp.kt                 # Inisialisasi DB & default seed data
│   ├── MainActivity.kt               # Entrypoint, edge-to-edge & scanner keyboard listener
│   ├── data/
│   │   ├── database/
│   │   │   ├── TokokuhDatabase.kt    # Room database builder
│   │   │   ├── entity/               # 12 Room database entities
│   │   │   └── dao/                  # 10 Data Access Objects
│   │   └── repository/
│   │       └── TokokuhRepository.kt  # Business logic & transactional operations
│   ├── ui/
│   │   ├── components/               # TokokuhCommon (top bar, card, empty state, dialogs)
│   │   ├── theme/                    # Color.kt (#292929, #FEC107), Theme.kt, Type.kt
│   │   ├── navigation/               # TokokuhAppRoot.kt & backstack handling
│   │   ├── dashboard/                # DashboardScreen.kt
│   │   ├── pos/                      # PosScreen, CartSheet, PaymentDialog, ReceiptPreviewDialog
│   │   ├── products/                 # ProductListScreen.kt
│   │   ├── transactions/             # TransactionHistoryScreen.kt
│   │   ├── reports/                  # ReportsScreen.kt
│   │   ├── shifts/                   # ShiftScreen.kt
│   │   ├── expenses/                 # ExpenseScreen.kt
│   │   ├── printer/                  # PrinterScreen.kt
│   │   ├── employees/                # EmployeeScreen.kt
│   │   ├── settings/                 # SettingsScreen.kt
│   │   ├── help/                     # HelpScreen.kt (WhatsApp & Email)
│   │   ├── about/                    # AboutScreen.kt (Official Header Logo & info)
│   │   └── scanner/                  # CameraScannerView.kt (Offline CameraX + ZXing)
│   ├── utils/
│   │   ├── CurrencyUtils.kt          # Format Rupiah (Rp10.000) & generator nomor struk
│   │   ├── EscPosPrinterHelper.kt    # Generator ESC/POS 58mm & dither logo credit
│   │   ├── NotificationHelper.kt     # Local notification manager
│   │   └── BackupRestoreManager.kt   # JSON Backup/Restore & CSV export
│   └── viewmodel/
│       └── TokokuhViewModel.kt       # ViewModel utama & StateFlow
└── res/
    ├── drawable/
    │   ├── logo_header_atas.png      # Logo resmi header aplikasi
    │   ├── logo_credit_struk.png     # Logo resmi wajib di bawah struk
    │   └── logo_icon_aplikasi.png    # Logo launcher & notifikasi
    └── values/
        └── strings.xml               # app_name = "TOKOKUH"
```

---

## 5. Cara Membuka Proyek & Build APK

### Melalui GitHub Codespaces:
Ikuti panduan toolchain lengkap di [`CODESPACES_BUILD.md`](CODESPACES_BUILD.md). Project memakai JDK 17, Gradle 9.3.1, Android SDK Platform 36, dan Build Tools 36.0.0.

Setelah Gradle Wrapper dibuat satu kali dan disimpan ke repository:

```bash
chmod +x gradlew
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```

APK debug tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions juga disiapkan pada `.github/workflows/android-build.yml` untuk build APK dan menjalankan unit test otomatis.

---

## 6. Jaminan 100% Offline
Aplikasi tidak memerlukan izin `android.permission.INTERNET` di `AndroidManifest.xml`. Seluruh transaksi, perhitungan diskon, pajak, cetak printer thermal, scanner barcode, pembuatan laporan, serta pencadangan database bekerja secara penuh saat perangkat berada dalam **Mode Pesawat (Airplane Mode)**.
