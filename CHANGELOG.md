## v0.13 (2017-07-25)

Changes:
- Add screen rotation lock menu option when displaying a card. If locked, the screen will transition to its "natural" orientation and further screen rotation will be blocked. (https://github.com/brarcher/loyalty-card-locker/pull/128)
- If a card is selected from the main screen but cannot be loaded, the application fails gracefully and posts a message. (https://github.com/brarcher/loyalty-card-locker/pull/132)
- Fix case where layout IDs for intro wizard could not be found. (https://github.com/brarcher/loyalty-card-locker/pull/128)

## v0.12 (2017-07-16)

Changes:

- A change in v0.11 reduced the memory usage of barcode drawing, but affected the barcode dimensions. This is now changed to maintain the barcode dimensions while reducing memory usage. (https://github.com/brarcher/loyalty-card-locker/pull/126)
- Update German and French translations. (https://github.com/brarcher/loyalty-card-locker/pull/122, https://github.com/brarcher/loyalty-card-locker/pull/124, https://github.com/brarcher/loyalty-card-locker/pull/125)

## v0.11.1 (2017-06-29)

Changes:
- Prevent a crash when rotation the screen in the first run intro wizard.

## v0.11 (2017-06-26)

Improvements:
- When editing a card ID, pre-populate the existing ID to start. (https://github.com/brarcher/loyalty-card-locker/pull/94)
- Limit the width of generated barcodes to reduce memory usage and out of memory errors. (https://github.com/brarcher/loyalty-card-locker/pull/103)
- When editing a card, change the "Enter Card" button to say "Edit Card" if a card ID already exists. (https://github.com/brarcher/loyalty-card-locker/pull/104)
- Change the color scheme to be softer and compatible with the app icon, and change the layout when viewing a card to be cleaner. (https://github.com/brarcher/loyalty-card-locker/pull/107)
- Add an intro wizard which launches on the app's first launch. (https://github.com/brarcher/loyalty-card-locker/pull/108)

## v0.10 (2017-02-12)

Improvements:
- Changed the default import/export filename. (https://github.com/brarcher/loyalty-card-locker/pull/84)
- Correct string on the import/export page. (https://github.com/brarcher/loyalty-card-locker/pull/87)
- Improve layout of card view page. The text should be easier to read, and is selectable with a long click. (https://github.com/brarcher/loyalty-card-locker/pull/91)

## v0.9 (2017-01-17)

The "Locker" part of the name was not intuitive. To help remedy this a new application icon was created by betsythefc which better represents the purpose of the application: to store loyalty cards which use barcodes. Along with this new icon the name of the application has been changed to "Loyalty Card Keychain".

Additional features/improvements:

- Importing/Exporting cards was changed to be more flexible. (https://github.com/brarcher/loyalty-card-locker/pull/76)
- Translations for Lithuanian added. (https://github.com/brarcher/loyalty-card-locker/pull/62)
- Translations for French added. (https://github.com/brarcher/loyalty-card-locker/pull/80)

## v0.8 (2016-11-22)

New features/improvements:
- Screen brightness increased to its maximum when displaying a card, to help barcode scanners successfully capture the barcode. (https://github.com/brarcher/loyalty-card-locker/pull/54)
- Add a delete confirmation when deleting a card. (https://github.com/brarcher/loyalty-card-locker/pull/55)
- Add translations for German (https://github.com/brarcher/loyalty-card-locker/pull/57) and Czech (https://github.com/brarcher/loyalty-card-locker/pull/58).
- Clarification change for Italian translation. (https://github.com/brarcher/loyalty-card-locker/pull/66)

## v0.7 (2016-07-14)

New features/improvements:
- Long-click of a card brings up option to copy card ID to the clipboard. (https://github.com/brarcher/loyalty-card-locker/issues/49)

Bug fixes:
- Back button on Input/Export view now works, moving user to main view

## v0.6 (2016-05-23)

New features/improvements:
- Allow user to enter barcode manually. If a user elects to enter a barcode manually, a list of all valid and supported barcode images is displayed. The user then may select the barcode image which matches what the user wants. https://github.com/brarcher/loyalty-card-locker/issues/33, https://github.com/brarcher/loyalty-card-locker/pull/44 

Bug fixes:
- Resolve issue where some displayed barcodes were blurry. (https://github.com/brarcher/loyalty-card-locker/issues/37)

## v0.5 (2016-05-16)

New features/improvements:
- An about dialog can be opened from the main screen, which gives details about the application and project on GitHub (https://github.com/brarcher/loyalty-card-locker/issues/19)
- Allow loyalty card information to be imported from/exported to a CSV file in external storage (https://github.com/brarcher/loyalty-card-locker/issues/36 https://github.com/brarcher/loyalty-card-locker/issues/20)

## v0.4 (2016-04-09)

New features/improvements:
- Dutch translation
- Allow name field to be editable after adding loyalty card
- Add an optional note field

Bug fixes:

- Resolve all issues identified by FindBugs and require all FindBugs issues be resolved prior to pull request acceptance

## v0.3 (2016-02-11)

- Now officially supports the following list of 1D and 2D barcodes:
  * AZTEC
  * CODABAR
  * CODE_39
  * CODE_128
  * DATA_MATRIX
  * EAN_8
  * EAN_13
  * ITF
  * PDF_417
  * QR_CODE
 * UPC_A
- Generated barcodes are larger, easier to scan from a scanning device

## v0.2 (2016-02-07)

- Italian translations
- Support for all 1D barcode types. (Originally only product 1D barcodes were supported)
- Add required camera permission, which was initially missing.


## v0.1 (2016-01-30)

- Ability to create/edit/delete loyalty cards
- Capture barcode of loyalty card using a camera
- Display captured barcode, for scanning at a store
