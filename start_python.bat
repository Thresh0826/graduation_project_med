@echo off
cd /d "%~dp0Python_script"
echo Starting Python Service on port 8000...
.venv\Scripts\python.exe image_parser_service.py
pause
