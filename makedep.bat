@echo off
setlocal

set RAD_INSTALL_DIR=%~dp0\libs
rem call "%~dp0..\RadConsole\make" install
call "%~dp0..\TextUI\make" install
