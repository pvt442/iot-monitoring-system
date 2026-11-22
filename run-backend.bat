@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - BACKEND
mvn spring-boot:run
pause
