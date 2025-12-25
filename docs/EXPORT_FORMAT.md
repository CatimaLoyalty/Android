# Export format

Since version 2.0, a Catima export is a Zip file containing:

1. A single catima.csv file
2. All image files as .png

Continue reading for more details about both.

Before 2.0, the backup format was just the .csv file, not inside a .zip file. It can still be imported in 2.0+. To import a 2.0+ backup into older Catima versions, one should extract the .csv file and import that instead.

## catima.csv
The catima.csv file is a CSV file in the following format:

```
[version number]

[group database table description]
[one line per group]

[card database table description]
[one line per card] (may contain a multi-line note)

[card to group linking database table description]
[one line per link]
```

Version number is always 2. If a database table is empty, there are simply no lines. For valid possible values for each table, see [Card fields](./CARD_FIELDS.md). Do keep in mind that, depending on the Catima version, some fields may not be supported and thus missing from the export. You should treat all the fields you don't need as optional.

Example full export:
```
2

_id
Health
Food
Fashion

_id,store,note,validfrom,expiry,balance,balancetype,cardid,barcodeid,barcodetype,barcodeencoding,headercolor,starstatus,lastused,archive
1,Clothing Store,Note about store,,,0,,qw,,,,-45147,1,1730493938,0
2,Department Store,,,,0,,A,,,,-1499549,0,1730493491,0
3,Grocery Store,,,1453633200000,50,,dhd,,,,-11751600,0,1730493369,0
4,Pharmacy,,,,0,,dhshsvshs,,,,-16766866,0,1684347330,1
5,Restaurant,Note about restaurant here,,,0,,98765432,,CODE_128,UTF-8,-10902850,0,1730493357,0
6,Shoe Store,,,,0,,zhxbx,,AZTEC,ISO-8859-1,-6543440,0,1684347744,0

cardId,groupId
8,Fashion
3,Food
4,Health
5,Food
6,Fashion
```

## Image files
The image files are named using the following pattern:
```
card_<id>_<side>.png
```

With id referring to the id of the loyalty card it belongs to and side being either `front`, `back` or `icon`.
