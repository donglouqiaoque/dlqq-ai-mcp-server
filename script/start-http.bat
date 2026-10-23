@echo off
setlocal
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

echo ============================================================
echo  dlqq-ai-mcp-server  -  HTTP mode
echo  Endpoint : http://localhost:49129/mcp
echo  JAR      : %JAR%
echo  Java     : %JAVA%
echo ============================================================
echo.

"%JAVA%" -Dfile.encoding=UTF-8 -jar "%JAR%" --transport http --port 49129 --path /mcp

set "RC=%ERRORLEVEL%"
echo.
echo Server stopped. Exit code: %RC%
pause
endlocal
