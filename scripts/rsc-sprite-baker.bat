@echo off
setlocal
set "APP_ROOT=%~dp0.."
set "APP_JAR=%APP_ROOT%\target\rsc-sprite-baker.jar"

set "JAVA_CMD=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
where "%JAVA_CMD%" >nul 2>nul
if errorlevel 1 if not exist "%JAVA_CMD%" (
  echo RSC Sprite Baker requires Java 11 or newer.
  exit /b 1
)
for /f tokens^=3 %%V in ('"%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~V"
for /f "tokens=1 delims=." %%V in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%V"
if not defined JAVA_MAJOR (
  echo Could not determine Java version. RSC Sprite Baker requires Java 11 or newer.
  exit /b 1
)
if "%JAVA_MAJOR%"=="1" (
  echo RSC Sprite Baker requires Java 11 or newer. Set JAVA_HOME to a compatible JDK.
  exit /b 1
)
if %JAVA_MAJOR% LSS 11 (
  echo RSC Sprite Baker requires Java 11 or newer. Set JAVA_HOME to a compatible JDK.
  exit /b 1
)
if not exist "%APP_JAR%" (
  echo Application JAR not found: %APP_JAR%
  echo Build it with: mvn clean package
  exit /b 1
)

"%JAVA_CMD%" -jar "%APP_JAR%" %*
endlocal
