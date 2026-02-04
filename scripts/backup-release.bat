@echo off
REM Backup Release Script for Ciclismo Portugal
REM Usage: backup-release.bat 1.2.1

IF "%1"=="" (
    echo Error: Version number required
    echo Usage: backup-release.bat 1.2.1
    exit /b 1
)

SET VERSION=%1
SET BACKUP_DIR=%USERPROFILE%\Releases\v%VERSION%
SET PROJECT_DIR=%~dp0..

echo.
echo ========================================
echo  Backing up Release v%VERSION%
echo ========================================
echo.

REM Create backup directory
echo [1/5] Creating backup directory...
mkdir "%BACKUP_DIR%" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo Directory already exists, continuing...
)

REM Copy AAB file
echo [2/5] Copying AAB file...
copy "%PROJECT_DIR%\app\build\outputs\bundle\release\app-release.aab" "%BACKUP_DIR%\app-release.aab" /Y
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: AAB file not found!
    exit /b 1
)

REM Copy ProGuard mapping
echo [3/5] Copying ProGuard mapping...
copy "%PROJECT_DIR%\app\build\outputs\mapping\release\mapping.txt" "%BACKUP_DIR%\mapping.txt" /Y
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: mapping.txt not found, skipping...
)

REM Copy release notes
echo [4/5] Copying release notes...
copy "%PROJECT_DIR%\RELEASE_NOTES_v%VERSION%*.md" "%BACKUP_DIR%\" /Y 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: Release notes not found, skipping...
)

REM Create metadata
echo [5/5] Creating metadata...
(
echo {
echo   "version": "%VERSION%",
echo   "buildDate": "%DATE% %TIME%",
echo   "buildBy": "%USERNAME%",
echo   "computerName": "%COMPUTERNAME%"
echo }
) > "%BACKUP_DIR%\metadata.json"

REM Summary
echo.
echo ========================================
echo  Backup Complete!
echo ========================================
echo.
echo Location: %BACKUP_DIR%
echo.
echo Files backed up:
dir /B "%BACKUP_DIR%"
echo.
echo Next steps:
echo 1. Upload to Google Drive/Cloud storage
echo 2. Upload AAB to Play Console
echo 3. Verify keystore backup exists
echo.
pause
