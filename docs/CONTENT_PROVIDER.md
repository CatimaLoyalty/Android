# Content Provider (for Gadgetbridge and other local apps)

Catima implements a [content provider](https://developer.android.com/guide/topics/providers/content-providers) to allow for external apps to interact with the cards database. Right now, this only provides read-only functionality.

Since runtime permissions are only available since Android 6.0 (API level 23), the content provider is disabled for older android versions in order to prevent unwanted access to the data.

The content provider can be controlled with the setting "Sync with Gadgetbridge" under "Smartwatch support". It is enabled by default.

The content provider is currently only used by Gadgetbridge. If your app uses it, please let us know, so we can update the documentation and contact your project in case of planned breaking changes.

## Package Names

There are 3 release channels, with 2 possible package names:

| Release Channel | Package Name                |
|-----------------|-----------------------------|
| Google Play     | me.hackerchick.catima       |
| F-Droid         | me.hackerchick.catima       |
| Debug Build     | me.hackerchick.catima.debug |

The package names are required for the authority and permissions, as described below.

## Authority

The authority for this content provider: `<package_name>.contentprovider.cards`

## Permissions

The content provider requires the following permissions:

- `<package_name>.READ_CARDS` - in order to access any of the URIs.

## URIs

### /version

Returns a single record with the current API version for the content provider.

A major version change implies breaking changes (eg. columns being renamed or removed).

| Column  | Type  | Description       | Value |
|---------|-------|-------------------|-------|
| `major` | `int` | The major version | `1`   |
| `minor` | `int` | The minor version | `1`   |

### /cards

| Column            | Type              | Description                |
|-------------------|-------------------|----------------------------|
| `_id`             | `int`             | Unique card ID |
| `store`           | `String`          | Card name |
| `validfrom`       | `long`            | Timestamp from which the card is valid (unix epoch millis). |
| `expiry`          | `long`            | Expiration timestamp (unix epoch millis). |
| `balance`         | `String`          | Current balance, as a string-formatted big decimal. |
| `balancetype`     | `Nullable String` | Balance currency code, ISO 4217. When null, it's a generic "Points" indicator. |
| `note`            | `String`          | A note. |
| `headercolor`     | `int`             | Header color, matching [Android color int](https://developer.android.com/reference/android/graphics/Color). |
| `cardid`          | `String`          | Card ID. |
| `barcodeid`       | `Nullable String` | Barcode value. If null, it's the same as the card ID. |
| `barcodetype`     | `Nullable String` | The barcode type name, matching [com.google.zxing.BarcodeFormat](https://zxing.github.io/zxing/apidocs/com/google/zxing/BarcodeFormat.html). When null, there is no barcode for this card. |
| `barcodeencoding` | `String`          | The barcode encoding used to render the barcode, matching a [StandardCharset](https://developer.android.com/reference/java/nio/charset/StandardCharsets)'s `.name()` output. Only `ISO-8859-1` and `UTF-8` are user selectable. |
| `starstatus`      | `int`             | 1 if starred, 0 if not |
| `lastused`        | `long`            | Timestamp of last card usage (unix epoch millis). |
| `archive`         | `int`             | 1 if archived, 0 if not |

### /groups

| Column    | Type     | Description                |
|-----------|----------|----------------------------|
| `_id`     | `String` | Group name (unique)        |
| `orderId` | `int`    | Group order, in the UI     |

### /card_groups

Returns the mapping between cards and groups, by ID.

- A card can be in 0 or more groups.
- A group can contain 0 or more cards.

| Column    | Type     | Description  |
|-----------|----------|--------------|
| `cardId`  | `String` | Card ID      |
| `groupId` | `String` | Group ID     |
