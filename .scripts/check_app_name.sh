#!/bin/bash

set -e
shopt -s lastpipe   # Run last command in a pipeline in the current shell.

# Colors
LIGHTCYAN='\033[1;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'    # No Color

# Vars
SUCCESS=1
CANONICAL_TITLE="Catima"
ALLOWLIST=("ar" "bn" "fa" "fa-IR" "he-IL" "hi" "hi-IN" "kn" "kn-IN" "ml" "mrx" "ta" "ta-IN" "zh-rTW" "zh-TW")

function get_lang() {
    LANG_DIRNAME=$(dirname $FILE | xargs basename)
    LANG=${LANG_DIRNAME#values-}    # Fetch lang name
    LANG=${LANG#values}             # Handle "app/src/main/res/values"
    LANG=${LANG:-en}                # Default to en
}

function check() {
    if [[ ${ALLOWLIST[@]} =~ ${LANG} || -z ${APP_NAME} ]]; then
        return 0

    elif [[ ! ${APP_NAME} =~ ^${CANONICAL_TITLE} ]]; then
        if [[ ${FILE} =~ "title.txt" ]]; then
            echo -e "${RED}Error: ${LIGHTCYAN}title in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected to start with ${GREEN}'$CANONICAL_TITLE'. ${NC}"
        else
            echo -e "${RED}Error: ${LIGHTCYAN}app_name in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected ${GREEN}'$CANONICAL_TITLE'. ${NC}"
        fi

        SUCCESS=0
    fi
}

echo -e "${LIGHTCYAN}Checking title.txt's. ${NC}"

find fastlane/metadata/android/* -maxdepth 1 -type f -name "title.txt" | while read FILE; do
    APP_NAME=$(head -n 1 $FILE)

    get_lang
    check
done

echo -e "${LIGHTCYAN}Checking string.xml's. ${NC}"

find app/src/main/res/values* -maxdepth 1 -type f -name "strings.xml" | while read FILE; do
    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$FILE" | head -n1)

    get_lang
    check
done

if [[ $SUCCESS -eq 1 ]]; then
    echo -e "\n${GREEN}Success!! All app_name values match the canonical title. ${NC}"
else
    echo -e "\n${RED}Unsuccessful!! Some app_name values did not match the canonical titles. ${NC}"
    exit 1
fi