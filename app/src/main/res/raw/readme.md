# stocard_stores.csv

stocard_stores.csv was created by extracting /data/data/de.stocard/de.stocard.stocard/databases/stores on a rooted devices and running the following command over it:

```
sqlite3 -header -csv sync_db "select id,content from synced_resources where collection = '/loyalty-card-providers/'" > stocard_providers.csv
while IFS= read -r line; do
  if [ "$line" = "id,content" ]; then
    echo "_id,name,barcodeFormat" > stocard_stores.csv
  else
    id="$(echo "$line" | cut -d ',' -f1)"
    name="$(echo "$line" | cut -d ',' -f2- | sed 's/""/"/g' | sed 's/^"//g' | sed 's/"$//g' | jq -r .name)"
    barcodeFormat="$(echo "$line" | cut -d ',' -f2- | sed 's/""/"/g' | sed 's/^"//g' | sed 's/"$//g' | jq -r .default_barcode_format)"
    echo "$id,\"$name\",$barcodeFormat" >> stocard_stores.csv
  fi
done < stocard_providers.csv
```

Only used for data portability reasons (ensuring importing works). Do NOT copy this anywhere else or use it for any purpose other than ensuring we can import a GDPR-provided export. We want to make sure this stays under fair use.
