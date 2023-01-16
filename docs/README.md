# Catima

Copylefted libre software (GPLv3+) card management app.

[![GitHub Version](https://img.shields.io/github/v/release/TheLastProject/Catima.svg?logo=github&label=GitHub)](https://github.com/TheLastProject/Catima/releases)
[![F-Droid Version](https://img.shields.io/f-droid/v/me.hackerchick.catima.svg?logo=f-droid&label=F-Droid)](https://f-droid.org/packages/me.hackerchick.catima/)
[![IzzyOnDroid Version](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/me.hackerchick.catima)](https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.catima)
[![Google Play Store Version](https://img.shields.io/endpoint?color=blue&logo=google-play&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dme.hackerchick.catima%26l%3DGoogle%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=me.hackerchick.catima)

![Android CI](https://github.com/TheLastProject/Catima/workflows/Android%20CI/badge.svg)
[![Translation status](https://hosted.weblate.org/widgets/catima/-/svg-badge.svg)](https://hosted.weblate.org/engage/catima/)

<a href="https://f-droid.org/repository/browse/?fdid=me.hackerchick.catima" target="_blank">
<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.catima" target="_blank">
<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="90"/></a>

<a href="https://play.google.com/store/apps/details?id=me.hackerchick.catima" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>

![Logo](https://github.com/TheLastProject/Catima/raw/main/app/src/main/ic_launcher-playstore.png)

*Logo by [Rose (TangentFoxy)](http://github.com/TangentFoxy)*

Stores your store loyalty and membership cards on your device, instead of having to carry them around.

Supported barcodes:

- AZTEC
- CODABAR
- CODE_39
- CODE_93
- CODE_128
- DATA_MATRIX
- EAN_8
- EAN_13
- ITF
- PDF_417
- QR_CODE
- UPC_A
- UPC_E

# Screenshots

[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png)

# Moving data from other apps

Within the app you can import cards and codes from files, Catima, FidMe, Loyalty Card Keychain, Voucher Vault, and Stocard.
For FidMe you need to select the barcode type for each entry afterwards.

# Building

Building can either be done through Android Studio (not reproducible!) or the build.sh script in this repository (reproducibly with Java 11, same way F-Droid builds it). This script can also sign the build.

Build without signing:
```
./build.sh
```

Build with signing:
```
KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh
```

# Translating

[![Translation status](https://hosted.weblate.org/widgets/catima/-/open-graph.png)](https://hosted.weblate.org/engage/catima/)

Help translate the app to your language from [our Hosted Weblate page](https://hosted.weblate.org/projects/catima/).

# Developer Note

This app is based on the great (but no longer developed) [Loyalty Card Keychain](https://github.com/brarcher/loyalty-card-locker) by [Branden Archer](https://github.com/brarcher).
