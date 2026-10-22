@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM dlqq-ai-mcp-server - build only (no copy)
REM Script lives in <project root>\script\ , project root = parent dir
REM Output: target\dlqq_ai_mcp_server.jar
REM RULE: this file must stay ASCII-only. Do NOT add CJK characters
REM       to a .bat file (cmd codepage would garble them).
REM ============================================================

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%SCRIPT_DIR%config.properties"
set "JAR_NAME=dlqq_ai_mcp_server.jar"

REM ---------- read version config ----------
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^release_version="') do set "RELEASE_VERSION=%%b"
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^debug_version="') do set "DEBUG_VERSION=%%b"
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^use_debug="') do set "USE_DEBUG=%%b"

set "RELEASE_VERSION=%RELEASE_VERSION: =%"
set "DEBUG_VERSION=%DEBUG_VERSION: =%"
set "USE_DEBUG=%USE_DEBUG: =%"

if /i "%USE_DEBUG%"=="true" (
    set "VERSION=%DEBUG_VERSION%"
    set "BUILD_TYPE=DEBUG"
) else (
    set "VERSION=%RELEASE_VERSION%"
    set "BUILD_TYPE=RELEASE"
)

echo [INFO] Build mode: %BUILD_TYPE%
echo [INFO] Version: %VERSION%
echo.

REM ========== maven build ==========
echo [INFO] ========================================
echo [INFO] Building Java project...
echo [INFO] ========================================
echo.

call mvn clean package -DskipTests -Drevision=%VERSION%

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    exit /b 1
)

echo.
echo [INFO] Build success!
echo [INFO] JAR: target\%JAR_NAME%
echo.

endlocal
