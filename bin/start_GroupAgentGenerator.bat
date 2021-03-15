@echo off

cd %~dp0
cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

if "%~1"=="" (
    echo Syntax error!
	echo. 
    echo Usage: start_GroupAgentGenerator filePathServiceAgent1 path2
) else (
	java -cp %CLASSPATH% i5.las2peer.tools.GroupAgentGenerator %1 %2
)
