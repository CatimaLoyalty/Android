#!/usr/bin/python3

import glob
import re

from typing import Iterator, List, Tuple

import requests

MIN_PERCENT = 90
NOT_LANGS = ("night", "w600dp")
REPLACE_CODES = {
    "el": "el-rGR",
    "id": "in-rID",
    "ro": "ro-rRO",
    "zh_Hans": "zh-rCN",
    "zh_Hant": "zh-rTW",
}
STATS_URL = "https://hosted.weblate.org/api/components/catima/catima/statistics/"


def get_weblate_langs() -> List[Tuple[str, int]]:
    r = requests.get(STATS_URL, timeout=5)
    r.raise_for_status()
    results = []
    for lang in r.json()["results"]:
        if lang["code"] != "en":
            code = REPLACE_CODES.get(lang["code"], lang["code"]).replace("_", "-r")
            results.append((code, round(lang["translated_percent"])))
    return sorted(results)


def get_dir_langs() -> List[str]:
    results = []
    for d in glob.glob("app/src/main/res/values-*"):
        code = d.split("-", 1)[1]
        if code not in NOT_LANGS:
            results.append(code)
    return sorted(results)


def get_xml_langs() -> List[Tuple[str, bool]]:
    results = []
    in_section = False
    with open("app/src/main/res/values/settings.xml") as fh:
        for line in fh:
            if not in_section and 'name="locale_values"' in line:
                in_section = True
            elif in_section:
                if "string-array" in line:
                    break
                disabled = "<!--" in line
                if m := re.search(r">(.*)<", line):
                    if m[1] != "en":
                        results.append((m[1], disabled))
    return sorted(results)


def update_xml_langs(langs: List[Tuple[str, bool]]) -> None:
    lines: List[str] = []
    in_section = False
    with open("app/src/main/res/values/settings.xml") as fh:
        for line in fh:
            if not in_section and 'name="locale_values"' in line:
                in_section = True
            elif in_section:
                if "string-array" in line:
                    in_section = False
                    lines.extend(_lang_lines(langs))
                else:
                    continue
            lines.append(line)
    with open("app/src/main/res/values/settings.xml", "w") as fh:
        for line in lines:
            fh.write(line)


def _lang_lines(langs: List[Tuple[str, bool]]) -> Iterator[str]:
    yield "        <item />\n"
    for lang, disabled in sorted(langs + [("en", False)]):
        if disabled:
            yield f"        <!-- <item>{lang}</item> -->\n"
        else:
            yield f"        <item>{lang}</item>\n"


def main() -> None:
    web_langs = get_weblate_langs()
    dir_langs = get_dir_langs()
    xml_langs = get_xml_langs()

    web_codes = set(code for code, _ in web_langs)
    dir_codes = set(dir_langs)
    xml_codes = set(code for code, _ in xml_langs)

    if diff := web_codes - dir_codes:
        print(f"WARNING: Weblate codes w/o dir: {diff}")
    if diff := xml_codes - dir_codes:
        print(f"WARNING: XML codes w/o dir: {diff}")

    percentages = dict(web_langs)
    all_langs = xml_langs[:]

    # add new langs as disabled
    for code in dir_codes - xml_codes:
        all_langs.append((code, True))

    # enable disabled langs if they are at least MIN_PERCENT translated now
    updated_langs = sorted(
        (code, percentages[code] < MIN_PERCENT if disabled else disabled)
        for code, disabled in all_langs
    )

    if updated_langs != xml_langs:
        print("Updating...")
        update_xml_langs(updated_langs)


if __name__ == "__main__":
    main()
