@echo off
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%\..\.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%SCRIPT_DIR%config.properties"

REM Read config
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^release_version="') do set "RELEASE_VERSION=%%b"
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^debug_version="') do set "DEBUG_VERSION=%%b"
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^use_debug="') do set "USE_DEBUG=%%b"

set "RELEASE_VERSION=%RELEASE_VERSION: =%"
set "DEBUG_VERSION=%DEBUG_VERSION: =%"
set "USE_DEBUG=%USE_DEBUG: =%"

if /i "%USE_DEBUG%"=="true" (
    set "VERSION=%DEBUG_VERSION%"
    set "BUILD_TYPE=DEBUG"
    set "OUTPUT_BASE=debug"
) else (
    set "VERSION=%RELEASE_VERSION%"
    set "BUILD_TYPE=RELEASE"
    set "OUTPUT_BASE=release"
)

set "BUILD_DIR=%OUTPUT_BASE%\V%VERSION%"
set "JAR_NAME=dlqq_ai_tools.jar"

echo [INFO] Build mode: %BUILD_TYPE%
echo [INFO] Version: %VERSION%
echo [INFO] Output dir: %BUILD_DIR%
echo.

REM ========== Maven Build Java Project ==========
echo [INFO] ========================================
echo [INFO] Building Java project...
echo [INFO] ========================================
echo.

call mvn clean package -DskipTests -Drevision=%VERSION%

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b 1
)

echo [INFO] Maven build success!
echo.

REM ========== Copy to output directory ==========
if not exist "%BUILD_DIR%" (
    echo [INFO] Creating output dir: %BUILD_DIR%
    mkdir "%BUILD_DIR%"
)

REM Copy JAR
echo [INFO] Copying JAR...
copy /Y "target\%JAR_NAME%" "%BUILD_DIR%\dlqq_ai_tools.jar" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] JAR copy failed!
    exit /b 1
)
echo [INFO] JAR: %BUILD_DIR%\dlqq_ai_tools.jar

echo.
echo [INFO] ========================================
echo [INFO] Build completed!
echo [INFO] ========================================
echo [INFO] Output dir: %BUILD_DIR%
echo.

endlocal
