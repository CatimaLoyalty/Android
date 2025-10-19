#!/bin/bash

set -e
shopt -s lastpipe   # Run last command in a pipeline in the current shell.

# Colors
LIGHTCYAN='\033[1;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'    # No Color

CANONICAL_TITLE="Catima"
ALLOWLIST=("ar" "ast" "bn" "bn-rIN" "bs" "cy" "fa" "he-rIL" "hi" "is" "iw" "kn" "lb" "ml" "mr" "oc" "ta" "zh-rTW")

success=1

find app/src/main/res/values* -name "strings.xml" | while read xml; do
    LANG=$(echo "$xml" | sed -n 's|.*/values-\([^/]*\)/strings.xml|\1|p')
    LANG=${LANG:-en}

    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$xml" | head -n1)

    if [[ " ${ALLOWLIST[@]} " =~ " ${LANG} " ]]; then
        continue

    elif [[ ! -s "$xml" || -z "$APP_NAME" ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}app_name is missing or file is empty in $xml ($LANG). ${NC}"
        success=$((success & 0))

    elif [[ "$APP_NAME" != "$CANONICAL_TITLE" ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}app_name in $xml ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected ${GREEN}'$CANONICAL_TITLE'. ${NC}"
        success=$((success & 0))
    fi
done

if [[ $success -eq 1 ]]; then
    echo -e "\n${GREEN}Success!! All app_name values match the canonical title. ${NC}"
else
    echo -e "\n${RED}Unsuccessful!! Some app_name values did not match the canonical titles. ${NC}"
    exit 1
fi