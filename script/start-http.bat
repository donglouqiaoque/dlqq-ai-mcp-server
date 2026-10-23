@echo off
setlocal EnableExtensions
chcp 65001 >nul

rem Deployment layout: this bat, dlqq_ai_mcp_server.jar and jre\ in the same folder.
set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%dlqq_ai_mcp_server.jar"
set "JAVA=%APP_DIR%jre\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"

if not exist "%JAR%" (
    echo [ERROR] dlqq_ai_mcp_server.jar not found:
    echo         %JAR%
    echo [ERROR] Keep start-http.bat, dlqq_ai_mcp_server.jar and jre\ in the same folder.
    pause
    exit /b 1
)

:ask_workspace
set "WORKSPACE="
echo.
echo ============================================================
echo  Workspace
echo  Enter an existing absolute directory for the MCP workspace.
echo  Press ENTER to use the current directory:
echo    %CD%
echo ============================================================
set /p "WORKSPACE=Workspace path: "
if not defined WORKSPACE goto use_cwd
set "WORKSPACE=%WORKSPACE:"=%"
if not defined WORKSPACE goto use_cwd
if not exist "%WORKSPACE%\" (
    echo [ERROR] Directory does not exist: %WORKSPACE%
    echo.
    goto ask_workspace
)
set "WS_ARG=--workspace "%WORKSPACE%""
goto workspace_ready

:use_cwd
set "WORKSPACE=%CD%"
set "WS_ARG="
echo Using current directory as workspace.

:workspace_ready
echo.
echo ============================================================
echo  dlqq-ai-mcp-server  -  HTTP mode
echo  Endpoint  : http://localhost:49129/mcp
echo  Workspace : %WORKSPACE%
echo  JAR       : %JAR%
echo  Java      : %JAVA%
echo ============================================================
echo.

"%JAVA%" -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -jar "%JAR%" --transport http --port 49129 --path /mcp %WS_ARG%

set "RC=%ERRORLEVEL%"
echo.
echo Server stopped. Exit code: %RC%
pause
endlocal
