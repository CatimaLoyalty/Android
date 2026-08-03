**This documentation is for maintainers. If you're a user, please ignore it.**

# General versioning note
Android releases should be at least versionCode 1000 and end on an even number (the uneven numbers are used for F-Droid legacy builds). Valid: 1002, 1004, etc.

WearOS releases should be between 200 and 999 and only have to be bumped by 1. Valid: 200, 201, etc.

Version codes below 200 are from the pre-WearOS days.

# When releasing, do the following:
1. Press "Commit" and "Push" on Weblate to ensure all translations are up to date
2. Merge Weblate pull request
3. Make sure to pull the `main` branch locally
4. Update `CHANGELOG.md` with the new version name and the release date
5. Update the relevant build.gradle.kts with the new `versionCode` and `versionName`
   - For Android releases, this is `app/build.gradle.kts`, make sure to use an even number above 1000
   - For Wear OS releases, this is `wear/build.gradle.kts`, make sure to use a number between 200 and 1000
6. Create a commit for the new release:
   - For Android releases, `git add CHANGELOG.md app/build.gradle.kts && git commit -m "Release Catima <VERSION>"`
   - For Wear OS releases, `git add CHANGELOG.md wear/build.gradle.kts && git commit -m "Release Catima Wear OS <VERSION>"`
7. Build the new .apks:
   - For Android releases, `BUILD_TYPE=app KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh`
   - For Wear OS releases, `BUILD_TYPE=wear KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh`
8. Upload the Google release to Google Play Open Testing
   - For Android releases, this is `app/build/outputs/apk/gplay/release/app-gplay-release.apk`
   - For Wear OS releases, this is `wear/build/outputs/apk/release/wear-release.apk` (Wear OS currently does not use flavours)
9. Push the version update commit: `git push`
10. Create a new release on GitHub and attach the non-Google APKs and `SHA256SUMS` files
    - For Android releases, this is `app/build/outputs/apk/foss/release/app-foss-release.apk` and `SHA256SUMS`. Releases are tagged vX.X.X.
    - For Wear OS releases, this is `wear/build/outputs/apk/release/wear-release.apk` and `SHA256SUMs` (Wear OS currently does not use flavours). Releases are tagged vX.X.X-wearos.
11. When pushing the release to Google Play Production, update the metadata there: `bundle exec fastlane supply --version_code <VERSION_CODE>`
