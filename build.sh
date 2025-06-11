#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

### build.sh
### Builds Catima the same way rbtlog/IzzyOnDroid does for reproducible builds

if [ -z "${ANDROID_SDK_ROOT:-}" ]; then
  echo "ANDROID_SDK_ROOT is not set, setting to $HOME/Android/Sdk";
  export ANDROID_SDK_ROOT=$HOME/Android/Sdk
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME is not set, setting to Java 21"
  if [ -f "/etc/debian_version" ]; then
    echo "Debian-based distro, Java 21 is /usr/lib/jvm/java-21-openjdk-amd64"
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  else
    echo "Not Debian-based, assuming Fedora and setting Java 21 as /usr/lib/jvm/java-21-openjdk"
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
  fi
fi

echo "Starting build"
./gradlew clean assembleRelease

echo "Build finished (unsigned)"
flavourDirs=$(find app/build/outputs/apk/ -mindepth 1 -maxdepth 1 -type d)
for flavourDir in $flavourDirs; do
  flavourName="$(basename "$flavourDir")"
  echo "Your $flavourName flavour is at $flavourDir/release/app-$flavourName-release-unsigned.apk"
done

if [ -z "${KEYSTORE:-}" ]; then
  echo "KEYSTORE not set, skipping signing..."
else
  if [ -z "${KEYSTORE_ALIAS:-}" ]; then
    echo "KEYSTORE_ALIAS is not set, setting to catima"
    KEYSTORE_ALIAS=catima
  fi

  apksigner_version="$(ls -1 "$ANDROID_SDK_ROOT/build-tools/" | tail -n 1)"

  for flavourDir in $flavourDirs; do
    flavourName="$(basename "$flavourDir")"
    echo "Signing $flavourName flavour..."
    cp "$flavourDir/release/app-$flavourName-release-unsigned.apk" "$flavourDir/release/app-$flavourName-release.apk"
    "$ANDROID_SDK_ROOT/build-tools/$apksigner_version/apksigner" sign -v --ks "$KEYSTORE" --ks-key-alias "$KEYSTORE_ALIAS" "$flavourDir/release/app-$flavourName-release.apk"

    echo "Build finished (signed)"
    echo "Your $flavourName flavour is at $flavourDir/release/app-$flavourName-release.apk"
  done

  shasumPath="$(pwd)/SHA256SUMS"
  echo "" > "$shasumPath"

  for flavourDir in $flavourDirs; do
    pushd "$flavourDir/release/"
    sha256sum -- *.apk >> "$shasumPath"
    popd
  done

  echo "SHA256SUMS generated"
  echo "Your SHA256SUMS are at SHA256SUMS"
fi
