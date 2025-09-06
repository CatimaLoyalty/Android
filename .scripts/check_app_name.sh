#!/bin/bash
set -e

CANONICAL_TITLE="Catima"

ALLOWLIST=("ja" "zh-rCN" "zh-rTW")

success=1
find app/src/main/res/values* -name "strings.xml" | while read xml; do
    LANG=$(echo "$xml" | sed -n 's|.*/values-\([^/]*\)/strings.xml|\1|p')
    LANG=${LANG:-en}

    if [[ " ${ALLOWLIST[@]} " =~ " ${LANG} " ]]; then
        continue
    fi

    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$xml" | head -n1)

    if [[ ! -s "$xml" || -z "$APP_NAME" ]]; then
        echo "Error: app_name is missing or file is empty in $xml ($LANG)"
        success=0
        continue
    fi

    if [[ "$APP_NAME" != "$CANONICAL_TITLE" ]]; then
        echo "Error: app_name in $xml ($LANG) is '$APP_NAME', expected '$CANONICAL_TITLE'"
        success=0
    fi
done

if [[ $success -eq 1 ]]; then
    echo "Success! All app_name values match the canonical title."
else
    echo "Unsuccessful. Some app_name values did not match the canonical titles."
    exit 1
fi
