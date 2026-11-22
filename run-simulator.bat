@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - SIMULATOR
mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java
if errorlevel 1 pause
