@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM dlqq-ai-mcp-server - build a portable Python runtime
REM
REM Put this file into the Python installation root (the folder
REM that contains python.exe) and run it. It creates "python_env"
REM next to it: a trimmed, self-contained runtime with tkinter,
REM pip and bleak, ready to ship next to dlqq_ai_mcp_server.jar.
REM
REM It never runs an installer and never modifies the source
REM installation. It only copies files and calls pip.
REM
REM Usage:
REM   make_python_env.bat [source_dir] [output_dir] [/force] [/nopip] [?]
REM
REM Options:
REM   /force   rebuild even if python_env already exists
REM   /nopip   skip installing bleak
REM   ?        show this help
REM
REM Output:
REM   <python_dir>\python_env
REM
REM Notes:
REM   - Lib\site-packages of the source is NOT copied. It is
REM     rebuilt with ensurepip + "pip install bleak", so large
REM     third-party packages (torch, nvidia, bpy, ...) never
REM     end up in the result.
REM   - Debug symbols, debug binaries and test suites are skipped.
REM   - Offline wheels: put *.whl into <python_dir>\wheels and
REM     they will be used instead of downloading.
REM   - The window stays open at the end (pause) so output and
REM     warnings can be copied.
REM
REM RULE: this file must stay ASCII-only.
REM ============================================================

set "SRCARG="
set "OUTARG="
set "FORCE=0"
set "SKIP_PIP=0"

:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="/?" ( call :print_usage & exit /b 0 )
if /i "%~1"=="?"  ( call :print_usage & exit /b 0 )
if /i "%~1"=="/force" ( set "FORCE=1" & shift & goto parse_args )
if /i "%~1"=="/nopip" ( set "SKIP_PIP=1" & shift & goto parse_args )
if "%SRCARG%"=="" ( set "SRCARG=%~1" & shift & goto parse_args )
if "%OUTARG%"=="" ( set "OUTARG=%~1" & shift & goto parse_args )
echo [ERROR] Unknown argument: %~1
call :print_usage
goto fail

:args_done
if "%SRCARG%"=="" ( set "PYDIR=%~dp0" ) else ( set "PYDIR=%SRCARG%" )
if not "%PYDIR:~-1%"=="\" set "PYDIR=%PYDIR%\"
if "%OUTARG%"=="" ( set "OUT=%PYDIR%python_env" ) else ( set "OUT=%OUTARG%")
set "WHEELS=%PYDIR%wheels"
for %%D in ("%OUT%") do set "OUTPARENT=%%~dpD"
set "BUILDTMP=%OUTPARENT%.python_env_tmp"
if not exist "%PYDIR%python.exe" (
    echo [ERROR] python.exe not found in: %PYDIR%
    echo         Put this script into the Python installation root.
    goto fail
)
if not exist "%PYDIR%Lib\tkinter\__init__.py" (
    echo [ERROR] tkinter not found in: %PYDIR%
    echo         A full CPython installation with Tcl/Tk is required.
    goto fail
)

echo [INFO] Source: %PYDIR%
echo [INFO] Output: %OUT%

REM ---------- output directory ----------
if exist "%OUT%\python.exe" (
    if "%FORCE%"=="0" (
        echo [INFO] python_env already exists.
        echo [INFO] Use /force to rebuild.
        goto done_ok
    )
)
if exist "%OUT%" (
    echo [INFO] Removing old output...
    rmdir /s /q "%OUT%"
    if errorlevel 1 (
        echo [ERROR] Cannot remove: %OUT%
        goto fail
    )
)

REM ---------- copy ----------
echo.
echo [INFO] ========================================
echo [INFO] Copying Python runtime...
echo [INFO] ========================================
robocopy "%PYDIR%." "%OUT%" /E /R:1 /W:1 /NFL /NDL /NJH /NJS ^
    /XD "%PYDIR%python_env" "%PYDIR%Lib\site-packages" "%PYDIR%Lib\test" "%PYDIR%Lib\idlelib" "%PYDIR%Lib\turtledemo" "__pycache__" ^
    /XF *.pdb *_d.pyd *_d.dll python_d.exe pythonw_d.exe make_python_env.bat
if errorlevel 8 (
    echo [ERROR] robocopy failed with code %ERRORLEVEL%
    goto fail
)

if not exist "%OUT%\python.exe" (
    echo [ERROR] python.exe was not copied.
    goto fail
)

REM ---------- verify standard library ----------
echo.
echo [INFO] Verifying standard library...
"%OUT%\python.exe" -c "import tkinter, ssl, sqlite3, asyncio, json, socket, threading" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Standard library check failed.
    echo         tkinter needs DLLs\_tkinter.pyd and the tcl\ folder.
    goto fail
)

REM ---------- rebuild pip and install bleak ----------
if "%SKIP_PIP%"=="0" (
    echo.
    echo [INFO] Rebuilding pip ^(ensurepip^)...
    if not exist "%BUILDTMP%" mkdir "%BUILDTMP%"
    set "TMP=%BUILDTMP%"
    set "TEMP=%BUILDTMP%"
    "%OUT%\python.exe" -m ensurepip --upgrade
    if errorlevel 1 (
        echo [ERROR] ensurepip failed.
        goto fail
    )

    echo [INFO] Installing bleak...
    if exist "%WHEELS%" (
        echo [INFO] Offline wheels found: %WHEELS%
        "%OUT%\python.exe" -m pip install --no-index --find-links "%WHEELS%" bleak
    ) else (
        "%OUT%\python.exe" -m pip install --no-warn-script-location --disable-pip-version-check bleak
    )
    if errorlevel 1 (
        echo [ERROR] Failed to install bleak.
        echo         Online:  "%OUT%\python.exe" -m pip install bleak
        echo         Offline: put wheels into %WHEELS% and rerun
        goto fail
    )

    "%OUT%\python.exe" -c "import bleak" >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] bleak import failed.
        goto fail
    )
)

REM ---------- version and size ----------
for /f "delims=" %%V in ('"%OUT%\python.exe" --version 2^>^&1') do set "PYVER=%%V"
> "%OUT%\VERSION.txt" echo %PYVER%
>> "%OUT%\VERSION.txt" echo built %DATE% %TIME%

for /f %%S in ('powershell -NoProfile -Command "[math]::Round((Get-ChildItem -LiteralPath '%OUT%' -Recurse -File | Measure-Object Length -Sum).Sum/1MB,1)"') do set "SIZE_MB=%%S"

if exist "%BUILDTMP%" rmdir /s /q "%BUILDTMP%" 2>nul

echo.
echo [INFO] ========================================
echo [INFO] Portable Python runtime ready
echo [INFO] ========================================
echo [INFO] Path:    %OUT%
echo [INFO] Version: %PYVER%
echo [INFO] Size:    %SIZE_MB% MB
echo [INFO] Copy this folder next to dlqq_ai_mcp_server.jar
echo [INFO] and jre\ as "python_env\".
goto done_ok

REM ============================================================
REM Exit points (window stays open so output can be copied)
REM ============================================================

:done_ok
echo.
echo Press any key to close this window...
pause >nul
endlocal
exit /b 0

:fail
echo.
echo [ERROR] Build failed. Press any key to close this window...
pause >nul
endlocal
exit /b 1

REM ============================================================
REM Subroutines
REM ============================================================

:print_usage
echo Usage: make_python_env.bat [source_dir] [output_dir] [/force] [/nopip] [?]
echo.
echo   source_dir   Python installation root (default: folder of this script)
echo   output_dir   target folder (default: ^<source_dir^>\python_env)
echo   /force       rebuild even if output_dir already has python.exe
echo   /nopip       skip installing bleak
echo.
echo Default usage: put this script into the Python installation root
echo and run it without arguments.
echo.
echo The source Lib\site-packages is not copied; pip and bleak are
echo reinstalled, so unrelated packages never enter the result.
exit /b 0
