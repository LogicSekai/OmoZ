## Ringkasan

<!-- Apa yang diubah dan kenapa? Tautkan issue terkait, mis. "Closes #12". -->

## Jenis perubahan

- [ ] Perbaikan bug
- [ ] Fitur baru
- [ ] Suara mesin baru / penyetelan suara
- [ ] Refactor / performa
- [ ] Dokumentasi / terjemahan
- [ ] Build / CI

## Cara menguji

<!-- Langkah uji, HP + versi Android/ROM. Lampirkan screenshot untuk perubahan UI dan WAV dari renderPreviews untuk suara. -->

## Checklist

- [ ] `./gradlew :engine-core:test :app:assembleDebug` lulus
- [ ] Teks UI lewat `strings.xml` (EN di `values/`, ID di `values-in/`), tidak di-hard-code
- [ ] File Kotlin baru memakai header `SPDX-License-Identifier: AGPL-3.0-or-later`
- [ ] Dependency/kode/suara pihak ketiga baru (bila ada) berlisensi kompatibel AGPL dan sudah dicatat di `THIRD-PARTY-NOTICES.md`
- [ ] `CHANGELOG.md` diperbarui bila perubahan terasa oleh pengguna
- [ ] Semua commit sudah *sign-off* (`git commit -s`)

## Perjanjian kontributor

- [ ] I have read the OmoZ CLA ([CLA.md](https://github.com/LogicSekai/OmoZ/blob/main/CLA.md)) and I agree to its terms for this and future contributions.
