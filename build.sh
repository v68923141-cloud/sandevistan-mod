#!/bin/sh
command -v gradle >/dev/null || { echo "Install Gradle 8.1.1 (https://gradle.org/releases/)"; exit 1; }
gradle build --no-daemon && echo "Jar: build/libs/"
