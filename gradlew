#!/usr/bin/env sh
# Dummy wrapper for preview — real gradle wrapper should be generated via `gradle wrapper`
# In CI, setup-java + setup-android will provide gradle.
echo "Use Android Studio or run: gradle wrapper && ./gradlew assembleDebug"
