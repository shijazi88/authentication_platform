@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script for Windows
@REM Bootstraps Maven 3.9.9 on first run; subsequent runs use the cached binary.
@REM ----------------------------------------------------------------------------

@echo off
setlocal enabledelayedexpansion

set BASE_DIR=%~dp0
set WRAPPER_PROPERTIES=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties

if not exist "%WRAPPER_PROPERTIES%" (
    echo Cannot find %WRAPPER_PROPERTIES%
    exit /b 1
)

for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_PROPERTIES%") do (
    if "%%A"=="distributionUrl" set DIST_URL=%%B
)

if "%DIST_URL%"=="" (
    echo distributionUrl is not set in %WRAPPER_PROPERTIES%
    exit /b 1
)

set MAVEN_USER_HOME=%USERPROFILE%\.m2
for %%I in ("%DIST_URL%") do set DIST_FILE=%%~nI
set INSTALL_DIR=%MAVEN_USER_HOME%\wrapper\dists\%DIST_FILE%
set MVN_BIN=%INSTALL_DIR%\%DIST_FILE%\bin\mvn.cmd

if not exist "%MVN_BIN%" (
    echo Bootstrapping Maven from %DIST_URL% ...
    if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
    powershell -NoProfile -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%INSTALL_DIR%\maven.zip'" || exit /b 1
    powershell -NoProfile -Command "Expand-Archive -Path '%INSTALL_DIR%\maven.zip' -DestinationPath '%INSTALL_DIR%' -Force" || exit /b 1
    del "%INSTALL_DIR%\maven.zip"
)

if "%JAVA_HOME%"=="" (
    where java >nul 2>nul || (
        echo JAVA_HOME is not set and java is not on PATH.
        exit /b 1
    )
)

call "%MVN_BIN%" %*
