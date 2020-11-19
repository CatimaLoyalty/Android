# Catima
![Android CI](https://github.com/TheLastProject/Catima/workflows/Android%20CI/badge.svg)
[![Translation status](https://hosted.weblate.org/widgets/catima/-/svg-badge.svg)](https://hosted.weblate.org/engage/catima/)

<a href="https://f-droid.org/repository/browse/?fdid=me.hackerchick.catima" target="_blank">
<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=me.hackerchick.catima" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.catima" target="_blank">
<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="90"/></a>

![Logo](https://github.com/TheLastProject/Catima/raw/master/app/src/main/ic_launcher-playstore.png)

*Logo by [Rose (TangentFoxy)](http://github.com/TangentFoxy)*

Stores all of your store loyalty cards on your phone, removing the need to carry them around.  Currently the following barcode types are supported:

- AZTEC
- CODABAR
- CODE_39
- CODE_128
- DATA_MATRIX
- EAN_8
- EAN_13
- ITF
- PDF_417
- QR_CODE
- UPC_A

# Screenshots

[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png)
[<img src="https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png" width=250>](https://github.com/TheLastProject/Catima/raw/master/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png)

# Migrating from other apps

[See our migration guides](migrate).

# Building

To build, use the gradle wrapper scripts provided in the top level directory of the project. The following will
compile the application and run all unit tests:

GNU/Linux, OSX, UNIX:
```
./gradlew build
```

Windows:
```
./gradlew.bat build
```

# Translating

Please contribute through [our Weblate page](https://hosted.weblate.org/projects/catima/).

# Thanks

This application uses the following image:
- [Save](https://thenounproject.com/term/save/716011) by [Bernar Novalyi](https://thenounproject.com/bernar.novalyi)

# Note from Developer
This application is based on the great [Loyalty Card Keychain](https://github.com/brarcher/loyalty-card-locker) by [Branden Archer](https://github.com/brarcher). This fork was created due to the original developer having stopped development of this app.
