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
    # (Lobster and Lexend have limited language support)
    case "$(basename "$lang")" in
      bg|el-GR|ru-RU|uk) sed -i "s/Lexend/Noto Sans/" featureGraphic.svg ;;
      ar|fa-IR) sed -i -e 's/svg direction="ltr"/svg direction="rtl"/' -e "s/Lobster/Noto Sans Arabic/" -e "s/Lexend/Noto Sans Arabic/" featureGraphic.svg ;;
      he-IL) sed -i -e "s/Lobster/Noto Sans Hebrew/" -e "s/Lexend/Noto Sans Hebrew/" featureGraphic.svg ;;
      hi-IN) sed -i -e "s/Lobster/Noto Sans Devanagari/" -e "s/Lexend/Noto Sans Devanagari/" featureGraphic.svg ;;
      ja-JP) sed -i "s/Lexend/Noto Sans CJK JP/" featureGraphic.svg ;;
      kn-IN) sed -i -e 's/font-size="150"/font-size="125"/' -e 's/\(<tspan x="469" \)y="270"/\1y="240"/' -e "s/Lobster/Noto Sans Kannada/" -e "s/Lexend/Noto Sans Kannada/" featureGraphic.svg ;;
      ko) sed -i "s/Lexend/Noto Sans CJK KR/" featureGraphic.svg ;;
      ta-IN) sed -i -e 's/font-size="150"/font-size="125"/' -e 's/\(<tspan x="469" \)y="270"/\1y="240"/' featureGraphic.svg ;;
      zh-CN) sed -i "s/Lexend/Noto Sans CJK SC/" featureGraphic.svg ;;
      zh-TW) sed -i -e "s/Lobster/Noto Sans CJK TC/" -e "s/Lexend/Noto Sans CJK TC/" featureGraphic.svg ;;
      *) ;;
    esac
  fi

  # Ensure images directory exists
  mkdir -p images
  # Generate .png (we use Inkscape because ImageMagick ignores RTL)
  xvfb-run inkscape --export-filename=images/featureGraphic.png featureGraphic.svg
  # Optimize .png
  optipng images/featureGraphic.png
  # Remove metadata (timestamps) from .png
  mat2 --inplace images/featureGraphic.png
  # Remove temporary .svg
  rm featureGraphic.svg
  popd
done
