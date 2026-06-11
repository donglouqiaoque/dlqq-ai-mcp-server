@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%\..\.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%SCRIPT_DIR%config.properties"

REM 读取配置
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
set "JAR_NAME=aitools-%VERSION%.jar"

echo [INFO] Build mode: %BUILD_TYPE%
echo [INFO] Version: %VERSION%
echo [INFO] Output dir: %BUILD_DIR%
echo.

REM ========== Maven 构建 Java 项目 ==========
echo [INFO] ========================================
echo [INFO] 编译 Java 项目...
echo [INFO] ========================================
echo.

call mvn clean package -DskipTests -Drevision=%VERSION%

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven 构建失败！
    exit /b 1
)

echo [INFO] Maven 构建成功！
echo.

REM ========== 复制到输出目录 ==========
if not exist "%BUILD_DIR%" (
    echo [INFO] 创建输出目录: %BUILD_DIR%
    mkdir "%BUILD_DIR%"
)

REM 复制 JAR
echo [INFO] 复制 JAR...
copy /Y "target\%JAR_NAME%" "%BUILD_DIR%\donglouqiaoque_ai_tools.jar" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] JAR 复制失败！
    exit /b 1
)
echo [INFO] JAR: %BUILD_DIR%\donglouqiaoque_ai_tools.jar

echo.
echo [INFO] ========================================
echo [INFO] 构建完成！
echo [INFO] ========================================
echo [INFO] 输出目录: %BUILD_DIR%
echo.

endlocal
