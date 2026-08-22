@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0detener-sistema.ps1" %*
exit /b %ERRORLEVEL%
