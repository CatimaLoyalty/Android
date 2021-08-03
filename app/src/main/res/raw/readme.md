# stocard_stores.csv

stocard_stores.csv was created by extracting /data/data/de.stocard/de.stocard.stocard/databases/stores on a rooted devices and running the following command over it:

    sqlite3 -header -csv stores "select _id,name,barcodeFormat from stores" > stocard_stores.csv

Only used for data portability reasons (ensuring importing works). Do NOT copy this anywhere else or use it for any purpose other than ensuring we can import a GDPR-provided export. We want to make sure this stays under fair use.
