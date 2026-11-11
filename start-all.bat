@echo off
chcp 65001 >nul
title IoT Monitoring System - Launcher
cd /d "%~dp0"

echo ========================================================
echo   KHOI DONG HE THONG GIAM SAT IOT (FULL STACK)
echo ========================================================
echo.

REM 1. Kiem tra va cai dat thu vien Frontend neu chua co
if not exist "frontend\node_modules" (
    echo [1/4] Phat hien chua co node_modules. Dang cai dat thu vien Frontend...
    cd frontend && call npm install && cd ..
) else (
    echo [1/4] Thu vien Frontend da san sang.
)

REM 2. Khoi dong Backend Spring Boot
echo [2/4] Dang khoi dong Backend Server (port 8080)...
start "IoT Backend (Spring Boot)" cmd /k "cd /d "%~dp0backend" && .\mvnw.cmd spring-boot:run"

REM Cho 8 giay de Backend khoi dong truoc
echo Dang doi Backend khoi dong...
timeout /t 8 /nobreak >nul

REM 3. Khoi dong Web Frontend
echo [3/4] Dang khoi dong Web Dashboard (port 5173)...
start "IoT Frontend (React Vite)" cmd /k "cd /d "%~dp0frontend" && npm run dev"

REM 4. Khoi dong IoT Client Simulator
echo [4/4] Dang khoi dong IoT Client Simulator...
start "IoT Client Simulator" cmd /k "cd /d "%~dp0iot-client" && .\run.bat"

REM Tu dong mo trinh duyet vao Dashboard
echo.
echo ========================================================
echo   HE THONG DA KHOI DONG THANH CONG!
echo   Dang mo Dashboard tai http://localhost:5173...
echo ========================================================
timeout /t 3 /nobreak >nul
start http://localhost:5173
