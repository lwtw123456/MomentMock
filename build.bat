@echo off
cd /d "%~dp0"
call gradlew.bat :app:buildApk
echo.
echo ------------------------------------------------------------
echo APK: %~dp0moment-mock.apk
echo ------------------------------------------------------------
pause
