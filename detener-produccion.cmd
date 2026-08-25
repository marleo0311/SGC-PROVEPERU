@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\detener-produccion.ps1" %*
