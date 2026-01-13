# Card fields

These are the fields available in a card.

An [export](./EXPORT_FORMAT.md) always contains all values. A [card sharing URL](./CARD_SHARING_URL_FORMAT.md) only contains some of the values.

| Key             | In sharing URL | Valid values | Explanation |
| --------------- | -------------- | ------------ | ----------- |
| store           | Required       | Any string | Name of the store this card belongs to |
| note            | Required       | Any string | An optional note for the end-user |
| validfrom       | Optional       | Any UNIX timestamp | When the loyalty card starts being valid |
| expiry          | Optional       | Any UNIX timestamp | When the loyalty card expires |
| balance         | Optional       | Any string value accepted by Java's BigDecimal constructor | The balance available in the loyalty card |
| balancetype     | Optional       | Any valid ISO 4217 value or unset for "Points" | The balance currency (USD, EUR, etc.) or "points" |
| cardid          | Required       | Any string | The loyalty card ID |
| barcodeid       | Optional       | Any string | The value of the loyalty card barcode, if different from the loyalty card ID |
| barcodetype     | Optional       | AZTEC, CODABAR, CODE_39, CODE_93, CODE_128, DATA_MATRIX, EAN_8, EAN_13, ITF, MAXICODE, PDF_417, QR_CODE, RSS_14, RSS_EXPANDED, UPC_A, UPC_E | The type of loyalty card barcode used |
| barcodeencoding | Optional       | Any [StandardCharset](https://developer.android.com/reference/java/nio/charset/StandardCharsets)'s `.name()` output. Only `ISO-8859-1` and `UTF-8` are user selectable | The encoding used to render the loyalty card barcode |
| headercolor     | Optional       | A valid Android color value (https://developer.android.com/reference/android/graphics/Color) | The color to use in the header and card background |
| starstatus      | Ignored        | 0,1 | If the card is "starred" |
| lastused        | Ignored        | Any UNIX timestamp | When the loyalty card was last opened |
| archive         | Ignored        | 0,1 | If the card should be marked as archived |
