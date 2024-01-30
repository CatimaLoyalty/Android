**This documentation is for maintainers. If you're a user, please ignore it.**

# When releasing, do the following:
1. Press "Commit" and "Push" on Weblate to ensure all translations are up to date
2. Merge Weblate pull request
3. Update `CHANGELOG.md` with the new version name and the release date
4. Update `app/build.gradle.kts` with the new `versionCode` and `versionName`
5. Build a new .apk: `KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh`
6. Upload the APK to Google Play Open Testing
7. Push the version update: `git add CHANGELOG.md app/build.gradle.kts && git commit -m "Release Catima <VERSION>" && git push`
8. Create a new release on GitHub and attach the `app-release.apk` and `SHA256SUMS` files
