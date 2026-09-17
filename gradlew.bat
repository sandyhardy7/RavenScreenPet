@echo off
setlocal
set "GRADLE_VERSION=8.9"
set "GRADLE_HOME=%USERPROFILE%\.gradle\manual-dists\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%TEMP%\gradle-%GRADLE_VERSION%-bin.zip"
if exist "%GRADLE_HOME%\bin\gradle.bat" goto run

echo First-time setup: downloading Gradle %GRADLE_VERSION%...
if not exist "%USERPROFILE%\.gradle\manual-dists" mkdir "%USERPROFILE%\.gradle\manual-dists"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
if errorlevel 1 goto fail
echo Extracting Gradle...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%GRADLE_ZIP%' -DestinationPath '%USERPROFILE%\.gradle\manual-dists' -Force"
if errorlevel 1 goto fail
del /q "%GRADLE_ZIP%" >nul 2>&1
:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
:fail
echo Failed to download or extract Gradle.
exit /b 1
