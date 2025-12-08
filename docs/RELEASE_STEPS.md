**This documentation is for maintainers. If you're a user, please ignore it.**

# When releasing, do the following:
1. Press "Commit" and "Push" on Weblate to ensure all translations are up to date
2. Merge Weblate pull request
3. Make sure to pull the `main` branch locally
4. Update `CHANGELOG.md` with the new version name and the release date
5. Update `app/build.gradle.kts` with the new `versionCode` and `versionName`
6. Create a commit for the new release: `git add CHANGELOG.md app/build.gradle.kts && git commit -m "Release Catima <VERSION>"`
7. Build the new .apks: `KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh`
8. Upload `app/build/outputs/apk/gplay/release/app-gplay-release.apk` to Google Play Open Testing
9. Push the version update commit: `git push`
10. Create a new release on GitHub and attach the `app/build/outputs/apk/foss/release/app-foss-release.apk` and `SHA256SUMS` files
11. When pushing the release to Google Play Production, update the metadata there: `bundle exec fastlane supply --version_code <VERSION_CODE>`
