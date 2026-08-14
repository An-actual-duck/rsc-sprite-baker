@echo off
setlocal
set "APP_ROOT=%~dp0.."
set "APP_JAR=%APP_ROOT%\target\rsc-sprite-baker.jar"

where java >nul 2>nul
if errorlevel 1 (
  echo RSC Sprite Baker requires Java 11 or newer.
  exit /b 1
)
if not exist "%APP_JAR%" (
  echo Application JAR not found: %APP_JAR%
  echo Build it with: mvn clean package
  exit /b 1
)

java -jar "%APP_JAR%" %*
endlocal
