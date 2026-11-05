@echo off
setlocal
rem Renders every engine sound to WAV files (engine-core\build\previews) to listen on the computer.
call "%~dp0_env.bat" || exit /b 1
call "%~dp0_gradle.bat" :engine-core:renderPreviews || (
  echo [OmoZ] Render GAGAL.
  exit /b 1
)
echo [OmoZ] Selesai: engine-core\build\previews\
if exist "%OMO_ROOT%\engine-core\build\previews" start "" "%OMO_ROOT%\engine-core\build\previews"
exit /b 0
