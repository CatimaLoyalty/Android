#!/usr/bin/python3

import csv
import json
import msgpack

MSGPACK = "bootstrapdata.msgpack"
OUTFILE = "stocard_stores.csv"


def load(fh):
    data = []
    for r in msgpack.Unpacker(fh, raw=False):
        if r["collection"] == "/loyalty-card-providers/":
            d = json.loads(r["data"])
            data.append([r["resource_id"], d["name"], d["default_barcode_format"]])
    return data


def save(data, output_file=OUTFILE):
    with open(output_file, "w") as fh:
        writer = csv.writer(fh, lineterminator="\n")
        writer.writerow(["_id", "name", "barcodeFormat"])
        for row in data:
            writer.writerow(row)


if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(
        epilog=f"INPUT_FILE must be a .msgpack or .apk and defaults to {MSGPACK}; "
               f"OUTPUT_FILE defaults to {OUTFILE}")
    parser.add_argument("input_file", metavar="INPUT_FILE", nargs="?", default=MSGPACK)
    parser.add_argument("output_file", metavar="OUTPUT_FILE", nargs="?", default=OUTFILE)
    args = parser.parse_args()
    if args.input_file.lower().endswith(".apk"):
        import zipfile
        with zipfile.ZipFile(args.input_file) as zf:
            with zf.open(f"assets/{MSGPACK}") as fh:
                data = load(fh)
    else:
        with open(args.input_file, "rb") as fh:
            data = load(fh)
    save(data, args.output_file)
