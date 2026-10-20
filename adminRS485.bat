@echo off
java -cp "%~dp0Clientboardrs485plugin-10.2.jar;%~dp0lib/RXTXLibrary.jar;%~dp0lib/RXTXcomm.jar;%~dp0lib/QSystem.jar" ru.apertum.cbrs485.test.TestRS
pause
