# OmoZ — Suara mesin untuk motor listrik

[![Android CI](https://github.com/LogicSekai/OmoZ/actions/workflows/android.yml/badge.svg)](https://github.com/LogicSekai/OmoZ/actions/workflows/android.yml)
[![Rilis](https://img.shields.io/github/v/release/LogicSekai/OmoZ?label=rilis)](https://github.com/LogicSekai/OmoZ/releases/latest)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)
[![Lisensi: AGPL-3.0 + komersial](https://img.shields.io/badge/lisensi-AGPL--3.0%20%2B%20komersial-orange)](COMMERCIAL-LICENSE.md)
[![Sponsor](https://img.shields.io/badge/sponsor-GitHub%20Sponsors-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/LogicSekai)

Bagian dari **Omo Ecosystem** oleh [Logic Sekai](https://logicsekai.com/omo/omoz).
> OmoZ adalah proyek independen dan **tidak berafiliasi** dengan OMOWAY maupun Google.

> 🇬🇧 English below.

Motor listrik hampir tanpa suara, sehingga pengendara lain sering kaget saat tiba-tiba ada motor di sampingnya. **OmoZ** memutar suara mesin sintetis yang mengikuti kecepatan GPS-mu, tetap berjalan di latar belakang (bahkan saat aplikasi dikeluarkan), dan bisa menyala otomatis saat HP tersambung ke perangkat Bluetooth tertentu (speaker motor, intercom helm, dll).

## Fitur

| | |
|---|---|
| 🔊 **7 suara mesin** | **Mesin Jet** (turbofan, wajib), **Mesin Jet Nging** (desing turbin saja), **Mesin Jet Ngung** (dengung turbin saja), **Dinamo Kereta Cepat** (motor traksi listrik), V-Twin Cruiser, Inline-4 Sport, 2-Tak Racing — semua disintesis real-time, tanpa file rekaman |
| 🛰️ **RPM dari GPS** | Kecepatan → putaran mesin + girboks virtual (jet: spool halus tanpa gigi) |
| 🎚️ **Tombol gas** | Tekan & tahan tombol bulat untuk menggeber mesin; tombol "Geber" juga ada di notifikasi |
| 📶 **Nyala otomatis Bluetooth** | Daftarkan perangkat → mesin nyala saat tersambung, mati saat terputus |
| 🔋 **Halaman pengaturan** | Latar belakang, optimasi baterai, autostart per merek, wakelock, frekuensi GPS, kualitas audio, mati otomatis saat parkir |
| 🔄 **Pembaruan otomatis** | Notifikasi saat ada versi baru di GitHub Releases, unduh dan pasang langsung dari aplikasi (checksum diperiksa) |
| ❤️ **Halaman Dukungan** | GitHub Sponsors, Ko-fi, Trakteer, dan jalan pintas untuk berkontribusi |
| 📱 **Semua merek** | Tanpa Google Play Services (jalan di Huawei/Honor), layar autostart khusus Xiaomi/Redmi/POCO, OPPO/realme, vivo/iQOO, Samsung, Huawei/Honor, OnePlus, ASUS, Infinix/Tecno/itel, Nokia, Meizu |

Android 8.0 (API 26) ke atas.

## Unduh

Ambil APK terbaru di **[Releases](https://github.com/LogicSekai/OmoZ/releases/latest)** atau di
[logicsekai.com/omo/omoz](https://logicsekai.com/omo/omoz). Setelah terpasang, OmoZ memberi tahu bila ada versi baru
(menu **Pembaruan aplikasi**).

## Build & pasang ke HP (Windows)

**Kebutuhan:** JDK 17+ (JDK bawaan Android Studio juga bisa) dan Android SDK (Android Studio atau command-line tools). Internet untuk unduhan pertama Gradle.

1. Di HP: *Pengaturan → Tentang ponsel →* ketuk **Nomor build** 7× → *Opsi pengembang →* aktifkan **USB debugging**.
2. Sambungkan HP via USB, setujui dialog *Izinkan USB debugging*.
3. Klik dua kali **`omoz.bat`** lalu pilih menu, atau jalankan skrip di folder `scripts\` langsung.

```bat
omoz.bat                                  :: menu build (semua pilihan di bawah)
scripts\build-debug.bat                   :: APK debug → dist\OmoZ-<versi>-debug.apk
scripts\build-release.bat                 :: APK release → dist\OmoZ-<versi>-release.apk
scripts\install-usb.bat [debug|release]   :: build + pasang lewat USB + buka aplikasi
scripts\install-wifi.bat [ip:port]        :: build + pasang lewat ADB Wi-Fi (bisa pairing Android 11+)
scripts\install-all.bat [watch]           :: pasang ke SEMUA HP tersambung; "watch" = pantau HP baru
scripts\test.bat                          :: unit test
scripts\render-previews.bat               :: render semua suara mesin ke WAV
scripts\signing-setup.bat                 :: buat kunci rilis (sekali saja)
scripts\bundle-release.bat                :: AAB untuk Google Play
scripts\clean.bat                         :: bersihkan build
```

Skrip otomatis mencari JDK 17–24 (JBR Android Studio, Temurin, dll.) dan Android SDK lalu membuat `local.properties`.
Linux/macOS: `scripts/build.sh [release]`, `scripts/install.sh [ip:port]`.

Manual dengan Gradle wrapper:

```bash
./gradlew :app:assembleDebug        # APK di app/build/outputs/apk/debug/
./gradlew :app:installDebug         # pasang ke HP yang tersambung adb
./gradlew :engine-core:test         # unit test mesin suara
./gradlew :engine-core:renderPreviews   # render semua suara ke WAV (engine-core/build/previews)
```

## Setelan pertama yang disarankan

Buka **Pengaturan** di aplikasi lalu aktifkan semua yang berstatus *Perlu*:
**Baterai tanpa batasan**, **Autostart** (khusus merek), **Lokasi sepanjang waktu** (untuk nyala otomatis Bluetooth), dan **Notifikasi**. Tanpa ini beberapa ROM (MIUI/HyperOS, ColorOS, FuntouchOS, dll.) akan mematikan suara di latar belakang. Panduan lengkap: [docs/OEM_BACKGROUND.md](docs/OEM_BACKGROUND.md).

## Struktur proyek

```
OmoZ/
├── engine-core/          Modul Kotlin murni (tanpa Android): sintesis suara + simulator RPM
│   └── src/main/kotlin/com/logicsekai/omoz/engine/
│       ├── EngineSound.kt        kontrak/interface suara mesin
│       ├── EngineCatalog.kt      daftar suara  ← tambahkan suara baru di sini
│       ├── EngineSimulator.kt    kecepatan + gas → RPM, beban, gigi
│       ├── dsp/                  filter, noise, oscillator
│       ├── synth/                JetTurbineSynth, PistonEngineSynth (konfigurasi silinder dll.)
│       └── tools/                PreviewRenderer (render WAV di komputer)
├── app/                  Aplikasi Android (Jetpack Compose)
│   └── src/main/java/com/logicsekai/omoz/app/
│       ├── service/              EngineService (foreground service) + notifikasi
│       ├── engine/               EngineAudioPlayer (AudioTrack), EngineRuntime (state)
│       ├── location/             SpeedTracker (GPS tanpa Play Services)
│       ├── bluetooth/            receiver nyala otomatis + helper
│       ├── system/               izin, optimasi baterai, layar autostart per merek
│       ├── data/                 SettingsRepository
│       └── ui/                   tema, komponen, layar (Beranda, Suara, Bluetooth, Pengaturan, Tentang)
├── docs/                 Arsitektur, panduan menambah suara, desain, panduan OEM
├── scripts/              Skrip build, pasang, tes, dan signing (dipanggil dari omoz.bat)
└── .github/              CI (build + test), template issue & PR
```

## Dukung OmoZ

OmoZ gratis dan open source. Dukunganmu membiayai pengembangan dan pengujian di lebih banyak HP:

- ❤️ [GitHub Sponsors](https://github.com/sponsors/LogicSekai) (sekaligus lisensi komersial lewat tier **Komersial**)
- ☕ [Ko-fi](https://ko-fi.com/logicsekai)
- 🇮🇩 [Trakteer](https://trakteer.id/kwaii)

Atau bantu lewat kode, suara mesin baru, terjemahan, dan laporan bug.

## Lisensi

OmoZ memakai **lisensi ganda**:

1. **[GNU AGPL-3.0-or-later](LICENSE)** dengan [Ketentuan Tambahan](NOTICE): boleh dipakai, diubah, dan
   dijual, asalkan kode sumber turunannya ikut dibuka dengan lisensi yang sama, kredit
   *"Based on OmoZ by Logic Sekai"* ditampilkan, dan versi ubahan memakai nama sendiri.
2. **[Lisensi komersial](COMMERCIAL-LICENSE.md)** untuk penggunaan yang tidak sesuai AGPL (mis. kode tertutup),
   diberikan kepada sponsor aktif tier **Komersial** di [GitHub Sponsors](https://github.com/sponsors/LogicSekai).

Kredit pihak ketiga: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). Kontribusi memerlukan persetujuan [CLA](CLA.md).

Baca [CONTRIBUTING.md](CONTRIBUTING.md) untuk mulai berkontribusi dan [docs/ADDING_ENGINE_SOUND.md](docs/ADDING_ENGINE_SOUND.md) untuk membuat suara mesin baru.

---

## English

**OmoZ** makes silent electric motorbikes audible. It synthesizes an engine sound in real time whose RPM follows your GPS speed, keeps running in the background (even after the app is swiped away), and can switch on automatically when the phone connects to a registered Bluetooth device.

- 7 engines: **Jet Turbine** (required), **Jet Whine** (whine only), **Jet Hum** (low hum only), **Bullet Train Motor** (electric traction), V-Twin Cruiser, Inline-4 Sport, 2-Stroke Racer — procedural audio, no samples.
- Hold-to-throttle button + "Rev" notification action.
- Settings for background running, battery optimization, OEM autostart screens, wake lock, GPS rate, audio quality and auto-stop.
- No Google Play Services dependency; works on every brand (Android 8.0+).

Build: install JDK 17+ and the Android SDK, then run `omoz.bat` (Windows menu, scripts in `scripts\`) or `scripts/build.sh` / `scripts/install.sh`. See [CONTRIBUTING.md](CONTRIBUTING.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Updates: OmoZ checks GitHub Releases, notifies you about new versions and installs them in-app.
Support the project via [GitHub Sponsors](https://github.com/sponsors/LogicSekai), [Ko-fi](https://ko-fi.com/logicsekai) or [Trakteer](https://trakteer.id/kwaii).

## License

Dual-licensed: [GNU AGPL-3.0-or-later](LICENSE) with the [additional terms in NOTICE](NOTICE)
(attribution "Based on OmoZ by Logic Sekai", modified versions use their own name), **or** a
[commercial license](COMMERCIAL-LICENSE.md) for active sponsors at the **Komersial** tier of GitHub Sponsors.
Third-party credits: [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md). Poppins font: SIL OFL 1.1 (`docs/POPPINS-OFL.txt`).
OmoZ is an independent project and is not affiliated with OMOWAY or Google.

> ⚠️ Ride safely: keep the volume reasonable and follow local regulations about vehicle sound. OmoZ is an awareness aid, not a replacement for your horn or attentive riding.
