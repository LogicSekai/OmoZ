@echo off
setlocal
rem Runs the JVM unit tests (engine-core: sound synthesis and simulator; app unit tests).
call "%~dp0_env.bat" || exit /b 1
call "%~dp0_gradle.bat" :engine-core:test :app:testDebugUnitTest || (
  echo [OmoZ] Ada tes yang GAGAL. Laporan: engine-core\build\reports\tests\
  exit /b 1
)
echo [OmoZ] Semua tes lolos.
exit /b 0
