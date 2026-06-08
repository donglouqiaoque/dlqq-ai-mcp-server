@echo off

set "PROJECT_DIR=%~dp0..\.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%~dp0config.properties"

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^version="') do (
    set "VERSION=%%b"
)

set "VERSION=%VERSION: =%"
echo [INFO] Version: %VERSION%

set "JAR_NAME=aitools-%VERSION%.jar"

echo [INFO] Start Maven build...
call mvn clean package -DskipTests

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed!
    exit /b 1
)

echo [INFO] Build success!
echo [INFO] JAR: target\%JAR_NAME%
