# Card sharing URL format

In the interest of interoperability, this page documents the URLs generated when sharing a card.

URLs have the following parts:
## hostname and path

Hostname and path must be any of the following combinations:

| Hostname                 | Path                       | Description                                                            |
| ------------------------ | -------------------------- | ---------------------------------------------------------------------- |
| catima.app               | /share                     | Default since 2021-07-11                                               |
| thelastproject.github.io | /Catima/share              | Created when forking away from Loyalty Card Keychain                   |
| brarcher.github.io       | /loyalty-card-locker/share | For compatibility with https://github.com/brarcher/loyalty-card-locker |

## parameters
The list of supported fields are listed in [Card fields](./CARD_FIELDS.md).

## Catima 2.0

As of Catima 2.0, Share URLs are in the following format for increased privacy (no leaking card info to the server):

```
https://[hostname]/[path]#[parameters]
```

Parameters are written as such before being url-encoded (so yes, the values are url-encoded twice)
```
key=urlencoded_value&key2=urlencoded_value2
```

An example share URL is as follows:
```
https://catima.app/share#store%3DGrocery%2BStore%26note%3DQuite%2Bnecessary%26balance%3D150%26cardid%3Ddhd%26barcodetype%3DAZTEC%26headercolor%3D-9977996
```

## Before 2.0

Share URLs are in the following format:
```
https://[hostname]/[path]?[parameters]
```

An example share URL is as follows:
```
https://catima.app/share?store=Grocery%20Store&note=Quite%20necessary&balance=150&cardid=dhd&barcodetype=AZTEC&headercolor=-9977996
```

These are still imported for backwards compatibility, but no longer generated.
