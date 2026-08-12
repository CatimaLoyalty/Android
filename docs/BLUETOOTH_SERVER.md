# Bluetooth server (for WearOS support and other Bluetooth devices)

Catima implements a [Rfcomm Bluetooth server](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#listenUsingRfcommWithServiceRecord(java.lang.String,%20java.util.UUID)) to allow Bluetooth devices to interact with the cards database. Right now, this only provides read-only functionality.

The UUID of the Rfcomm Bluetooth server is `e5b4f020-3a7e-4b6d-9f2c-1a8c5d3e7f90`. You should bind to this using [createRfcommSocketToServiceRecord](https://developer.android.com/reference/android/bluetooth/BluetoothDevice#createRfcommSocketToServiceRecord(java.util.UUID)) on Android or, if not Android, whatever is the alternative for your platform. Insecure Rfcomm sockets are not supported.

Since running the Bluetooth server requires extra permissions (Bluetooth and Notifications) and runs a constant background process it is disabled by default.

For security, Catima keeps a list of known MAC addresses and tokens and will only allow devices explicitly allowed by the user to interact with the Bluetooth server. This prevents every paired Bluetooth device from being able to access cards without user authentication.

The Bluetooth server can be controlled with the setting "Sync with Wear OS" under "Smartwatch support". It is disabled by default.

The Bluetooth server is currently only used by the Catima WearOS companion app. If your app/device uses it, please let us know, so we can update the documentation and contact your project in case of planned breaking changes.

## API versions

For each API version, we have a major and a minor version. The rules for versioning are as follows:
1. A new minor version may add fields or endpoints, but must never modify or remove existing fields or endpoints.
2. A new minor version must not change any authentication systems compared to the previous minor version.
3. A device cannot request Catima to use a specific minor version, it will always return data as per the most recently supported minor version.

The main driver of compatibility is the major version.

Due to this system, a device only supporting API version 1.0 can talk to Catima supporting API version 1.1 with no issues, as long as it ignores fields it doesn't know. On the other side, a device wanting to use API version 1.1 with Catima on API version 1.0 has to accept certain fields may not be available. Which minor version introduces which additional fields and endpoints is documented in this file.

### Unversioned

#### /VERSIONS

Return type: JSON.

Returns all supported major versions and the most recent supported minor version.

Example request:
```
/VERSIONS
```

Example return value:
```
["1.0"]
```

In this example case, the phone supports only API version 1. Of API version 1, the most recent minor version supported is 0.

### V1

All V1 endpoints are authenticated and must be communicated with by sending 2 lines over an RfConn socket. First the full command, then a device-specific token (recommended: a base64-encoded version of a 32 characters long SecureRandom-generated string).

The device-specific token must be generated once on the companion device and never changed.

For example, to call the first page of `/V1/CARDS_REQUEST_PAGE/` you should send:
```
/V1/CARDS_REQUEST_PAGE/0
TOKEN: your_base64_encoded_token_here
```

#### /V1/CARDS_REQUEST_PAGE/{id:int}

Return type: JSON.

Request a single page of cards with page and total page details. The page ID is 0-indexed, making 0 the first page. Currently, a page is 10 cards but this may change in the future.

Each card contains the following fields:

| Column            | Type              | Description                | Introduced in minor version |
|-------------------|-------------------|----------------------------| --------------------------- |
| `id`              | `int`             | Unique card ID | 0 |
| `store`           | `String`          | Card name | 0 |
| `cardId`          | `String`          | Card ID. | 0 |
| `barcodeId`       | `Nullable String` | Barcode value. If empty, it's the same as the card ID. | 0 |
| `barcodeType`     | `Nullable String` | The barcode type name, matching [com.google.zxing.BarcodeFormat](https://zxing.github.io/zxing/apidocs/com/google/zxing/BarcodeFormat.html). When null, there is no barcode for this card. | 0 |
| `headerColor`     | `int`             | Header color, matching [Android color int](https://developer.android.com/reference/android/graphics/Color). | 0 |

Example request:
```
/V1/CARDS_REQUEST_PAGE/0
TOKEN: your_base64_encoded_token_here
```

Example return value:
```
{
    "page": 0,
    "totalPages": 1,
    "cards": [
        {
            "id": 1,
            "store": "Bookshop",
            "cardId": "123456",
            "barcodeId": null,
            "barcodeType": "QR_CODE",
            "headerColor": -1
        }
    ]
}
```
