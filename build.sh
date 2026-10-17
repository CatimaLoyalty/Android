#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

### build.sh
### Builds Catima the same way rbtlog/IzzyOnDroid does for reproducible builds

if [ -z "${BUILD_TYPE:-}" ]; then
  echo "BUILD_TYPE is not set, setting to app."
  export BUILD_TYPE="app"
fi

if [ "${BUILD_TYPE}" != "app" ] && [ "${BUILD_TYPE}" != "wear" ]; then
  echo "Invalid BUILD_TYPE ${BUILD_TYPE}, must be either app or wear."
  exit 1
fi

if [ -z "${ANDROID_HOME:-}" ]; then
  echo "ANDROID_HOME is not set, setting to $HOME/Android/Sdk";
  export ANDROID_HOME=$HOME/Android/Sdk
fi

if [ -z "${JAVA_HOME:-}" ]; then
  echo "JAVA_HOME is not set, setting to Java 25"
  if [ -f "/etc/debian_version" ]; then
    echo "Debian-based distro, Java 25 is /usr/lib/jvm/java-25-openjdk-amd64"
    export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
  else
    echo "Not Debian-based, assuming Fedora and setting Java 25 as /usr/lib/jvm/java-25-openjdk"
    export JAVA_HOME=/usr/lib/jvm/java-25-openjdk
  fi
fi

echo "Starting build"
./gradlew clean ":${BUILD_TYPE}:assembleRelease"

echo "Build finished (unsigned)"
flavourDirs=$(find "${BUILD_TYPE}/build/outputs/apk/" -mindepth 1 -maxdepth 1 -type d)
for flavourDir in $flavourDirs; do
  flavourName="$(basename "$flavourDir")"
  if [ "$flavourName" == "release" ]; then
    echo "Your $flavourName flavour is at $flavourDir/${BUILD_TYPE}-release-unsigned.apk"
  else
    echo "Your $flavourName flavour is at $flavourDir/release/${BUILD_TYPE}-$flavourName-release-unsigned.apk"
  fi
done

if [ -z "${KEYSTORE:-}" ]; then
  echo "KEYSTORE not set, skipping signing..."
else
  if [ -z "${KEYSTORE_ALIAS:-}" ]; then
    echo "KEYSTORE_ALIAS is not set, setting to catima"
    KEYSTORE_ALIAS=catima
  fi

  apksigner_version="$(ls -1 "$ANDROID_HOME/build-tools/" | tail -n 1)"

  for flavourDir in $flavourDirs; do
    flavourName="$(basename "$flavourDir")"
    echo "Signing $flavourName flavour..."
    if [ "$flavourName" == "release" ]; then
      cp "$flavourDir/${BUILD_TYPE}-release-unsigned.apk" "$flavourDir/${BUILD_TYPE}-release.apk"
      "$ANDROID_HOME/build-tools/$apksigner_version/apksigner" sign -v --ks "$KEYSTORE" --ks-key-alias "$KEYSTORE_ALIAS" "$flavourDir/${BUILD_TYPE}-release.apk"
    else
      cp "$flavourDir/release/${BUILD_TYPE}-$flavourName-release-unsigned.apk" "$flavourDir/release/${BUILD_TYPE}-$flavourName-release.apk"
      "$ANDROID_HOME/build-tools/$apksigner_version/apksigner" sign -v --ks "$KEYSTORE" --ks-key-alias "$KEYSTORE_ALIAS" "$flavourDir/release/${BUILD_TYPE}-$flavourName-release.apk"
    fi

    echo "Build finished (signed)"
    if [ "$flavourName" == "release" ]; then
      echo "Your $flavourName flavour is at $flavourDir/${BUILD_TYPE}-release.apk"
    else
      echo "Your $flavourName flavour is at $flavourDir/release/${BUILD_TYPE}-$flavourName-release.apk"
    fi
  done

  shasumPath="$(pwd)/SHA256SUMS"
  echo "" > "$shasumPath"

  for flavourDir in $flavourDirs; do
    flavourName="$(basename "$flavourDir")"
    if [ "$flavourName" == "release" ]; then
      pushd "$flavourDir"
    else
      pushd "$flavourDir/release/"
    fi
    sha256sum -- "${BUILD_TYPE}"-*.apk >> "$shasumPath"
    popd
  done

  echo "SHA256SUMS generated"
  echo "Your SHA256SUMS are at SHA256SUMS"
fi
