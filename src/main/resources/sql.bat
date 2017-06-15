@echo off

set "CURRENT_DIR=%cd%"
if not "%SQL_HOME%" == "" goto filesql

set "SQL_HOME=%CURRENT_DIR%"
if exist "%SQL_HOME%\bin\sql.bat" goto filesql

cd ..
set "SQL_HOME=%cd%"
cd "%CURRENT_DIR%"


:filesql

java -jar "%SQL_HOME%/lib/sql.jar"  sql "-Dlog=%SQL_HOME%/log"  "-Dscript=%SQL_HOME%/script" "-Dtable=%SQL_HOME%/table" "-Dworkspace=%CURRENT_DIR%"