@echo off

set "PROJECT_DIR=%~dp0..\.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%~dp0config.properties"

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^version="') do (
    set "VERSION=%%b"
)

set "VERSION=%VERSION: =%"
set "RELEASE_DIR=release\V%VERSION%"
set "JAR_NAME=aitools-%VERSION%.jar"

echo [INFO] Version: %VERSION%
echo [INFO] Release dir: %RELEASE_DIR%

echo [INFO] Start Maven build...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed!
    exit /b 1
)

echo [INFO] Build success!

if not exist "%RELEASE_DIR%" (
    echo [INFO] Create release dir: %RELEASE_DIR%
    mkdir "%RELEASE_DIR%"
)

echo [INFO] Copy JAR to release dir...
copy /Y "target\%JAR_NAME%" "%RELEASE_DIR%\%JAR_NAME%"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Copy failed!
    exit /b 1
)

echo [INFO] Release success: %RELEASE_DIR%\%JAR_NAME%
