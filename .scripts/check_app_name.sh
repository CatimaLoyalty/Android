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
ALLOWLIST=("ar" "bn" "fa" "fa-IR" "he-IL" "hi" "hi-IN" "kn" "kn-IN" "ml" "mrx" "ta" "zh-rTW" "zh-TW")

function get_lang() {
    LANG=$(echo "$FILE" | LC_ALL=C perl -nE "say \$2 if /.*\/(values-)?(?!values)([a-zA-Z-]+)\/($1)/")  # LC_ALL=C to suppress perl warning.
    LANG=${LANG:-en}
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

    get_lang "title.txt"
    check
done

echo -e "${LIGHTCYAN}Checking string.xml's. ${NC}"

find app/src/main/res/values* -maxdepth 1 -type f -name "strings.xml" | while read FILE; do
    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$FILE" | head -n1)

    get_lang "strings.xml"
    check
done

if [[ $SUCCESS -eq 1 ]]; then
    echo -e "\n${GREEN}Success!! All app_name values match the canonical title. ${NC}"
else
    echo -e "\n${RED}Unsuccessful!! Some app_name values did not match the canonical titles. ${NC}"
    exit 1
fi