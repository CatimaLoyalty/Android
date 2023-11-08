#!/bin/bash
set -euo pipefail

script_location="$(dirname "$(readlink -f "$0")")"

for lang in "$script_location/../../fastlane/metadata/android/"*; do
  pushd "$lang" || exit 1
  # Place temporary copy for editing if needed
  cp "$script_location/featureGraphic.svg" featureGraphic.svg
  if grep -q — title.txt; then
    # Try splitting title.txt on — (em dash)
    IFS='—' read -r appname subtext < title.txt
  else
    # No result, try splitting on - (dash)
    IFS='-' read -r appname subtext < title.txt
  fi
  export appname=${appname%% }
  export subtext=${subtext## }
  # If there is subtext, change the .svg accordingly
  if [ -n "$subtext" ]; then
    perl -pi -e 's/Catima/$ENV{appname}/' featureGraphic.svg
    perl -pi -e 's/Loyalty Card Wallet/$ENV{subtext}/' featureGraphic.svg
    # Set correct font or font size for language if needed
    # (Lexend Deca has limited support and some characters are big)
    # We specifically need the Serif version because of the 200 weight
    case "$(basename "$lang")" in
      bg|el-GR|ru-RU|uk) sed -i "s/Lexend Deca/Noto Serif/" featureGraphic.svg ;;
      ja-JP) sed -i "s/Lexend Deca/Noto Serif CJK JP/" featureGraphic.svg ;;
      ko) sed -i "s/Lexend Deca/Noto Serif CJK KR/" featureGraphic.svg ;;
      kn-IN) sed -i -e 's/font-size="150"/font-size="100"/' -e 's/y="285.511"/y="235.511"/' featureGraphic.svg ;;
      zh-CN) sed -i "s/Lexend Deca/Noto Serif CJK SC/" featureGraphic.svg ;;
      zh-TW) sed -i "s/Lexend Deca/Noto Serif CJK TC/" featureGraphic.svg ;;
      *) ;;
    esac
  fi
  # Ensure images directory exists
  mkdir -p images
  # Generate .png
  convert featureGraphic.svg images/featureGraphic.png
  # Optimize .png
  optipng images/featureGraphic.png
  # Remove metadata (timestamps) from .png
  mat2 --inplace images/featureGraphic.png
  # Remove temporary .svg
  rm featureGraphic.svg
  popd || exit 1
done
