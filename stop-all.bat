@echo off
chcp 65001 >nul
title IoT Monitoring System - Stop All
echo ========================================================
echo   DANG TAT CAC TIEN TRINH CUA IOT PROJECT...
echo ========================================================
echo.

REM Tat tien trinh Java (Backend va IoT Client)
taskkill /F /IM java.exe 2>nul
if %errorlevel% equ 0 (
    echo [OK] Da dung Backend va IoT Client Simulator.
) else (
    echo [-] Khong co tien trinh Java dang chay.
)

REM Tat tien trinh Node (Frontend)
taskkill /F /IM node.exe 2>nul
if %errorlevel% equ 0 (
    echo [OK] Da dung Web Frontend.
) else (
    echo [-] Khong co tien trinh Node dang chay.
)

echo.
echo ========================================================
echo   DA TAT TOAN BO HE THONG THANH CONG!
echo ========================================================
timeout /t 3 >nul
