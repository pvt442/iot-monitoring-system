@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - JAVA SWING DESKTOP
mvn -Dexec.mainClass=com.pulseiot.desktop.PulseIoTApplication exec:java
if errorlevel 1 pause
