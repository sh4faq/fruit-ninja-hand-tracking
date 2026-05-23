@echo off
REM Launches the Python MediaPipe hand tracking sidecar.
REM Keeps the window open so you can read any error messages.
REM
REM Usage:
REM   run-tracker.bat              - normal mode (waits for the Java game)
REM   run-tracker.bat --diagnose   - 5-second self-test, no game needed

cd /d "%~dp0"
echo Launching hand tracker...
echo Working dir: %CD%
echo.

REM -u forces unbuffered stdout/stderr so messages show up immediately even
REM when this batch is launched in a way that pipes output.
py -3.13 -u hand_tracker.py %*

echo.
echo --- Tracker exited with code %ERRORLEVEL%. Press any key to close. ---
pause >nul
