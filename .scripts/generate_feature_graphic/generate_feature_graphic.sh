#!/bin/bash
set -euo pipefail

script_location="$(dirname "$(readlink -f "$0")")"

for lang in "$script_location/../../fastlane/metadata/android/"*; do
  # Skip languages without title.txt
  if [ ! -f "$lang/title.txt" ]; then
    continue
  fi

  pushd "$lang"
  # Place temporary copy for editing if needed
  cp "$script_location/featureGraphic.svg" featureGraphic.svg
  if grep -q — title.txt; then
    # Try splitting title.txt on — (em dash)
    IFS='—' read -r appname subtext < title.txt
  elif grep -q – title.txt; then
    # No result, try splitting title.txt on – (en dash)
    IFS='–' read -r appname subtext < title.txt
  elif grep -q - title.txt; then
    # No result, try splitting on - (dash)
    IFS='-' read -r appname subtext < title.txt
  else
    # No result, use the full title as app name and default subtext
    appname=$(< title.txt)
    subtext="Loyalty Card Wallet"
  fi
  export appname=${appname%% }
  export subtext=${subtext## }
  # If the appname isn't Catima or there is subtext, change the .svg accordingly
  if [ "$appname" != "Catima" ] || [ -n "$subtext" ]; then
    perl -pi -e 's/Catima/$ENV{appname}/' featureGraphic.svg
    perl -pi -e 's/Loyalty Card Wallet/$ENV{subtext}/' featureGraphic.svg
    # Set correct font or font size for language if needed
    # (Lexend Deca has limited support and some characters are big)
    # We specifically need the Serif version because of the 200 weight
    case "$(basename "$lang")" in
      bg|el-GR|ru-RU|uk) sed -i "s/Lexend Deca/Noto Serif/" featureGraphic.svg ;;
      hi-IN) sed -i -e "s/Yesteryear/Noto Sans Devanagari/" -e "s/Lexend Deca/Noto Serif Devanagari/" featureGraphic.svg ;;
      ja-JP) sed -i "s/Lexend Deca/Noto Serif CJK JP/" featureGraphic.svg ;;
      kn-IN) sed -i -e 's/font-size="150"/font-size="100"/' -e "s/Yesteryear/Noto Serif Kannada/" featureGraphic.svg ;;
      ko) sed -i "s/Lexend Deca/Noto Serif CJK KR/" featureGraphic.svg ;;
      zh-CN) sed -i "s/Lexend Deca/Noto Serif CJK SC/" featureGraphic.svg ;;
      zh-TW) sed -i -e "s/Yesteryear/Noto Sans CJK TC/" -e "s/Lexend Deca/Noto Serif CJK TC/" featureGraphic.svg ;;
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
  popd
done
