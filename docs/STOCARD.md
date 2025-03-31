# Stocard Importer

The `app/src/main/res/raw/stocard_stores.csv` CSV file used by the Stocard importer was created using the `.scripts/dump_stocard_stores.py` script.

Only used for data portability reasons (ensuring importing works). Do NOT copy this anywhere else or use it for any purpose other than ensuring we can import a GDPR-provided export. We want to make sure this stays under fair use.``
Run 
``` bash
sqlite3 -header -csv /home/markus/sync_db "
SELECT  row_number() over (order by content) as _id,
json_extract(content,'$.input_id') barcodeid,
json_extract(content,'$.input_id') cardid,
json_extract(content,'$.input_barcode_format') barcodetype, 
json_extract((select content from synced_resources where collection ||id =  json_extract(x.content,'$.input_provider_reference.identifier')), '$.name')  store,
'' note,
'' validfrom,
'' expiry,
'' balance,
'' balancetype,
'' headercolor,
'' starstatus,
'' lastused,
'' archive
FROM synced_resources x where content_type like 'application/%json' AND ( (collection LIKE '%user%'))
and json_extract(content,'$.input_provider_reference.identifier') is not null" > catima.csv
```
 to get a import file.
The file sync_db is from /data/data/de.stocard.stocard/databases/sync_db in andrid-x86.9.0 running in qemu

While analyziing this file I found that each use in Stocard was logged including the location !
SELECT json_extract(content,'$.time.value'),  cast('point('||json_extract(content,'$.gps_location.lng')||' '||json_extract(content,'$.gps_location.lat')||')' as varchar(500)), *
FROM synced_resources where content_type like 'application/%json' AND ( (collection LIKE '%user%'))
and json_extract(content,'$.gps_location.lat') is not null order by json_extract(content,'$.time.value')

