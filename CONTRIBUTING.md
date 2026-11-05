# Kontribusi ke OmoZ / Contributing

Terima kasih sudah mau ikut mengembangkan OmoZ! Proyek ini hidup dari kontribusi komunitas
**Logic Sekai**. Prinsipnya: **kalau kamu memperbaiki atau menambah sesuatu, kirim balik ke sini.**

*English speakers are welcome. Issues and pull requests may be written in Indonesian or English.*

## Sebelum mulai

1. Baca [Kode Etik](CODE_OF_CONDUCT.md).
2. Pahami lisensinya: **AGPL-3.0-or-later + Ketentuan Tambahan** ([`NOTICE`](NOTICE)) dan lisensi ganda
   dengan [lisensi komersial](COMMERCIAL-LICENSE.md).
3. Kontribusi pertama wajib menyetujui [CLA](CLA.md) (centang di template Pull Request) dan setiap
   commit wajib *sign-off* (`git commit -s`).
4. Untuk perubahan besar, **buka issue dulu** agar arahnya disepakati.
5. **Celah keamanan**: jangan lewat issue publik; ikuti [SECURITY.md](SECURITY.md).

## Mulai cepat / Quick start

1. Fork & clone, buka folder di **Android Studio** (Ladybug atau lebih baru) atau gunakan menu `omoz.bat` (Windows) / `scripts/build.sh` (Linux, macOS).
2. JDK 17+ dan Android SDK 35.
3. Jalankan test sebelum PR:
   ```bash
   ./gradlew :engine-core:test :app:assembleDebug
   ```
   Windows: `scripts\test.bat` dan `scripts\build-debug.bat`.

## Cara paling mudah untuk membantu

| Tugas | Di mana | Tingkat |
|---|---|---|
| 🔊 Tambah / setel suara mesin | `engine-core/.../EngineCatalog.kt` — [panduan](docs/ADDING_ENGINE_SOUND.md) | Mudah–menengah |
| 🌐 Terjemahan | `app/src/main/res/values-xx/strings.xml` (salin dari `values/strings.xml`) | Mudah |
| 📱 Layar autostart untuk merek HP-mu | `app/.../system/SystemSettings.kt` → `OEM_SCREENS` | Mudah |
| 🎨 UI / aksesibilitas | `app/.../ui/` — ikuti [docs/DESIGN.md](docs/DESIGN.md) | Menengah |
| ⚙️ Simulator (girboks, akselerasi) | `engine-core/.../EngineSimulator.kt` + test | Menengah |

## Aturan kode

- **Kotlin**, gaya resmi (`kotlin.code.style=official`), lihat `.editorconfig`. Komentar dan nama simbol dalam bahasa Inggris.
- **engine-core tidak boleh bergantung pada Android.** Semua DSP harus bisa di-test di JVM.
- **Tanpa alokasi di jalur audio** (`EngineSynth.render`) — tidak ada `listOf`, lambda, atau boxing di dalam loop sampel.
- **Tanpa Google Play Services** — aplikasi harus jalan di Huawei/Honor dan ROM tanpa GMS.
- Teks UI selalu lewat `strings.xml` (Inggris di `values/`, Indonesia di `values-in/`).
- ID suara mesin (`EngineInfo.id`) **tidak boleh diubah** setelah rilis karena disimpan di pengaturan pengguna.
- Dependency baru: versinya di `gradle/libs.versions.toml`, lisensinya kompatibel AGPL-3.0, dan
  kreditnya ditambahkan ke [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md) dan teks `about_licenses_body`.
- File Kotlin baru diawali header:

  ```kotlin
  // SPDX-License-Identifier: AGPL-3.0-or-later
  // Copyright (C) 2026 Logic Sekai and OmoZ contributors. See NOTICE.
  ```

- Satu PR = satu topik. Sertakan screenshot untuk perubahan UI dan file WAV (dari `renderPreviews`) untuk suara baru.

## Memakai kode orang lain

- Jangan menyalin kode atau rekaman suara dari proyek lain tanpa menyebut sumber dan lisensinya di PR.
- Kode hasil dekompilasi aplikasi pihak ketiga **tidak** diterima.
- Jangan commit kunci (`keystore/`, `keystore.properties`) atau APK.

## Commit

Gunakan pesan singkat bergaya [Conventional Commits](https://www.conventionalcommits.org/) dan selalu `git commit -s`:
`feat(sound): add Vespa 2-stroke`, `fix(bt): stop engine on disconnect`, `docs: ...`.

## Melaporkan bug

Gunakan template issue. Sertakan merek & model HP, versi Android, versi OmoZ, dan log bila bisa:

```bash
adb logcat -s OmoZService OmoZAudio OmoZBt OmoZUpdate
```

## Rilis (maintainer)

1. Naikkan `omo.versionCode` dan `omo.versionName` di `gradle.properties`, lalu catat di [`CHANGELOG.md`](CHANGELOG.md).
2. Pertama kali saja: `scripts\signing-setup.bat` untuk membuat kunci rilis. **Cadangkan** `keystore\` dan
   `keystore.properties`; tanpa keduanya pengguna tidak bisa memperbarui ke versi berikutnya.
3. `scripts\build-release.bat` → buat GitHub Release dengan tag `v<versionName>` dan lampirkan
   `dist\OmoZ-<versi>-release.apk`.
   Aplikasi memverifikasi SHA-256, nama paket, dan versionCode sebelum memasang pembaruan.
