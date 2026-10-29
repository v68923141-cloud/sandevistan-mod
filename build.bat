@echo off
where gradle >nul 2>nul || (echo Gradle 8.1.1 not found. Install it from https://gradle.org/releases/ & pause & exit /b 1)
gradle build --no-daemon
echo.
echo Jar: build\libs\
pause
