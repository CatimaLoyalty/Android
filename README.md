# Loyalty Card Keychain
[![Build Status](https://travis-ci.org/brarcher/loyalty-card-locker.svg?branch=master)](https://travis-ci.org/brarcher/loyalty-card-locker)

<a href="https://f-droid.org/repository/browse/?fdid=protect.card_locker" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="90"/></a>
<a href="https://play.google.com/store/apps/details?id=protect.card_locker" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="90"/></a>

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

If there is any interest in improving this project, kindly submit a pull request with
proposed changes.

# Screenshots

[<img src="https://user-images.githubusercontent.com/5264535/27416124-79b09162-56d9-11e7-967b-8923177dc228.png" width=250>](https://user-images.githubusercontent.com/5264535/27416124-79b09162-56d9-11e7-967b-8923177dc228.png)
[<img src="https://user-images.githubusercontent.com/5264535/27416127-7baea332-56d9-11e7-8a10-5be90bb02225.png" width=250>](https://user-images.githubusercontent.com/5264535/27416127-7baea332-56d9-11e7-8a10-5be90bb02225.png)
[<img src="https://user-images.githubusercontent.com/5264535/27416128-7d50f7b2-56d9-11e7-9833-1dd962f9cf66.png" width=250>](https://user-images.githubusercontent.com/5264535/27416128-7d50f7b2-56d9-11e7-9833-1dd962f9cf66.png)

[<img src="https://user-images.githubusercontent.com/5264535/27416132-7ea6272c-56d9-11e7-9a52-d73424bf902c.png" width=250>](https://user-images.githubusercontent.com/5264535/27416132-7ea6272c-56d9-11e7-9a52-d73424bf902c.png)
[<img src="https://user-images.githubusercontent.com/5264535/27416137-800aee90-56d9-11e7-9cc9-2a7dc63bb4fb.png" width=250>](https://user-images.githubusercontent.com/5264535/27416137-800aee90-56d9-11e7-9cc9-2a7dc63bb4fb.png)
[<img src="https://user-images.githubusercontent.com/5264535/27416140-82d8211a-56d9-11e7-8031-c71d3077bdc6.png" width=250>](https://user-images.githubusercontent.com/5264535/27416140-82d8211a-56d9-11e7-8031-c71d3077bdc6.png)

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

# Thanks

This application uses the following image:
- [Save](https://thenounproject.com/term/save/716011) by [Bernar Novalyi](https://thenounproject.com/bernar.novalyi)
