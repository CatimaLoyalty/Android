## v0.28 (2020-03-09)

Changes:
- Fix barcode centering when exiting full screen ([#351](https://github.com/brarcher/loyalty-card-locker/pull/351))
- Allow backup export location to be selected ([#352](https://github.com/brarcher/loyalty-card-locker/pull/352))
- Update translations ([#357](https://github.com/brarcher/loyalty-card-locker/pull/357)) & ([#362](https://github.com/brarcher/loyalty-card-locker/pull/362))

## v0.27 (2020-01-26)

Changes:
- Tapping on a barcode now moves it to the top of the screen ([#348](https://github.com/brarcher/loyalty-card-locker/pull/348))
- Add white space around barcodes to improve scanning in dark mode ([#328](https://github.com/brarcher/loyalty-card-locker/issues/328))
- Fix swapped import buttons. ([#346](https://github.com/brarcher/loyalty-card-locker/pull/346))

## v0.26.1 (2020-01-09)

Changes:
- Fix issue with sharing cards without background color ([#343](https://github.com/brarcher/loyalty-card-locker/pull/343))

## v0.26 (2020-01-05)

Changes:
- Add ability to search for a card ([#320](https://github.com/brarcher/loyalty-card-locker/pull/320))
- Add ability to share and receive loyalty cards ([#321](https://github.com/brarcher/loyalty-card-locker/pull/321))
- Dark mode support ([#322](https://github.com/brarcher/loyalty-card-locker/pull/322))
- Loyalty cards can now be barcodeless (e.g. not have a barcode) ([#324](https://github.com/brarcher/loyalty-card-locker/pull/324))
- Notes can span multiple lines ([#326](https://github.com/brarcher/loyalty-card-locker/pull/326))
- Improvements with the sizing of notes ([#319](https://github.com/brarcher/loyalty-card-locker/pull/319))
- Improve notification and app icon visibility ([#330](https://github.com/brarcher/loyalty-card-locker/pull/330))
- Update target SDK to Android 10
- Improve the following translations:
  * German
  * Italian
  * Dutch
  * Polish
  * Russian

## v0.25.4 (2019-10-04)

Changes
- Enable app backups
- Update French and Slovenian translations

## v0.25.3 (2019-03-02)

Changes
- Update Russian translations

## v0.25.2 (2019-01-05)
  
Changes
- Update and add translations

## v0.25.1 (2018-10-14)

Changes:
- Fix creating new card by manually entering barcode (https://github.com/brarcher/loyalty-card-locker/issues/272)

## v0.25 (2018-10-07)

Changes:
- Sort card list case insensitive (https://github.com/brarcher/loyalty-card-locker/pull/266)
- Add setting to lock orientation for all cards (https://github.com/brarcher/loyalty-card-locker/pull/269)

## v0.24 (2018-07-31)

Changes:
- Add a setting to control screen brightness when displaying a barcode (https://github.com/brarcher/loyalty-card-locker/pull/259)
- Add Greek translations (https://github.com/brarcher/loyalty-card-locker/pull/252)
- Add Slovenian translations (https://github.com/brarcher/loyalty-card-locker/pull/260)
- Update translations (https://github.com/brarcher/loyalty-card-locker/pull/260, https://github.com/brarcher/loyalty-card-locker/pull/254)

## v0.23.4 (2018-05-12)

Changes: 
- Fix Spanish translations (https://github.com/brarcher/loyalty-card-locker/pull/244)
- Update translations (https://github.com/brarcher/loyalty-card-locker/pull/244)

## v0.23.3 (2018-05-05)

Changes:
- Added translations
  * Polish (https://github.com/brarcher/loyalty-card-locker/pull/232)
  * Spanish (https://github.com/brarcher/loyalty-card-locker/pull/232)
  * Slovak (https://github.com/brarcher/loyalty-card-locker/pull/232)
- Updated translations (https://github.com/brarcher/loyalty-card-locker/pull/239)

## v0.23.2 (2018-03-11)

Changes:
- Reduce min SDK from 17 to 15. (https://github.com/brarcher/loyalty-card-locker/pull/226)
- Remove usage of legacy apache library, used only in unit tests but no longer needed. (https://github.com/brarcher/loyalty-card-locker/pull/225)

## v0.23.1 (2018-03-07)

Changes:
- Prevent crash when rendering a barcode exhausts the application's memory. (https://github.com/brarcher/loyalty-card-locker/pull/219)

## v0.23 (2018-02-28)

Changes:
- Reduce space in header when viewing a card. (https://github.com/brarcher/loyalty-card-locker/pull/213)
- Disable beep when scanning a barcode. (https://github.com/brarcher/loyalty-card-locker/pull/216)

## v0.22 (2018-02-19)

Changes:
- Update translations. (https://github.com/brarcher/loyalty-card-locker/pull/208)
- Barcode rendering updates: (https://github.com/brarcher/loyalty-card-locker/pull/209)
  * Reload card view activity when screen is rotated, so barcode image is correct size.
  * Render 1D barcodes in a larger space, allowing them to better fill the screen. 

## v0.21 (2018-02-17)

Changes
- Add quiet space at the start/end of barcodes. (https://github.com/brarcher/loyalty-card-locker/pull/200)
- Add options to configure the colors used for the store name font and background. (https://github.com/brarcher/loyalty-card-locker/pull/203)
- Add options to adjust font sizes on the card listing page and single card page. (https://github.com/brarcher/loyalty-card-locker/pull/204)

## v0.20 (2018-02-10)

Changes:
- Changes to Card view to display the note, allow the card ID to take multiple lines, and show the store name. (https://github.com/brarcher/loyalty-card-locker/pull/197)

## v0.19 (2018-02-01)

Changes:
- Improved layout for card list. (https://github.com/brarcher/loyalty-card-locker/pull/188)
- Improved layout when viewing a card. (https://github.com/brarcher/loyalty-card-locker/pull/190)

## v0.18.1 (2018-01-24)

Changes:
- Workaround crash during install on some Android versions (likely Android 5 and below). (https://github.com/brarcher/loyalty-card-locker/pull/184)

## v0.18 (2018-01-19)

Changes:
- Fix crash when importing certain types of corrupted CSV files. (https://github.com/brarcher/loyalty-card-locker/pull/177)
- Fix importing backups directly from the file system. (https://github.com/brarcher/loyalty-card-locker/pull/180)
- Fix importing backups from certain types of content providers. (https://github.com/brarcher/loyalty-card-locker/pull/179)

## v0.17 (2018-01-11)

Changes:
- Fix issue on Android SDK 24+ where using the file chooser import option would cause a crash. (https://github.com/brarcher/loyalty-card-locker/pull/170)
- New icon and color scheme. (https://github.com/brarcher/loyalty-card-locker/pull/171)

## v0.16 (2017-11-29)

Changes:
- Add support for adding loyalty card shortcuts from the launcher/homescreen. (https://github.com/brarcher/loyalty-card-locker/pull/161)
- Remove support for adding loyalty card shortcuts from the app itself. This removes the need for the shortcut permission. (https://github.com/brarcher/loyalty-card-locker/pull/163)

## v0.15 (2017-11-25)

Changes:
- Add support for adding shortcuts to home screen when adding or editing a card. (https://github.com/brarcher/loyalty-card-locker/pull/155)
- Remove widget, as it was a poor substitute for shortcuts. (https://github.com/brarcher/loyalty-card-locker/pull/155)
- Fix exporting backups on Android 7+. (https://github.com/brarcher/loyalty-card-locker/pull/153)
- Report more accurate mime type when exporting backup data. (https://github.com/brarcher/loyalty-card-locker/pull/156)
- Fix bug where a card could not be edited. (https://github.com/brarcher/loyalty-card-locker/pull/155)

## v0.14 (2017-10-26)

Changes:
- Add support for app shortcuts (Android 7.1+), where the most recently used cards will appear as shortcuts. (https://github.com/brarcher/loyalty-card-locker/pull/145)
- Add a widget which works like a pinned app shortcut, to support devices which run below Android 7.1. (https://github.com/brarcher/loyalty-card-locker/pull/142)

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
