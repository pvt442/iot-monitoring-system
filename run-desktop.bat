@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - JAVA SWING DESKTOP
if exist ".\mvnw.cmd" (
    call .\mvnw.cmd -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java
) else (
    call mvn -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java
)
if errorlevel 1 pause
