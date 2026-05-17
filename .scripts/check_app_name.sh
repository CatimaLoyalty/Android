#!/bin/bash
set -e
shopt -s lastpipe

# Colors
LIGHTCYAN='\033[1;36m'
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

# Vars
SUCCESS=1
CANONICAL_TITLE="Catima"

# peo is only temporarily allowed due to https://github.com/WeblateOrg/weblate/issues/17455
ALLOWLIST=("ar" "bn" "fa" "fa-IR" "he-IL" "hi" "hi-IN" "kn" "kn-IN" "ml" "mr" "peo" "ta" "ta-IN" "zh-rTW" "zh-TW")

declare -A FASTLANE_TITLES   # lang -> title.txt app name
declare -A STRINGS_NAMES     # lang -> strings.xml app_name

function get_lang() {
    LANG_DIRNAME=$(dirname "$FILE" | xargs basename)
    LANG=${LANG_DIRNAME#values-}
    LANG=${LANG#values}
    LANG=${LANG:-en}
}

function is_allowlisted() {
    local lang="$1"
    for entry in "${ALLOWLIST[@]}"; do
        if [[ "$entry" == "$lang" ]]; then
            return 0
        fi
    done
    return 1
}

# --- Pass 1: collect title.txt values ---
echo -e "${LIGHTCYAN}Checking title.txt's.${NC}"
find fastlane/metadata/android/* -maxdepth 1 -type f -name "title.txt" | while read -r FILE; do
    APP_NAME=$(head -n 1 "$FILE")
    get_lang

    FASTLANE_TITLES["$LANG"]="$APP_NAME"

    if is_allowlisted "$LANG"; then
        continue
    fi

    if [[ -n "$APP_NAME" && ! "$APP_NAME" =~ ^${CANONICAL_TITLE} ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}title in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected to start with ${GREEN}'$CANONICAL_TITLE'.${NC}"
        SUCCESS=0
    fi
done

# --- Pass 2: collect strings.xml app_name values ---
echo -e "${LIGHTCYAN}Checking strings.xml's.${NC}"
find app/src/main/res/values* -maxdepth 1 -type f -name "strings.xml" | while read -r FILE; do
    APP_NAME=$(grep -oP '<string name="app_name">\K[^<]+' "$FILE" | head -n1)
    get_lang

    [[ -n "$APP_NAME" ]] && STRINGS_NAMES["$LANG"]="$APP_NAME"

    if [[ -z "$APP_NAME" ]] || is_allowlisted "$LANG"; then
        continue
    fi

    if [[ "$APP_NAME" != "$CANONICAL_TITLE" ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}app_name in $FILE ($LANG) is ${RED}'$APP_NAME'${LIGHTCYAN}, expected ${GREEN}'$CANONICAL_TITLE'.${NC}"
        SUCCESS=0
    fi

    # Check all strings containing "Catima" use app_name for consistency
    while IFS= read -r line; do
        string_name=$(echo "$line" | grep -oP 'name="\K[^"]+')
        string_val=$(echo "$line" | grep -oP '>[^<]+<' | tr -d '><')
        if [[ "$string_name" != "app_name" && "$string_val" == *"Catima"* ]]; then
            echo -e "${RED}Warning: ${LIGHTCYAN}string '$string_name' in $FILE ($LANG) hardcodes 'Catima' instead of using @string/app_name.${NC}"
        fi
    done < <(grep -E '<string name=".+">.*Catima.*</string>' "$FILE")
done

# --- Pass 3: cross-check title.txt vs strings.xml per language ---
echo -e "${LIGHTCYAN}Cross-checking title.txt vs strings.xml per language.${NC}"
for lang in "${!FASTLANE_TITLES[@]}"; do
    fastlane_name="${FASTLANE_TITLES[$lang]}"
    strings_name="${STRINGS_NAMES[$lang]:-}"

    if [[ -z "$strings_name" ]]; then
        continue
    fi

    if is_allowlisted "$lang"; then
        continue
    fi

    # Strip subtitle after any dash variant or colon, then remove all whitespace
    fastlane_appname=$(echo "$fastlane_name" | sed 's/[â€”â€“\-].*//' | sed 's/:.*//' | tr -d '[:space:]')
    if [[ "$fastlane_appname" != "$strings_name" ]]; then
        echo -e "${RED}Error: ${LIGHTCYAN}lang '$lang' title.txt app name='$fastlane_appname' does not match strings.xml app_name='$strings_name'.${NC}"
        SUCCESS=0
    fi
done

if [[ $SUCCESS -eq 1 ]]; then
    echo -e "\n${GREEN}Success!! All app_name values match the canonical title.${NC}"
else
    echo -e "\n${RED}Unsuccessful!! Some app_name values did not match the canonical titles.${NC}"
    exit 1
fi