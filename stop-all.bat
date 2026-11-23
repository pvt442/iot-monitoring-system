@echo off
chcp 65001 >nul
echo Dang dong cac cua so va tien trinh PULSE IoT...
taskkill /FI "WINDOWTITLE eq Pulse IoT Backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Pulse IoT Simulator*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Pulse IoT Desktop*" /T /F >nul 2>&1
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /PID %%a /F >nul 2>&1
)
echo Da dong xong cac tien trinh PULSE IoT.
pause
