# Kebijakan Keamanan

## Versi yang didukung

| Versi | Didukung |
|---|---|
| 1.0.x | ✅ |
| < 1.0 | ❌ |

## Melaporkan celah keamanan

**Jangan** laporkan celah keamanan lewat issue publik. Gunakan salah satu jalur privat berikut:

1. **GitHub › tab Security › Report a vulnerability** (private vulnerability reporting), atau
2. email ke **logicsekai@gmail.com** dengan subjek `[SECURITY] OmoZ`.

Sertakan versi OmoZ, model HP dan versi Android, langkah reproduksi, serta dampaknya.
Kami akan membalas dalam **7 hari**, memberi kabar perkembangan, dan mencantumkan nama Anda di catatan
rilis (bila Anda mau) setelah perbaikan dirilis.

## Lingkup

Termasuk lingkup laporan, misalnya:
- receiver Bluetooth yang diekspor (`bluetooth/BluetoothAutoReceiver.kt`) dan intent yang bisa memicu
  mesin dari aplikasi lain;
- foreground service (`service/EngineService.kt`) dan aksi notifikasinya;
- penggunaan lokasi (GPS) dan izin latar belakang;
- mekanisme pembaruan (`update/`): unduhan APK, pemeriksaan checksum, nama paket, dan versi.

Di luar lingkup: celah pada Android, firmware HP, perangkat Bluetooth, atau firmware OMOWAY. Laporkan ke
pemiliknya masing-masing.
