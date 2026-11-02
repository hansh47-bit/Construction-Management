@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0backend\scripts\Start-GmailNotifications.ps1"
if errorlevel 1 pause
