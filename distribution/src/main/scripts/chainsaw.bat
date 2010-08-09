@echo off
set LIBDIR=%~dp0lib
set CHAINSAW_CLASSPATH=conf
setlocal EnableDelayedExpansion
for %%c in ("%LIBDIR%\*.jar") do set CHAINSAW_CLASSPATH=!CHAINSAW_CLASSPATH!;%%c
if "%JAVA_HOME%" == "" goto noJavaHome
set JAVACMD=%JAVA_HOME%\bin\java.exe
goto runChainsaw

:noJavaHome
set JAVACMD=java.exe

:runChainsaw
"%JAVACMD%" -Dorg.omg.CORBA.ORBClass=com.ibm.CORBA.iiop.ORB -cp "%CHAINSAW_CLASSPATH%" org.apache.log4j.chainsaw.LogUI
