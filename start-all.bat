@echo off
chcp 65001 >nul
set "ROOT=%~dp0"
title PULSE IoT - JAVA SWING SYSTEM

echo ========================================================
echo   PULSE IoT - HE THONG GIAM SAT (JAVA SWING DESKTOP)
echo ========================================================
echo Hay khoi dong MySQL truoc khi tiep tuc.
echo.

echo [1/3] Dang khoi dong Backend Server (Spring Boot 8080)...
start "Pulse IoT Backend" /D "%ROOT%" cmd /k "call "%ROOT%run-backend.bat""

echo Dang doi Backend khoi dong 12 giay...
timeout /t 12 /nobreak >nul

echo [2/3] Dang khoi dong Client Simulator...
start "Pulse IoT Simulator" /D "%ROOT%" cmd /k "call "%ROOT%run-simulator.bat""

echo [3/3] Dang khoi dong Giao dien Desktop (Java Swing)...
start "Pulse IoT Desktop" /D "%ROOT%" cmd /k "call "%ROOT%run-desktop.bat""

echo.
echo ========================================================
echo   DA KHOI DONG THANH CONG!
echo ========================================================
pause
