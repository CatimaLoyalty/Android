#!/bin/bash
set -e

TITLE_FILE="fastlane/metadata/android/en-US/title.txt"
CANONICAL_TITLE=$(cat "$TITLE_FILE" | tr -d '\r\n')

# Languages allowed to have a different app_name
ALLOWLIST=("ja" "zh-rCN" "zh-rTW")

find app/src/main/res/values* -name "strings.xml" | while read xml; do
    LANG=$(echo "$xml" | sed -n 's|.*/values-\([^/]*\)/strings.xml|\1|p')
    LANG=${LANG:-en} # Default to 'en' for base values

    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$xml" | head -n1)

    # Check if language is allowlisted
    if [[ " ${ALLOWLIST[@]} " =~ " ${LANG} " ]]; then
        continue
    fi

    # Compare
    if [[ "$APP_NAME" != "$CANONICAL_TITLE" ]]; then
        echo "❌ app_name in $xml ($LANG) is '$APP_NAME', expected '$CANONICAL_TITLE'"
        exit 1
    fi
done

echo "All app_name values are consistent."
