# Kredit & pemberitahuan pihak ketiga

OmoZ ditulis oleh Logic Sekai di atas pustaka open source berikut. Setiap komponen tetap berada di
bawah lisensinya sendiri; lisensi AGPL OmoZ **tidak** mengubah lisensi mereka.

*Third-party credits and notices. Each component below keeps its own license.*

## Dibundel di dalam aplikasi

| Komponen | Pembuat | Lisensi | Dipakai untuk |
|---|---|---|---|
| [Kotlin standard library](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. dan kontributor Kotlin | Apache-2.0 | Bahasa & runtime |
| [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines) | JetBrains s.r.o. dan kontributor | Apache-2.0 | Tugas latar belakang, pembaruan aplikasi |
| [AndroidX Core KTX, Activity, Lifecycle](https://developer.android.com/jetpack/androidx) | The Android Open Source Project | Apache-2.0 | Fondasi aplikasi Android |
| [Jetpack Compose](https://developer.android.com/jetpack/compose) (UI, Foundation, Material 3, Material Icons Core) | The Android Open Source Project | Apache-2.0 | Seluruh tampilan aplikasi |
| [Navigation Compose](https://developer.android.com/jetpack/androidx/releases/navigation) | The Android Open Source Project | Apache-2.0 | Navigasi antar-layar |
| [AndroidX WorkManager](https://developer.android.com/jetpack/androidx/releases/work) | The Android Open Source Project | Apache-2.0 | Pemeriksaan pembaruan berkala |
| [Material Symbols](https://github.com/google/material-design-icons) | Google LLC | Apache-2.0 | Sebagian ikon (`app/src/main/res/drawable/`) |
| [Poppins](https://github.com/itfoundry/Poppins) (Regular, Medium, SemiBold, Bold) | The Poppins Project Authors (Indian Type Foundry) | SIL OFL 1.1 ([teks lisensi](docs/POPPINS-OFL.txt)) | Font aplikasi (`app/src/main/res/font/`) |

Semua suara mesin **disintesis** oleh kode OmoZ sendiri (`engine-core`); tidak ada rekaman suara pihak ketiga.

## Hanya untuk build & pengujian (tidak masuk APK)

| Komponen | Pembuat | Lisensi |
|---|---|---|
| [Gradle](https://gradle.org) & Gradle Wrapper | Gradle, Inc. | Apache-2.0 |
| [Android Gradle Plugin](https://developer.android.com/build) | Google LLC | Apache-2.0 |
| [Kotlin Gradle Plugin](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. | Apache-2.0 |
| [JUnit 4](https://github.com/junit-team/junit4) | JUnit team | EPL-1.0 |
| [kotlin-test](https://github.com/JetBrains/kotlin) | JetBrains s.r.o. | Apache-2.0 |
| GitHub Actions: [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [upload-artifact](https://github.com/actions/upload-artifact), [gradle/actions](https://github.com/gradle/actions) | GitHub, Inc. / Gradle, Inc. | MIT |

## Merek dagang

OMOWAY, Android, Google Play, Bluetooth, dan nama terkait adalah merek dagang milik pemiliknya masing-masing.
OmoZ adalah proyek independen dan **tidak berafiliasi** dengan, didukung, atau disponsori oleh mereka.

## Teks lisensi

- Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- SIL Open Font License 1.1: [`docs/POPPINS-OFL.txt`](docs/POPPINS-OFL.txt)
- GNU AGPL v3.0: [`LICENSE`](LICENSE)
- Eclipse Public License 1.0: <https://www.eclipse.org/legal/epl-v10.html>
- MIT License: <https://opensource.org/license/mit>

Menemukan kredit yang kurang atau salah? Buka issue atau kirim email ke logicsekai@gmail.com.
