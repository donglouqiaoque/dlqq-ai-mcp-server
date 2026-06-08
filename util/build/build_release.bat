@echo off

set "PROJECT_DIR=%~dp0\..\.."
cd /d "%PROJECT_DIR%"

set "CONFIG_FILE=%~dp0config.properties"

REM 读取配置文件的各个字段
for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^release_version="') do (
    set "RELEASE_VERSION=%%b"
)

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^debug_version="') do (
    set "DEBUG_VERSION=%%b"
)

for /f "tokens=1,2 delims==" %%a in ('type "%CONFIG_FILE%" ^| findstr "^use_debug="') do (
    set "USE_DEBUG=%%b"
)

REM 去除空格
set "RELEASE_VERSION=%RELEASE_VERSION: =%"
set "DEBUG_VERSION=%DEBUG_VERSION: =%"
set "USE_DEBUG=%USE_DEBUG: =%"

REM 根据 use_debug 选择版本和输出目录
if /i "%USE_DEBUG%"=="true" (
    set "VERSION=%DEBUG_VERSION%"
    set "BUILD_TYPE=debug"
    set "OUTPUT_DIR=debug"
    echo [INFO] Build mode: DEBUG
) else (
    set "VERSION=%RELEASE_VERSION%"
    set "BUILD_TYPE=release"
    set "OUTPUT_DIR=release"
    echo [INFO] Build mode: RELEASE
)

set "BUILD_DIR=%OUTPUT_DIR%\V%VERSION%"
set "JAR_NAME=aitools-%VERSION%.jar"

echo [INFO] Version: %VERSION%
echo [INFO] Output dir: %BUILD_DIR%

echo [INFO] Start Maven build with revision=%VERSION%...
call mvn clean package -DskipTests -Drevision=%VERSION%

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Build failed!
    exit /b 1
)

echo [INFO] Build success!

if not exist "%BUILD_DIR%" (
    echo [INFO] Create output dir: %BUILD_DIR%
    mkdir "%BUILD_DIR%"
)

echo [INFO] Copy JAR to output dir...
copy /Y "target\%JAR_NAME%" "%BUILD_DIR%\%JAR_NAME%"

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Copy failed!
    exit /b 1
)

echo [INFO] %BUILD_TYPE% success: %BUILD_DIR%\%JAR_NAME%
