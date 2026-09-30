@echo off
setlocal DisableDelayedExpansion
title Foundations PL4 1.21.1 Development Build
set "PL_PROJECT="
echo Select the source folder containing build.gradle and gradlew.bat.
for /f "usebackq delims=" %%D in (`powershell -NoProfile -STA -Command "Add-Type -AssemblyName System.Windows.Forms; $picker = New-Object System.Windows.Forms.FolderBrowserDialog; $picker.Description = 'Select Foundations PL4 source folder'; if ($picker.ShowDialog() -eq 'OK') { [Console]::WriteLine($picker.SelectedPath) }"`) do set "PL_PROJECT=%%D"
if not defined PL_PROJECT set /p "PL_PROJECT=Full source path (blank cancels): "
if not defined PL_PROJECT exit /b 1
if not exist "%PL_PROJECT%\build.gradle" goto badfolder
if not exist "%PL_PROJECT%\gradlew.bat" goto badfolder
where java >nul 2>nul
if errorlevel 1 goto nojava
where javac >nul 2>nul
if errorlevel 1 goto nojava
java -version
echo.
echo Java 21 JDK is required. This is a DEVELOPMENT checkpoint, not stable parity.
echo 1. Build the JAR
echo 2. Build and run the server GameTests
set "PL_CHOICE=1"
set /p "PL_CHOICE=Choose 1 or 2 [1]: "
pushd "%PL_PROJECT%"
if "%PL_CHOICE%"=="2" (call gradlew.bat clean build runGameTestServer) else (call gradlew.bat clean build)
set "PL_RESULT=%ERRORLEVEL%"
popd
if not "%PL_RESULT%"=="0" (echo BUILD FAILED. Read the error above.) else (echo Build completed. JARs are in the selected project's build\libs folder.)
pause
exit /b %PL_RESULT%
:badfolder
echo This is not the source folder. Choose the folder containing build.gradle and gradlew.bat.
pause
exit /b 1
:nojava
echo Install a Java 21 JDK and ensure java and javac are available on PATH.
pause
exit /b 1
