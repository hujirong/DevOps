@echo off
REM Licensed Materials - Property of IBM* and/or HCL**
REM UrbanCode Deploy
REM UrbanCode Build
REM UrbanCode Release
REM AnthillPro
REM (c) Copyright IBM Corporation 2011, 2017. All Rights Reserved.
REM (c) Copyright HCL Technologies Ltd. 2018. All Rights Reserved.
REM
REM U.S. Government Users Restricted Rights - Use, duplication or disclosure restricted by
REM GSA ADP Schedule Contract with IBM Corp.
REM
REM * Trademark of International Business Machines
REM ** Trademark of HCL Technologies Limited

setlocal

if "%JAVA_HOME%" == "" goto doJavaHomeNotSet

set JAVACMD="%JAVA_HOME%\bin\java"
set JARFILE=%~dp0udclient.jar

if not exist "%JARFILE%" goto doJarNotFound

%JAVACMD% -jar "%JARFILE%" %*
set EXIT_VAL=%ERRORLEVEL%
goto end

:doJarNotFound
echo Didn't find %JARFILE% in directory %~dp0
set EXIT_VAL=1
goto end

:doJavaHomeNotSet
echo You must have JAVA_HOME set on your environment to use the uDeploy client.
set EXIT_VAL=1
goto end 

:end
exit /b %EXIT_VAL%
