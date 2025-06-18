**This documentation is for maintainers. If you're a user, please ignore it.**

# When releasing, do the following:
1. Press "Commit" and "Push" on Weblate to ensure all translations are up to date
2. Merge Weblate pull request
3. Update `CHANGELOG.md` with the new version name and the release date
4. Update `app/build.gradle.kts` with the new `versionCode` and `versionName`
5. Create a commit for the new release: `git add CHANGELOG.md app/build.gradle.kts && git commit -m "Release Catima <VERSION>"`
6. Build the new .apks: `KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh`
7. Upload `app/build/outputs/apk/gplay/release/app-gplay-release.apk` to Google Play Open Testing
8. Push the version update commit: `git push`
9. Create a new release on GitHub and attach the `app/build/outputs/apk/foss/release/app-foss-release.apk` and `SHA256SUMS` files
10. When pushing the release to Google Play Production, update the metadata there: `bundle exec fastlane supply --version_code <VERSION_CODE>`
