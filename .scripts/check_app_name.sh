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
ALLOWLIST=("ar" "bn" "fa" "fa-IR" "he-IL" "hi" "hi-IN" "kn" "kn-IN" "ml" "mr" "ta" "ta-IN" "zh-rTW" "zh-TW")  # TODO: Link values and fastlane with different codes together

function get_lang() {
    LANG_DIRNAME=$(dirname "$FILE" | xargs basename)
    LANG=${LANG_DIRNAME#values-}    # Fetch lang name
    LANG=${LANG#values}             # Handle "app/src/main/res/values"
    LANG=${LANG:-en}                # Default to en
}

# FIXME: This function should use its own variables and return a success/fail status, instead of working on global variables
function check() {
    # FIXME: This allows inconsistency between values and fastlane if the app name is not Catima
    # When the app name is not Catima, it should still check if title.txt and strings.xml use the same app name (or start)
    if echo "${ALLOWLIST[*]}" | grep -w -q "${LANG}" || [[ -z ${APP_NAME} ]]; then
        return 0
    fi

    if [[ ${FILE} == *"title.txt" ]]; then
        if [[ ! ${APP_NAME} =~ ^${CANONICAL_TITLE} ]]; then
            echo -e "${RED}Error: ${LIGHTCYAN}title in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected to start with ${GREEN}'$CANONICAL_TITLE'. ${NC}"
            SUCCESS=0
        fi
    else
        if [[ ${APP_NAME} != "${CANONICAL_TITLE}" ]]; then
            echo -e "${RED}Error: ${LIGHTCYAN}app_name in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected ${GREEN}'$CANONICAL_TITLE'. ${NC}"
            SUCCESS=0
        fi
    fi
}

# FIXME: This checks all title.txt and strings.xml files separately, but it needs to check if the title.txt and strings.xml match for a language as well
echo -e "${LIGHTCYAN}Checking title.txt's. ${NC}"

find fastlane/metadata/android/* -maxdepth 1 -type f -name "title.txt" | while read -r FILE; do
    APP_NAME=$(head -n 1 "$FILE")

    get_lang
    check
done

echo -e "${LIGHTCYAN}Checking string.xml's. ${NC}"

find app/src/main/res/values* -maxdepth 1 -type f -name "strings.xml" | while read -r FILE; do
    # FIXME: This only checks app_name, but there are more strings with Catima inside it
    # It should check the original English text for all strings that contain Catima and ensure they use the correct app_name for consistency
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
