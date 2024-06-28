# Catima

Aplicativo de gerenciamento de cartões com software livre e copyleft (GPLv3+).

[![Versão GitHub](https://img.shields.io/github/v/release/TheLastProject/Catima.svg?logo=github&label=GitHub)](https://github.com/TheLastProject/Catima/releases)
[![Versão IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/me.hackerchick.catima)](https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.catima)
[![Versão Google Play Store](https://img.shields.io/endpoint?color=blue&logo=google-play&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dme.hackerchick.catima%26l%3DGoogle%2520Play%26m%3D%24version)](https://play.google.com/store/apps/details?id=me.hackerchick.catima)

![Android CI](https://github.com/TheLastProject/Catima/workflows/Android%20CI/badge.svg)
[![Status da tradução](https://hosted.weblate.org/widgets/catima/-/svg-badge.svg)](https://hosted.weblate.org/engage/catima/)

[![Matrix](https://img.shields.io/matrix/catima%3Amatrix.org)](https://matrix.to/#/%23catima:matrix.org)

<a href="https://apt.izzysoft.de/fdroid/index/apk/me.hackerchick.catima" target="_blank">
<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Baixe-o no IzzyOnDroid" height="90"/></a>

<a href="https://play.google.com/store/apps/details?id=me.hackerchick.catima" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Baixe-o no Google Play" height="90"/></a>

[![Gráfico de destaque](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/featureGraphic.png)](https://catima.app/)

*Logotipo desenvolvido por [Rose (TangentFoxy)](https://github.com/TangentFoxy), gráfico de destaque por [Ziad OUALHADJ](https://github.com/ziadOUA)*

Armazena os cartões de fidelidade e de associação de sua loja em seu dispositivo, em vez de precisar carregá-los.

Códigos de barras compatíveis:

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

# Capturas de tela

[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-01.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-02.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-03.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-04.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-05.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-06.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-07.png)
[<img src="https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png" width=250>](https://github.com/TheLastProject/Catima/raw/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-08.png)

# Mover dados de outros aplicativos

No aplicativo, você pode importar cartões e códigos de arquivos, como Catima, FidMe, Loyalty Card Keychain, Voucher Vault e Stocard.
Para o FidMe, você precisa selecionar o tipo de código de barras para cada entrada posteriormente.

# Compilações

A compilação pode ser feita através do Android Studio (não reproduzível!) ou do script `build.sh` neste repositório (reproduzível com OpenJDK 17). Este script também pode assinar a compilação.

Compilar sem assinatura:
```
./build.sh
```

Compilar com assinatura:
```
KEYSTORE=/path/to/keystore KEYSTORE_ALIAS=catima ./build.sh
```

# Tradução

[![Status da tradução](https://hosted.weblate.org/widgets/catima/-/open-graph.png)](https://hosted.weblate.org/engage/catima/)

Ajude a traduzir o aplicativo para seu idioma na [nossa página Hosted Weblate](https://hosted.weblate.org/projects/catima/).

#  Nota do Desenvolvedor

Este aplicativo é baseado no excelente (mas não mais desenvolvido) [Loyalty Card Keychain](https://github.com/brarcher/loyalty-card-locker) por [Branden Archer](https://github.com/brarcher).
