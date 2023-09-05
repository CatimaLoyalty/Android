#!/usr/bin/python3

import subprocess
import xml.etree.ElementTree as ET

root = ET.parse("app/src/main/res/values/settings.xml").getroot()
for e in root.findall("string-array"):
    if e.get("name") == "locale_values":
        locales = [x.text for x in e if x.text]
        break

locales = [
    # e.g. de or es-rAR (not es-AR)
    loc.replace("-", "-r") if "-" in loc and loc[loc.index("-") + 1] != "r" else loc
    for loc in locales
]

res = ", ".join(f'"{loc}"' for loc in locales)
sed = [
    "sed",
    "-i",
    f"s/resourceConfigurations .*/resourceConfigurations += [{res}]/",
    "app/build.gradle"
]
subprocess.run(sed, check=True)

with open("app/src/main/res/xml/locales_config.xml", "w") as fh:
    fh.write('<?xml version="1.0" encoding="utf-8"?>\n')
    fh.write('<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n')
    fh.write('    <locale android:name="en-US" />\n')
    for loc in locales:
        if loc != "en":
            # e.g. de or en-AR (not es-rAR)
            loc = loc.replace("-r", "-")
            fh.write(f'    <locale android:name="{loc}" />\n')
    fh.write('</locale-config>\n')
