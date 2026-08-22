@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar-sistema.ps1" %*
exit /b %ERRORLEVEL%
