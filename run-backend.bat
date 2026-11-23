@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - BACKEND
if exist ".\mvnw.cmd" (
    call .\mvnw.cmd spring-boot:run
) else (
    call mvn spring-boot:run
)
pause
