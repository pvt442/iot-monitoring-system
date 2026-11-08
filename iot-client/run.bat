@echo off

REM IoT Client Simulator - biên dịch và chạy
REM Yêu cầu JDK 21, không cần Maven
REM Ví dụ:
REM run.bat --server=http://localhost:8080 --interval=3000 --devices=DEV_01,DEV_02

chcp 65001 >nul
cd /d "%~dp0"

if not exist "out" mkdir "out"

if exist "sources.txt" del /f /q "sources.txt"

echo.
echo ========================================
echo Dang bien dich IoT Client...
echo ========================================
echo.

for /R "src\main\java" %%F in (*.java) do echo %%F>>sources.txt

javac -encoding UTF-8 -d "out" @sources.txt

if errorlevel 1 (
    echo.
    echo ========================================
    echo Bien dich that bai.
    echo ========================================
    pause
    exit /b 1
)

del /f /q "sources.txt"

echo.
echo ========================================
echo Bien dich thanh cong.
echo Dang khoi dong IoT Client...
echo ========================================
echo.

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "out" com.demo.client.ClientApp %*