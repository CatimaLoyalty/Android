#!/usr/bin/python3

import os
import re

changelogs = {}

with open('CHANGELOG.md') as changelog:
    version_code = None
    text = []

    for line in changelog:
        if line.startswith("## "):
            if version_code != None:
                changelogs[version_code] = text

            text = []
            match = re.match("## \S* - (\d*).*", line)
            if not match:
                raise ValueError(f"Invalid version line: {line}")
            version_code = match.group(1)
        elif line:
            # Turn Markdown [links](to_url) into links (to_url)
            text.append(re.sub(r'\[(.*?)\]\((.*?)\)', r'\1 (\2)', line))

for version, description in changelogs.items():
    description = "".join(description).strip()

    if not description:
        continue

    with open(os.path.join("fastlane", "metadata", "android", "en-US", "changelogs", f"{version}.txt"), "w") as fastlane_file:
        fastlane_file.write(description)
