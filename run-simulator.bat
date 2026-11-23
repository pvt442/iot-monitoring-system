@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo PULSE IoT - SIMULATOR
if exist ".\mvnw.cmd" (
    call .\mvnw.cmd -Dexec.mainClass=com.demo.client.ClientApp exec:java
) else (
    call mvn -Dexec.mainClass=com.demo.client.ClientApp exec:java
)
if errorlevel 1 pause
