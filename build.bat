@echo off
REM Compile every .java file under src/ into bin/.
REM Requires Java 8 or later on PATH (javac will compile to a Java 8 target).
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -d bin --release 8 @sources.txt
del sources.txt
echo Build complete.
