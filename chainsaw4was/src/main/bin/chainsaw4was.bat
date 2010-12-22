@echo off
REM  Copyright 2010 Andreas Veithen
REM
REM  Licensed under the Apache License, Version 2.0 (the "License");
REM  you may not use this file except in compliance with the License.
REM  You may obtain a copy of the License at
REM
REM      http://www.apache.org/licenses/LICENSE-2.0
REM
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

set HOME=%~dp0

REM *** Locate Java executable ***

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome

set JAVA=%JAVA_HOME%\bin\java.exe

goto hasJavaHome

:noJavaHome

set JAVA=java.exe

:hasJavaHome

REM *** Build classpath ***

set CP="resources"
for %%f in ("%HOME%\lib\*.jar") do set CP=!CP!;%%f

"%JAVA%" -cp "!CP!" org.apache.log4j.chainsaw.LogUI
