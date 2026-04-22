@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.3.2

@REM Set local scope for the variables with windows NT shell
@if "%OS%"=="Windows_NT" @setlocal

@set ERROR_CODE=0

@REM Set JAVA_HOME if not already set
@if not "%JAVA_HOME%"=="" goto OkJHome
@echo ERROR: JAVA_HOME not found in your environment. >&2
@echo Please set the JAVA_HOME variable in your environment to match the >&2
@echo location of your Java installation. >&2
@goto error

:OkJHome
@if exist "%JAVA_HOME%\bin\java.exe" goto init
@echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
@goto error

:init
@set MAVEN_PROJECTBASEDIR=%~dp0
@set MAVEN_WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
@set MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties

@if exist "%MAVEN_WRAPPER_JAR%" goto runWrapper
@echo Wrapper jar not found, trying system mvn...
@where mvn >nul 2>nul
@if %ERRORLEVEL% equ 0 (
    mvn %*
    @if ERRORLEVEL 1 goto error
    @goto end
)
@echo ERROR: Cannot find maven-wrapper.jar and mvn is not installed. >&2
@goto error

:runWrapper
"%JAVA_HOME%\bin\java.exe" ^
  %MAVEN_OPTS% ^
  -classpath "%MAVEN_WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*
@if ERRORLEVEL 1 goto error
@goto end

:error
@set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
@if not "%MAVEN_TERMINATION_UNHANDLED%"=="" goto exit
@exit /B %ERROR_CODE%

:exit
@exit %ERROR_CODE%
