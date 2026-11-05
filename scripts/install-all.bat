@echo off
setlocal EnableExtensions
rem Build OmoZ, then install and launch it on EVERY device in "adb devices" (USB and Wi-Fi).
rem Usage: install-all.bat [debug^|release] [watch] [ip:port]
rem   watch    keep running: every phone that connects later is installed too (Ctrl+C to stop)
rem   ip:port  "adb connect" to that device first
set "VARIANT=debug"
set "WATCH="
set "TARGET="
:parse
if "%~1"=="" goto :parsed
if /i "%~1"=="debug" (set "VARIANT=debug") else if /i "%~1"=="release" (set "VARIANT=release") else if /i "%~1"=="watch" (set "WATCH=1") else set "TARGET=%~1"
shift
goto :parse
:parsed

call "%~dp0_env.bat" || exit /b 1
if not defined ADB (
  echo [OmoZ] adb tidak ditemukan. Install Android SDK Platform-Tools.
  exit /b 1
)
call "%~dp0_variant.bat" %VARIANT% || exit /b 1
set "DONE=%TEMP%\omoz_done.txt"
set "SERIALS=%TEMP%\omoz_serials.txt"
type nul >"%DONE%"

call "%~dp0_gradle.bat" %OMO_TASK% || (
  echo [OmoZ] Build GAGAL.
  exit /b 1
)

if defined TARGET "%ADB%" connect %TARGET%
"%ADB%" start-server >nul 2>&1

set "WAITMSG="
:waitdev
call :scan
if not "%DEVCOUNT%"=="0" goto :have_devices
if not defined WAITMSG echo [OmoZ] Menunggu HP... sambungkan USB atau aktifkan Wireless debugging. Ctrl+C untuk batal.
set "WAITMSG=1"
timeout /t 3 /nobreak >nul
goto :waitdev

:have_devices
echo [OmoZ] %DEVCOUNT% perangkat terdeteksi.
set "FAILS=0"
for /f "usebackq delims=" %%s in ("%SERIALS%") do call :install_one "%%s"
if defined WATCH goto :watch
echo.
if "%FAILS%"=="0" (echo [OmoZ] Selesai. OmoZ terpasang dan dibuka di semua perangkat.) else (echo [OmoZ] Selesai dengan %FAILS% kegagalan.)
exit /b 0

:watch
echo [OmoZ] Mode pantau aktif: HP yang baru tersambung langsung dipasangi. Ctrl+C untuk berhenti.
:watch_loop
timeout /t 3 /nobreak >nul
call :scan
for /f "usebackq delims=" %%s in ("%SERIALS%") do call :install_one "%%s"
goto :watch_loop

rem ----------------------------------------------------------------------------
:scan
rem Serials can contain spaces (e.g. "adb-XXXX (2)._adb-tls-connect._tcp"), so split on TAB.
set "DEVCOUNT=0"
type nul >"%SERIALS%"
"%ADB%" devices >"%TEMP%\omoz_dev.txt" 2>&1
for /f "usebackq skip=1 tokens=1,2 delims=	" %%a in ("%TEMP%\omoz_dev.txt") do call :add_serial "%%a" "%%b"
exit /b 0

:add_serial
rem (no parenthesised blocks: serials may contain brackets)
if "%~2"=="device" goto :add_serial_ok
if not "%~2"=="unauthorized" exit /b 0
findstr /x /c:"UNAUTH %~1" "%DONE%" >nul && exit /b 0
echo [OmoZ] %~1 belum diizinkan: setujui dialog "Izinkan USB/Wireless debugging" di HP.
>>"%DONE%" echo(UNAUTH %~1
exit /b 0
:add_serial_ok
>>"%SERIALS%" echo(%~1
set /a DEVCOUNT+=1
exit /b 0

:install_one
set "SER=%~1"
findstr /x /c:"%SER%" "%DONE%" >nul && exit /b 0
set "MODEL="
"%ADB%" -s "%SER%" shell getprop ro.product.model >"%TEMP%\omoz_model.txt" 2>nul
set /p MODEL=<"%TEMP%\omoz_model.txt"
echo [OmoZ] Memasang ke %MODEL% [%SER%] ...
"%ADB%" -s "%SER%" install -r "%OMO_APK%" >"%TEMP%\omoz_inst.txt" 2>&1
findstr /c:"Success" "%TEMP%\omoz_inst.txt" >nul && goto :installed
findstr /c:"not enough space" /c:"INSUFFICIENT_STORAGE" "%TEMP%\omoz_inst.txt" >nul && goto :no_space
findstr /c:"INSTALL_FAILED_UPDATE_INCOMPATIBLE" /c:"INSTALL_FAILED_VERSION_DOWNGRADE" "%TEMP%\omoz_inst.txt" >nul
if errorlevel 1 goto :install_failed
echo [OmoZ]   Tanda tangan atau versi berbeda dengan yang terpasang: uninstall lalu pasang ulang (data aplikasi terhapus)...
"%ADB%" -s "%SER%" uninstall %OMO_PKG% >nul 2>&1
"%ADB%" -s "%SER%" install "%OMO_APK%" >"%TEMP%\omoz_inst.txt" 2>&1
findstr /c:"Success" "%TEMP%\omoz_inst.txt" >nul && goto :installed
goto :install_failed
:no_space
echo [OmoZ]   Memori %MODEL% penuh. Kosongkan penyimpanan HP (sisakan minimal 200-300 MB) lalu coba lagi.
set /a FAILS+=1
>>"%DONE%" echo(%SER%
exit /b 0
:install_failed
type "%TEMP%\omoz_inst.txt"
echo [OmoZ]   GAGAL memasang ke %MODEL%. Di Xiaomi/POCO aktifkan juga "Instal via USB" di Opsi pengembang.
set /a FAILS+=1
>>"%DONE%" echo(%SER%
exit /b 0
:installed
"%ADB%" -s "%SER%" shell am start -n %OMO_PKG%/com.logicsekai.omoz.app.ui.MainActivity >nul 2>&1
echo [OmoZ]   OK - OmoZ terpasang dan dibuka di %MODEL%.
>>"%DONE%" echo(%SER%
exit /b 0
