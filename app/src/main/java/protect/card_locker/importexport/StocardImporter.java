package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.ImageLocationType;
import protect.card_locker.R;
import protect.card_locker.Utils;
import protect.card_locker.ZipUtils;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class StocardImporter implements Importer {
    public void importData(Context context, DBHelper db, InputStream input, char[] password) throws IOException, FormatException, JSONException, ParseException {
        HashMap<String, HashMap<String, Object>> loyaltyCardHashMap = new HashMap<>();
        HashMap<String, HashMap<String, String>> providers = new HashMap<>();

        final CSVParser parser = new CSVParser(new InputStreamReader(context.getResources().openRawResource(R.raw.stocard_stores), StandardCharsets.UTF_8), CSVFormat.RFC4180.builder().setHeader().build());

        try {
            for (CSVRecord record : parser) {
                HashMap<String, String> recordData = new HashMap<>();
                recordData.put("name", record.get("name"));
                recordData.put("barcodeFormat", record.get("barcodeFormat"));

                providers.put(record.get("_id"), recordData);
            }

            parser.close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        }

        ZipInputStream zipInputStream = new ZipInputStream(input, password);

        String[] providersFileName = null;
        String[] cardBaseName = null;
        String cardName = "";
        LocalFileHeader localFileHeader;
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
            String fileName = localFileHeader.getFileName();
            String[] nameParts = fileName.split("/");

            if (providersFileName == null) {
                providersFileName = new String[]{
                        nameParts[0],
                        "sync",
                        "data",
                        "users",
                        nameParts[0],
                        "analytics-properties.json"
                };
                cardBaseName = new String[]{
                        nameParts[0],
                        "sync",
                        "data",
                        "users",
                        nameParts[0],
                        "loyalty-cards"
                };
            }

            if (startsWith(nameParts, cardBaseName, 1)) {
                // Extract cardName
                cardName = nameParts[cardBaseName.length].split("\\.", 2)[0];

                // This is the card itself
                if (nameParts.length == cardBaseName.length + 1) {
                    // Ignore the .txt file
                    if (fileName.endsWith(".json")) {
                        JSONObject jsonObject = ZipUtils.readJSON(zipInputStream);

                        loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                                loyaltyCardHashMap,
                                cardName,
                                "cardId",
                                jsonObject.getString("input_id")
                        );
                        loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                                loyaltyCardHashMap,
                                cardName,
                                "_providerId",
                                jsonObject
                                        .getJSONObject("input_provider_reference")
                                        .getString("identifier")
                                        .substring("/loyalty-card-providers/".length())
                        );

                        if (jsonObject.has("input_barcode_format")) {
                            loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                                    loyaltyCardHashMap,
                                    cardName,
                                    "barcodeType",
                                    jsonObject.getString("input_barcode_format")
                            );
                        }
                    }
                } else if (fileName.endsWith("notes/default.json")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "note",
                            ZipUtils.readJSON(zipInputStream)
                                    .getString("content")
                    );
                } else if (fileName.endsWith("/images/front.png")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "frontImage",
                            ZipUtils.readImage(zipInputStream)
                    );
                } else if (fileName.endsWith("/images/back.png")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "backImage",
                            ZipUtils.readImage(zipInputStream)
                    );
                }
            }
        }

        if (loyaltyCardHashMap.keySet().size() == 0) {
            throw new FormatException("Couldn't find any loyalty cards in this Stocard export.");
        }

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        for (HashMap<String, Object> loyaltyCardData : loyaltyCardHashMap.values()) {
            String providerId = (String) loyaltyCardData.get("_providerId");
            HashMap<String, String> providerData = providers.get(providerId);

            String store = providerData != null ? providerData.get("name") : providerId;
            String note = (String) Utils.mapGetOrDefault(loyaltyCardData, "note", "");
            String cardId = (String) loyaltyCardData.get("cardId");
            String barcodeTypeString = (String) Utils.mapGetOrDefault(loyaltyCardData, "barcodeType", providerData != null ? providerData.get("barcodeFormat") : null);
            CatimaBarcode barcodeType = null;
            if (barcodeTypeString != null) {
                if (barcodeTypeString.equals("RSS_DATABAR_EXPANDED")) {
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.RSS_EXPANDED);
                } else {
                    barcodeType = CatimaBarcode.fromName(barcodeTypeString);
                }
            }

            long loyaltyCardInternalId = db.insertLoyaltyCard(database, store, note, null, BigDecimal.valueOf(0), null, cardId, null, barcodeType, null, 0, null);

            if (loyaltyCardData.containsKey("frontImage")) {
                Utils.saveCardImage(context, (Bitmap) loyaltyCardData.get("frontImage"), (int) loyaltyCardInternalId, ImageLocationType.front);
            }
            if (loyaltyCardData.containsKey("backImage")) {
                Utils.saveCardImage(context, (Bitmap) loyaltyCardData.get("backImage"), (int) loyaltyCardInternalId, ImageLocationType.back);
            }
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();

        zipInputStream.close();
    }

    private boolean startsWith(String[] full, String[] start, int minExtraLength) {
        if (full.length - minExtraLength < start.length) {
            return false;
        }

        for (int i = 0; i < start.length; i++) {
            if (!start[i].contentEquals(full[i])) {
                return false;
            }
        }

        return true;
    }

    private HashMap<String, HashMap<String, Object>> appendToLoyaltyCardHashMap(HashMap<String, HashMap<String, Object>> loyaltyCardHashMap, String cardID, String key, Object value) {
        HashMap<String, Object> loyaltyCardData = loyaltyCardHashMap.get(cardID);
        if (loyaltyCardData == null) {
            loyaltyCardData = new HashMap<>();
        }

        loyaltyCardData.put(key, value);
        loyaltyCardHashMap.put(cardID, loyaltyCardData);

        return loyaltyCardHashMap;
    }
}