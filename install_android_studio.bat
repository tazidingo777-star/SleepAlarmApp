@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul 2>&1
title Android Studio Installer

:: =============================================
::  Android Studio Auto Download & Install
::  For Windows 10/11 64-bit
:: =============================================

:: ---- Config ----
set "DL_URL=https://redirector.gvt1.com/edgedl/android/studio/install/2024.2.2.14/android-studio-2024.2.2.14-windows.exe"
set "DL_DIR=%TEMP%\android-studio-install"
set "INSTALLER=%DL_DIR%\as-installer.exe"
set "INSTALL_DIR=C:\Android\AndroidStudio"

:: ---- Check Admin ----
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Requesting administrator privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo =============================================
echo   Android Studio Auto Installer
echo =============================================
echo.

:: ---- Step 1: Prep ----
echo [1/5] Preparing download directory...
if not exist "%DL_DIR%" mkdir "%DL_DIR%"
cd /d "%DL_DIR%"

:: ---- Step 2: Download ----
echo [2/5] Downloading Android Studio (~1GB, please wait)...
echo   URL: %DL_URL%
echo   Save: %INSTALLER%
echo.

:: Use PowerShell for download with progress
set "PS_CMD=$url='!DL_URL!'; $out='!INSTALLER!'; if ((Test-Path $out) -and ((Get-Item $out).Length -gt 104857600)) { Write-Host '[INFO] Installer already exists, skipping download' -ForegroundColor Yellow; exit 0 } else { Write-Host '[INFO] Downloading...' -ForegroundColor Green; $ProgressPreference='Continue'; Invoke-WebRequest -Uri $url -OutFile $out -UseBasicParsing; Write-Host '[INFO] Download complete!' -ForegroundColor Green }"
powershell -ExecutionPolicy Bypass -Command "%PS_CMD%"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Download failed! Check your network connection.
    echo You can manually download from: https://developer.android.com/studio
    pause
    exit /b 1
)

:: ---- Step 3: Install ----
echo.
echo [3/5] Installing Android Studio (silent mode)...
echo   Install path: %INSTALL_DIR%
echo.

:: NSIS silent install: /S for silent, /D= for directory (must be LAST parameter)
"%INSTALLER%" /S /D=%INSTALL_DIR%

if %errorlevel% equ 0 (
    echo [OK] Android Studio installed successfully!
) else (
    echo [WARN] Silent install may have failed. Launching interactive installer...
    echo Please follow the on-screen wizard to complete installation.
    start "" "%INSTALLER%"
)

:: ---- Step 4: Desktop Shortcut ----
echo.
echo [4/5] Creating desktop shortcut...
set "PS_SHORTCUT=$ws=New-Object -ComObject WScript.Shell; $sc=$ws.CreateShortcut([Environment]::GetFolderPath('Desktop')+'\Android Studio.lnk'); $sc.TargetPath='!INSTALL_DIR!\bin\studio64.exe'; $sc.WorkingDirectory='!INSTALL_DIR!\bin'; $sc.IconLocation='!INSTALL_DIR!\bin\studio.ico'; $sc.Save(); Write-Host '[OK] Desktop shortcut created'"
powershell -ExecutionPolicy Bypass -Command "%PS_SHORTCUT%"

:: ---- Step 5: Cleanup ----
echo.
echo [5/5] Cleanup...
echo.
echo Delete the installer file (~1GB)?
set /p "ANSWER=Enter Y to delete, any other key to keep: "
if /i "!ANSWER!"=="Y" (
    del /f /q "%INSTALLER%" >nul 2>&1
    echo [OK] Installer deleted.
) else (
    echo [INFO] Installer kept at: %INSTALLER%
)

:: ---- Done ----
echo.
echo =============================================
echo   Installation Complete!
echo.
echo   Install path: %INSTALL_DIR%
echo   Desktop shortcut: Android Studio
echo.
echo   After first launch:
echo     1. Select "Do not import settings"
echo     2. Follow the Setup Wizard to download SDK
echo     3. Open the SleepAlarm project folder
echo =============================================
echo.
pause
endlocal
