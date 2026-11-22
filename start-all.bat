@echo off
chcp 65001 >nul
set "ROOT=%~dp0"
title PULSE IoT - JAVA SWING SYSTEM
where java >nul 2>&1 || (echo [LOI] Chua cai JDK 21.& pause & exit /b 1)
where mvn >nul 2>&1 || (echo [LOI] Chua cai Maven.& pause & exit /b 1)
echo Hay khoi dong MySQL truoc khi tiep tuc.
start "Pulse IoT Backend" /D "%ROOT%" cmd /k "mvn spring-boot:run"
echo Dang doi Backend khoi dong 15 giay...
timeout /t 15 /nobreak >nul
start "Pulse IoT Simulator" /D "%ROOT%" cmd /k "mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java"
start "Pulse IoT Desktop" /D "%ROOT%" cmd /k "mvn -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java"
echo Da mo ba tien trinh. Neu Backend chua san sang, doi them vai giay.
pause
