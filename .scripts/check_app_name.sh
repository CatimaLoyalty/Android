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
ALLOWLIST_TITLE_TXT=("ar" "fa-IR" "he-IL" "hi-IN" "kn-IN" "zh-TW")
ALLOWLIST_STRING_XML=("ar" "ast" "bn" "bn-rIN" "bs" "cy" "fa" "he-rIL" "hi" "is" "iw" "kn" "lb" "ml" "mr" "oc" "ta" "zh-rTW")

# Arguments:
#   $1: LANG
#   $2: APP_NAME
#   $3: FILE
function check() {
    if [[ ! -s $3 || -z $2 ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}app_name/title is missing or file is empty in $3 ($1). ${NC}"
        SUCCESS=0

    elif [[ ! $2 =~ ^${CANONICAL_TITLE} ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}app_name/title in $3 ($1) is ${RED}'$2'${LIGHTCYAN}, expected to start with ${GREEN}'$CANONICAL_TITLE'. ${NC}"
        SUCCESS=0
    fi
}

echo -e "${LIGHTCYAN}Checking title.txt's. ${NC}"

find fastlane/metadata/android/* -maxdepth 1 -type f -name "title.txt" | while read FILE; do
    LANG=$(echo $FILE | sed -n 's|.*/\([^/]*\)/title.txt|\1|p')
    LANG=${LANG:-en}

    APP_NAME=$(head -n 1 $FILE)

    if [[ ${ALLOWLIST_TITLE_TXT[@]} =~ ${LANG} ]]; then
        continue
    fi

    check "$LANG" "$APP_NAME" "$FILE"
done

echo -e "${LIGHTCYAN}Checking string.xml's. ${NC}"

find app/src/main/res/values* -maxdepth 1 -type f -name "strings.xml" | while read FILE; do
    LANG=$(echo "$FILE" | sed -n 's|.*/values-\([^/]*\)/strings.xml|\1|p')
    LANG=${LANG:-en}

    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$FILE" | head -n1)

    if [[ " ${ALLOWLIST_STRING_XML[@]} " =~ " ${LANG} " ]]; then
        continue
    fi

    check "$LANG" "$APP_NAME" "$FILE"
done

if [[ $SUCCESS -eq 1 ]]; then
    echo -e "\n${GREEN}Success!! All app_name values match the canonical title. ${NC}"
else
    echo -e "\n${RED}Unsuccessful!! Some app_name values did not match the canonical titles. ${NC}"
    exit 1
fi