@echo off
call gradlew.bat --no-daemon build
exit /b %errorlevel%
