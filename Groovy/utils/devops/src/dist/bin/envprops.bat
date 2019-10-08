@echo off
setlocal enabledelayedexpansion

echo Manage environment properties...
echo.

@REM Check JAVA_HOME
if "%JAVA_HOME%" == "" (
  set JAVA_EXE="C:\Program Files\Java\jre7\bin\java.exe"
) else (
  set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

echo Using Java: %JAVA_EXE%

if "%CONF_DIR%" == "" (
  set CONF_DIR=%userprofile%\devops\conf
)

echo "Using CONF_DIR: %CONF_DIR%"

@REM Check log config
if not exist %CONF_DIR%\devops-logback.xml (
  echo Please configure files at %CONF_DIR%
  exit /B 1
)

@REM Check log config
if not exist %CONF_DIR%\devops-logback.xml (
  echo Please configure files at %CONF_DIR%
  exit /B 1
)

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_HOME=%DIRNAME%..

rem --------------------------------------
set JVM_OPTS=%JVM_OPTS% -DAPP_HOME="%APP_HOME%" -Dlogback.configurationFile="%CONF_DIR%\devops-logback.xml" -Djava.util.logging.config.file="%APP_HOME%\conf\jlog.properties"
rem --------------------------------------

set CMD_LINE_ARGS=%*

rem include all files under lib
set CLASSPATH=
for /f "delims=" %%f in ('dir /b %APP_HOME%\lib') do (
  set CLASSPATH=!CLASSPATH!;%APP_HOME%\lib\%%f
)

%JAVA_EXE% %JVM_OPTS% -classpath "%CLASSPATH%" com.devops.admin.EnvPropsManager %CMD_LINE_ARGS%

