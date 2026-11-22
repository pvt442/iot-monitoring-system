@echo off
chcp 65001 >nul
echo Dong cac cua so PULSE IoT...
taskkill /FI "WINDOWTITLE eq Pulse IoT Backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Pulse IoT Simulator*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Pulse IoT Desktop*" /T /F >nul 2>&1
echo Da gui lenh dong. Neu chay trong NetBeans, bam Stop trong IDE.
pause
