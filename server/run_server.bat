@echo off
title Hermes Agent Windows 11 Controller & Remote Gateway
color 0B
echo ======================================================================
echo           HERMES AGENT - WINDOWS 11 REMOTE GATEWAY
echo ======================================================================
echo.

:: Check Python installation
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python is not installed or not in PATH!
    echo Please install Python 3.10+ from python.org and ensure "Add Python to PATH" is checked.
    pause
    exit /b 1
)

echo [1/3] Checking dependencies...
pip install -r requirements.txt --quiet
if errorlevel 1 (
    echo [WARNING] Encountered issue installing some dependencies. Trying to start anyway...
)

echo.
echo [2/3] Checking Tailscale status...
tailscale ip -4 >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%i in ('tailscale ip -4') do set TAILSCALE_IP=%%i
    echo [INFO] Your Tailscale IPv4 address: %TAILSCALE_IP%
    echo Enter this IP in Hermes Control App on Android: %TAILSCALE_IP%
) else (
    echo [INFO] Tailscale not detected or not active. Local network IP will be used.
)

echo.
echo [3/3] Launching FastAPI Controller Gateway...
echo.
python hermes_gateway.py
pause
