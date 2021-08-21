# Changelog

## Unreleased

Changes:

- Add theming colour support (thanks, Subhashish Anand!)

## v2.3.0 (2021-08-19)

Changes:

- Fix images not imported from backup
- Option to override language

## v2.2.3 (2021-08-13)

Changes:

- Fix widget creating different-looking shortcut than app shortcuts
- Replace default Android black screen with splash screen

## v2.2.2 (2021-08-08)

Changes:

- Fix crash on rotation in loyalty card edit activity

## v2.2.1 (2021-08-07)

Changes:

- Improve Stocard importer
- Fix importing Catima export with multiline note
- Scale card title in acceptable range
- Animation improvements

## v2.2.0 (2021-08-02)

Changes:

- Make links in notes clickable
- Pre-select group the user is currently in when creating a new card
- Comma-separate group names in loyalty card view
- Fix maximize button appearing on no barcode

## v2.1.0 (2021-08-01)

Changes:

- Fix selected colour in colour changing dialog
- Support for deleting multiple cards at once
- Fix possible ArithmeticException when resizing image
- Fix fullscreen is closed when rotating device

## v2.0.4 (2021-07-27)

Changes:

- Fix shortcut creation
- Generate card-specific shortcut icon
- Fix ability to change loyalty card colour

## v2.0.3 (2021-07-25)

Changes:

- Fix loading photos when editing existing card

## v2.0.2 (2021-07-25)

Changes:

- Fix inability to configure photos in new loyalty card

## v2.0.1 (2021-07-21)

Changes:

- Several minor translation and UI fixes
- Fix crash in import/sharing loyalty card on Android 6

## v2.0 (2021-07-14)

Breaking changes:
- The backup format changed, see https://github.com/TheLastProject/Catima/wiki/Export-format
- The URL sharing format changed, see https://github.com/TheLastProject/Catima/wiki/Card-sharing-URL-format

Changes:

- Make it possible to enable or disable the flashlight while scanning
- Add UPC-E support
- Support adding a front and back photo to each card
- Support importing password-protected zip files
- Support importing from Stocard (Beta)
- Fix useless whitespace in notes from Fidme import
- Support new Voucher Vault export format
- Fix Floating Action Buttons being behind other UI elements on Android 4
- Fix loyalty card viewer appbar top margin

## v1.14.1 (2021-06-14)

Changes:

- Add missing barcode ID to export
- Don't show update barcode dialog if value is the same as card ID
- Add Finnish translation

## v1.14 (2021-06-07)

Changes:

- Support new PDF417 export from Voucher Vault
- Support copying multiple barcodes at once
- Support sharing multiple loyalty cards at once
- Ask to update barcode value if card ID changes

## v1.13 (2021-04-10)

Changes:

- Add option to set a separate barcode value from card ID
- Simplify font sizing configuration
- Several small UI fixes
- Use letter icon for shortcuts too
- Always show all barcode types in manual entry
- Remove privacy policy first start dialog

## v1.12 (2021-03-30)

Changes:

- Support importing [Fidme](https://play.google.com/store/apps/details?id=fr.snapp.fidme) exports
- Allow importing a card from a picture stored in the user's Android gallery
- Fix multiline note cutoff
- Change "Thank you" text on privacy dialog to "Accept" because Huawei is overly pedantic

## v1.11 (2021-03-21)

Changes:

- Add privacy policy dialog on first start (required by Huawei)

## v1.10 (2021-03-07)

Changes:

- Support importing [Voucher Vault](https://github.com/tim-smart/vouchervault/) exports
- Option to keep the screen on while viewing a loyalty card
- Option to suspend the lock screen while viewing a loyalty card

## v1.9.2 (2021-02-24)

Changes:

- Fix parsing balance for countries using space as separator

## v1.9.1 (2021-02-23)

Changes:

- Improve balance parsing logic
- Fix currency decimal display on main screen

## v1.9 (2021-02-22)

Changes:

- Add balance support
- Reorganize barcode tab of edit view

## v1.8.1 (2021-02-12)

Changes:

- Fix Crash on versions before Android 7

## v1.8 (2021-01-28)

Changes:

- Add support for scaling the barcode when moving to top to fit even more small scanners
- Fix bottom sheet jumping after switching to fullscreen
- Make header in loyalty card view small in landscape mode
- Fix cards not staying in group when group gets renamed

## v1.7.1 (2021-01-18)

Changes:

- Fix crash on switching to barcode tab in edit view if there is no barcode

## v1.7.0 (2021-01-18)

Changes:

- Separate edit UI in tabs to make it feel more spacious
- Add expiry field support

## v1.6.2 (2021-01-04)

Changes:

- Fix edit button or more info bottom sheet drawing over barcode ID

## v1.6.1 (2020-12-16)

Changes:

- Fix regression causing manual barcode entry to not be saved

## v1.6.0 (2020-12-15)

Changes:

- Automatically focus text field when creating or editing a group
- Fix blurry icons (use SVG everywhere)
- Always open camera but add manual scan button to camera view

## v1.5.1 (2020-12-03)

Changes:

- Fix bottomsheet background being transparent

## v1.5.0 (2020-12-03)

Changes:

- Improve contrast by always using white text on red buttons
- Draggable bottom sheet in loyalty card view

## v1.4.1 (2020-12-01)

Changes:

- Improved translations
- Small UI fixes

## v1.4.0 (2020-11-28)

Changes:

- Move About screen into its own activity
- Ask user if they want to use their camera or manually enter ID on add/edit card
- Make group ordering manual instead of forced alphabetically

## v1.3.0 (2020-11-22)

Changes:

- Always show all import/export options and show a toast on actual issues (improves compat with XPrivacyLua)
- Ask for confirmation when leaving edit view after making changes without saving

## v1.2.2 (2020-11-19)

Changes:

- Remember active group tab between screens and sessions

## v1.2.1 (2020-11-17)

Changes:

- Fix home screen swiping triggering during vertical swipes too

## v1.2.0 (2020-11-17)

Changes:

- Add swiping between groups on the home screen
- Fix crash with cards lacking header colour

## v1.1.0 (2020-11-11)

Changes:

- Improved edit UI
- Removed header text colour option (now automatically generated based on brightness)
- Updated translations

## v1.0.1 (2020-11-07)

Changes:

- Fix crash in search with no groups

## v1.0 (2020-11-06)

Changes:

- Added rounded edges to card icons on main overview
- Added support for grouping entries

## v0.29 (2020-10-29)

Changes:

- Rebrand to Catima
- Removed intro
- Add floating action buttons
- Fix Android 5 crash when opening About screen
- Add favourites support
- Fix disabled auto-rotate being ignored

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
  - German
  - Italian
  - Dutch
  - Polish
  - Russian

## v0.25.4 (2019-10-04)

Changes:

- Enable app backups
- Update French and Slovenian translations

## v0.25.3 (2019-03-02)

Changes:

- Update Russian translations

## v0.25.2 (2019-01-05)

Changes:

- Update and add translations

## v0.25.1 (2018-10-14)

Changes:

- Fix creating new card by manually entering barcode ([issue #272](https://github.com/brarcher/loyalty-card-locker/issues/272))

## v0.25 (2018-10-07)

Changes:

- Sort card list case insensitive ([pull #266](https://github.com/brarcher/loyalty-card-locker/pull/266))
- Add setting to lock orientation for all cards ([pull #269](https://github.com/brarcher/loyalty-card-locker/pull/269)

## v0.24 (2018-07-31)

Changes:

- Add a setting to control screen brightness when displaying a barcode ([pull #259](https://github.com/brarcher/loyalty-card-locker/pull/259))
- Add Greek translations ([pull #252](https://github.com/brarcher/loyalty-card-locker/pull/252))
- Add Slovenian translations ([pull #260](https://github.com/brarcher/loyalty-card-locker/pull/260))
- Update translations ([pull #260](https://github.com/brarcher/loyalty-card-locker/pull/260), [pull #254](https://github.com/brarcher/loyalty-card-locker/pull/254))

## v0.23.4 (2018-05-12)

Changes:

- Fix Spanish translations ([pull #244](https://github.com/brarcher/loyalty-card-locker/pull/244))
- Update translations ([pull #244](https://github.com/brarcher/loyalty-card-locker/pull/244))

## v0.23.3 (2018-05-05)

Changes:

- Added translations
  - Polish ([pull #232](https://github.com/brarcher/loyalty-card-locker/pull/232))
  - Spanish ([pull #232](https://github.com/brarcher/loyalty-card-locker/pull/232))
  - Slovak ([pull #232](https://github.com/brarcher/loyalty-card-locker/pull/232))
- Updated translations ([pull #239](https://github.com/brarcher/loyalty-card-locker/pull/239))

## v0.23.2 (2018-03-11)

Changes:

- Reduce min SDK from 17 to 15. ([pull #226](https://github.com/brarcher/loyalty-card-locker/pull/226))
- Remove usage of legacy apache library, used only in unit tests but no longer needed. ([pull #225](https://github.com/brarcher/loyalty-card-locker/pull/225))

## v0.23.1 (2018-03-07)

Changes:

- Prevent crash when rendering a barcode exhausts the application's memory. ([pull #219](https://github.com/brarcher/loyalty-card-locker/pull/219))

## v0.23 (2018-02-28)

Changes:

- Reduce space in header when viewing a card. ([pull #213](https://github.com/brarcher/loyalty-card-locker/pull/213))
- Disable beep when scanning a barcode. ([pull #216](https://github.com/brarcher/loyalty-card-locker/pull/216))

## v0.22 (2018-02-19)

Changes:

- Update translations. ([pull #208](https://github.com/brarcher/loyalty-card-locker/pull/208))
- Barcode rendering updates: ([pull #209](https://github.com/brarcher/loyalty-card-locker/pull/209))
  - Reload card view activity when screen is rotated, so barcode image is correct size.
  - Render 1D barcodes in a larger space, allowing them to better fill the screen.

## v0.21 (2018-02-17)

Changes:

- Add quiet space at the start/end of barcodes. ([pull #200](https://github.com/brarcher/loyalty-card-locker/pull/200))
- Add options to configure the colors used for the store name font and background. ([pull #203](https://github.com/brarcher/loyalty-card-locker/pull/203))
- Add options to adjust font sizes on the card listing page and single card page. ([pull #204](https://github.com/brarcher/loyalty-card-locker/pull/204))

## v0.20 (2018-02-10)

Changes:

- Changes to Card view to display the note, allow the card ID to take multiple lines, and show the store name. ([pull #197](https://github.com/brarcher/loyalty-card-locker/pull/197))

## v0.19 (2018-02-01)

Changes:

- Improved layout for card list. ([pull #188](https://github.com/brarcher/loyalty-card-locker/pull/188))
- Improved layout when viewing a card. ([pull #190](https://github.com/brarcher/loyalty-card-locker/pull/190))

## v0.18.1 (2018-01-24)

Changes:

- Workaround crash during install on some Android versions (likely Android 5 and below). ([pull #184](https://github.com/brarcher/loyalty-card-locker/pull/184))

## v0.18 (2018-01-19)

Changes:

- Fix crash when importing certain types of corrupted CSV files. ([pull #177](https://github.com/brarcher/loyalty-card-locker/pull/177))
- Fix importing backups directly from the file system. ([pull #180](https://github.com/brarcher/loyalty-card-locker/pull/180))
- Fix importing backups from certain types of content providers. ([pull #179](https://github.com/brarcher/loyalty-card-locker/pull/179))

## v0.17 (2018-01-11)

Changes:

- Fix issue on Android SDK 24+ where using the file chooser import option would cause a crash. ([pull #170](https://github.com/brarcher/loyalty-card-locker/pull/170))
- New icon and color scheme. ([pull #171](https://github.com/brarcher/loyalty-card-locker/pull/171))

## v0.16 (2017-11-29)

Changes:

- Add support for adding loyalty card shortcuts from the launcher/homescreen. ([pull #161](https://github.com/brarcher/loyalty-card-locker/pull/161))
- Remove support for adding loyalty card shortcuts from the app itself. This removes the need for the shortcut permission. ([pull #163](https://github.com/brarcher/loyalty-card-locker/pull/163))

## v0.15 (2017-11-25)

Changes:

- Add support for adding shortcuts to home screen when adding or editing a card. ([pull #155](https://github.com/brarcher/loyalty-card-locker/pull/155))
- Remove widget, as it was a poor substitute for shortcuts. ([pull #155](https://github.com/brarcher/loyalty-card-locker/pull/155))
- Fix exporting backups on Android 7+. ([pull #153](https://github.com/brarcher/loyalty-card-locker/pull/153))
- Report more accurate mime type when exporting backup data. ([pull #156](https://github.com/brarcher/loyalty-card-locker/pull/156))
- Fix bug where a card could not be edited. ([pull #155](https://github.com/brarcher/loyalty-card-locker/pull/155))

## v0.14 (2017-10-26)

Changes:

- Add support for app shortcuts (Android 7.1+), where the most recently used cards will appear as shortcuts. ([pull #145](https://github.com/brarcher/loyalty-card-locker/pull/145))
- Add a widget which works like a pinned app shortcut, to support devices which run below Android 7.1. ([pull #142](https://github.com/brarcher/loyalty-card-locker/pull/142))

## v0.13 (2017-07-25)

Changes:

- Add screen rotation lock menu option when displaying a card. If locked, the screen will transition to its "natural" orientation and further screen rotation will be blocked. ([pull #128](https://github.com/brarcher/loyalty-card-locker/pull/128))
- If a card is selected from the main screen but cannot be loaded, the application fails gracefully and posts a message. ([pull #132](https://github.com/brarcher/loyalty-card-locker/pull/132))
- Fix case where layout IDs for intro wizard could not be found. ([pull #128](https://github.com/brarcher/loyalty-card-locker/pull/128))

## v0.12 (2017-07-16)

Changes:

- A change in v0.11 reduced the memory usage of barcode drawing, but affected the barcode dimensions. This is now changed to maintain the barcode dimensions while reducing memory usage. ([pull #126](https://github.com/brarcher/loyalty-card-locker/pull/126))
- Update German and French translations. ([pull #122](https://github.com/brarcher/loyalty-card-locker/pull/122), [pull #124](https://github.com/brarcher/loyalty-card-locker/pull/124), [pull #125](https://github.com/brarcher/loyalty-card-locker/pull/125))

## v0.11.1 (2017-06-29)

Changes:

- Prevent a crash when rotation the screen in the first run intro wizard.

## v0.11 (2017-06-26)

Improvements:

- When editing a card ID, pre-populate the existing ID to start. ([pull #94](https://github.com/brarcher/loyalty-card-locker/pull/94))
- Limit the width of generated barcodes to reduce memory usage and out of memory errors. ([pull #103](https://github.com/brarcher/loyalty-card-locker/pull/103))
- When editing a card, change the "Enter Card" button to say "Edit Card" if a card ID already exists. ([pull #104](https://github.com/brarcher/loyalty-card-locker/pull/104))
- Change the color scheme to be softer and compatible with the app icon, and change the layout when viewing a card to be cleaner. ([pull #107](https://github.com/brarcher/loyalty-card-locker/pull/107))
- Add an intro wizard which launches on the app's first launch. ([pull #108](https://github.com/brarcher/loyalty-card-locker/pull/108))

## v0.10 (2017-02-12)

Improvements:

- Changed the default import/export filename. ([pull #84](https://github.com/brarcher/loyalty-card-locker/pull/84))
- Correct string on the import/export page. ([pull #87](https://github.com/brarcher/loyalty-card-locker/pull/87))
- Improve layout of card view page. The text should be easier to read, and is selectable with a long click. ([pull #91](https://github.com/brarcher/loyalty-card-locker/pull/91))

## v0.9 (2017-01-17)

The "Locker" part of the name was not intuitive. To help remedy this a new application icon was created by betsythefc which better represents the purpose of the application: to store loyalty cards which use barcodes. Along with this new icon the name of the application has been changed to "Loyalty Card Keychain".

Additional features/improvements:

- Importing/Exporting cards was changed to be more flexible. ([pull #76](https://github.com/brarcher/loyalty-card-locker/pull/76))
- Translations for Lithuanian added. ([pull #62](https://github.com/brarcher/loyalty-card-locker/pull/62))
- Translations for French added. ([pull #80](https://github.com/brarcher/loyalty-card-locker/pull/80))

## v0.8 (2016-11-22)

New features/improvements:

- Screen brightness increased to its maximum when displaying a card, to help barcode scanners successfully capture the barcode. ([pull #54](https://github.com/brarcher/loyalty-card-locker/pull/54))
- Add a delete confirmation when deleting a card. ([pull #55](https://github.com/brarcher/loyalty-card-locker/pull/55))
- Add translations for German ([pull #57](https://github.com/brarcher/loyalty-card-locker/pull/57)) and Czech ([pull #58](https://github.com/brarcher/loyalty-card-locker/pull/58)).
- Clarification change for Italian translation. ([pull #66](https://github.com/brarcher/loyalty-card-locker/pull/66))

## v0.7 (2016-07-14)

New features/improvements:

- Long-click of a card brings up option to copy card ID to the clipboard. ([pull #49](https://github.com/brarcher/loyalty-card-locker/issues/49))

Bug fixes:

- Back button on Input/Export view now works, moving user to main view

## v0.6 (2016-05-23)

New features/improvements:

- Allow user to enter barcode manually. If a user elects to enter a barcode manually, a list of all valid and supported barcode images is displayed. The user then may select the barcode image which matches what the user wants. [issue #33](https://github.com/brarcher/loyalty-card-locker/issues/33), [pull #44](https://github.com/brarcher/loyalty-card-locker/pull/44)

Bug fixes:

- Resolve issue where some displayed barcodes were blurry. ([issue #37](https://github.com/brarcher/loyalty-card-locker/issues/37))

## v0.5 (2016-05-16)

New features/improvements:

- An about dialog can be opened from the main screen, which gives details about the application and project on GitHub ([issue #19](https://github.com/brarcher/loyalty-card-locker/issues/19))
- Allow loyalty card information to be imported from/exported to a CSV file in external storage ([issue #36](https://github.com/brarcher/loyalty-card-locker/issues/36), [issue #20](https://github.com/brarcher/loyalty-card-locker/issues/20))

## v0.4 (2016-04-09)

New features/improvements:

- Dutch translation
- Allow name field to be editable after adding loyalty card
- Add an optional note field

Bug fixes:

- Resolve all issues identified by FindBugs and require all FindBugs issues be resolved prior to pull request acceptance

## v0.3 (2016-02-11)

- Now officially supports the following list of 1D and 2D barcodes:
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

- Generated barcodes are larger, easier to scan from a scanning device

## v0.2 (2016-02-07)

- Italian translations
- Support for all 1D barcode types. (Originally only product 1D barcodes were supported)
- Add required camera permission, which was initially missing.

## v0.1 (2016-01-30)

- Ability to create/edit/delete loyalty cards
- Capture barcode of loyalty card using a camera
- Display captured barcode, for scanning at a store
