# Agar OmoZ tidak dimatikan di latar belakang / Keeping OmoZ alive

Android standar menjaga *foreground service* tetap hidup, tetapi banyak ROM pabrikan menambahkan "pembunuh aplikasi" sendiri. Buka **OmoZ → Pengaturan** dan pastikan semua berstatus *Aktif*, lalu ikuti langkah khusus merek di bawah.

| Merek | Langkah |
|---|---|
| **Xiaomi / Redmi / POCO** (MIUI/HyperOS) | Pengaturan OmoZ → *Autostart* → aktifkan. *Penghemat baterai* → **Tanpa batasan**. Di Terbaru, tahan kartu OmoZ → **Kunci** 🔒. |
| **OPPO / realme / OnePlus** (ColorOS) | *Autostart* → aktifkan. Baterai → *Izinkan aktivitas latar belakang* & *Izinkan mulai otomatis*. Kunci di Terbaru. |
| **vivo / iQOO** (FuntouchOS/OriginOS) | *Autostart / Mulai latar belakang* → aktifkan. Baterai → *Konsumsi daya latar belakang tinggi* → izinkan. |
| **Samsung** (One UI) | Baterai → *Tidak dibatasi*. Pastikan OmoZ tidak ada di *Aplikasi tidur / tidur dalam*. |
| **Huawei / Honor** (EMUI/MagicOS) | *Peluncuran aplikasi* → matikan *Kelola otomatis*, aktifkan ketiga sakelar manual. |
| **Infinix / Tecno / itel** (XOS/HiOS) | Phone Master → *Autostart* → aktifkan. Baterai → OmoZ → *Tanpa batasan*. |
| **ASUS** | Mobile Manager → *Auto-start manager* → aktifkan. |

Lebih lengkap: <https://dontkillmyapp.com> (tombol *Panduan untuk ponselmu* di aplikasi langsung membuka halaman merekmu).

## Mengapa izin "Lokasi sepanjang waktu"?

Saat mesin dinyalakan **otomatis oleh Bluetooth**, aplikasi sedang di latar belakang. Android 10+ hanya memberi akses GPS ke service seperti ini bila izin lokasi diset ke *Izinkan sepanjang waktu*. Tanpanya suara tetap menyala tetapi RPM tidak mengikuti kecepatan (hanya tombol gas).

## Mengapa "Baterai tanpa batasan"?

Android 12+ melarang aplikasi memulai foreground service dari latar belakang, **kecuali** aplikasi dikecualikan dari optimasi baterai. Tanpa pengecualian ini, saat Bluetooth tersambung OmoZ hanya bisa menampilkan notifikasi "Ketuk untuk menyalakan".
