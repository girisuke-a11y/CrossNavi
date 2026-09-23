@echo off
title CrossNavi Emulator Launcher
cd /d "%~dp0"

echo ========================================================
echo  CrossNavi Android Emulator Launcher
echo ========================================================
echo.

set ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
set EMULATOR=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe

echo [1/3] Starting Android Emulator (medium_phone)...
start "" "%EMULATOR%" -avd medium_phone

echo [2/3] Waiting for emulator to start...
"%ADB%" wait-for-device

:check_boot
for /f "delims=" %%i in ('"%ADB%" shell getprop sys.boot_completed 2^>nul') do set BOOT_STATE=%%i
if "%BOOT_STATE%"=="1" goto boot_done
timeout /t 2 /nobreak >nul
goto check_boot

:boot_done
echo [3/3] Launching CrossNavi App...
"%ADB%" shell am start -n com.example.crossnavi/.MainActivity >nul 2>&1

echo.
echo ========================================================
echo  Ready! You can now use the app in the emulator window.
echo ========================================================
pause
