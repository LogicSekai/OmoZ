@echo off
rem Installs %OMO_APK% and launches the app. Args: adb device selector (-d  or  -s ip:port)
set "SEL=%*"
echo [OmoZ] Memasang %OMO_PKG% ...
"%ADB%" %SEL% install -r "%OMO_APK%" > "%TEMP%\omoz_install.txt" 2>&1
type "%TEMP%\omoz_install.txt"
findstr /c:"Success" "%TEMP%\omoz_install.txt" >nul && goto :launch
findstr /c:"not enough space" /c:"INSUFFICIENT_STORAGE" "%TEMP%\omoz_install.txt" >nul && goto :no_space
findstr /c:"INSTALL_FAILED_UPDATE_INCOMPATIBLE" /c:"INSTALL_FAILED_VERSION_DOWNGRADE" /c:"signatures do not match" "%TEMP%\omoz_install.txt" >nul || goto :failed
echo.
echo [OmoZ] Versi di HP ditandatangani kunci lain. Uninstall dulu? (data aplikasi akan terhapus)
choice /c YN /n /m "Uninstall lalu pasang ulang [Y/N]? "
if errorlevel 2 goto :failed
"%ADB%" %SEL% uninstall %OMO_PKG%
"%ADB%" %SEL% install -r "%OMO_APK%" || goto :failed

:launch
"%ADB%" %SEL% shell am start -n %OMO_PKG%/com.logicsekai.omoz.app.ui.MainActivity >nul
echo [OmoZ] Terpasang dan dijalankan.
exit /b 0
:no_space
echo [OmoZ] Memori HP penuh. Kosongkan penyimpanan HP (sisakan minimal 200-300 MB) lalu coba lagi,
echo           atau pasang versi release yang jauh lebih kecil (omoz.bat menu 6).
exit /b 1
:failed
echo [OmoZ] Pemasangan GAGAL. Di Xiaomi/POCO aktifkan juga "Instal via USB" di Opsi pengembang.
exit /b 1
