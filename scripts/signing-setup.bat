@echo off
setlocal
rem Creates the release/upload keystore and keystore.properties (both ignored by git).
rem BACK UP keystore\ and keystore.properties somewhere safe: losing them means you can no
rem longer publish updates under the same signing key.
call "%~dp0_env.bat" || exit /b 1
set "KS_DIR=%OMO_ROOT%\keystore"
set "KS_FILE=%KS_DIR%\omoz-release.jks"
if exist "%OMO_ROOT%\keystore.properties" (
  echo [OmoZ] keystore.properties sudah ada - tidak ditimpa.
  exit /b 0
)
echo Buat kunci rilis OmoZ. Gunakan huruf dan angka saja untuk password (hindari tanda ^& %% ^^ ! ^< ^>).
set "ALIAS=omoz"
set /p ALIAS=Alias kunci [omoz]: 
set "OWNER=LogicSekai"
set /p OWNER=Nama/organisasi [LogicSekai]: 
set /p STOREPASS=Password (min. 6 karakter): 
if "%STOREPASS%"=="" (
  echo [OmoZ] Password kosong, dibatalkan.
  exit /b 1
)
if not exist "%KS_DIR%" mkdir "%KS_DIR%"
"%JAVA_HOME%\bin\keytool.exe" -genkeypair -v -storetype PKCS12 -keystore "%KS_FILE%" -alias "%ALIAS%" -keyalg RSA -keysize 4096 -validity 10000 -storepass "%STOREPASS%" -keypass "%STOREPASS%" -dname "CN=%OWNER%, O=%OWNER%, C=ID"
if errorlevel 1 (
  echo [OmoZ] keytool GAGAL.
  exit /b 1
)
> "%OMO_ROOT%\keystore.properties" (
  echo storeFile=keystore/omoz-release.jks
  echo storePassword=%STOREPASS%
  echo keyAlias=%ALIAS%
  echo keyPassword=%STOREPASS%
)
echo.
echo [OmoZ] Kunci dibuat: keystore\omoz-release.jks
echo           SIMPAN CADANGAN folder keystore\ dan file keystore.properties di tempat aman.
exit /b 0
