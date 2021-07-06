package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;

import com.google.zxing.BarcodeFormat;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.Utils;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class StocardImporter implements Importer
{
    public void importData(Context context, DBHelper db, InputStream input, char[] password) throws IOException, FormatException, JSONException, ParseException {
        HashMap<String, HashMap<String, Object>> loyaltyCardHashMap = new HashMap<>();
        HashMap<String, String> providers = new HashMap<>();

        ZipInputStream zipInputStream = new ZipInputStream(input, password);

        String[] providersFileName = null;
        String[] cardBaseName = null;
        String cardName = "";
        LocalFileHeader localFileHeader;
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
            String fileName = localFileHeader.getFileName();
            String[] nameParts = fileName.split("/");

            if (providersFileName == null) {
                providersFileName = new String[] {
                        nameParts[0],
                        "sync",
                        "data",
                        "users",
                        nameParts[0],
                        "analytics-properties.json"
                };
                cardBaseName = new String[] {
                        nameParts[0],
                        "sync",
                        "data",
                        "users",
                        nameParts[0],
                        "loyalty-cards"
                };
            }

            if (startsWith(nameParts, providersFileName, 0) && !localFileHeader.isDirectory()) {
                providers = parseProviders(zipInputStream);
            } else if (startsWith(nameParts, cardBaseName, 1)) {
                // Extract cardName
                cardName = nameParts[cardBaseName.length].split("\\.", 2)[0];

                // This is the card itself
                if (nameParts.length == cardBaseName.length + 1) {
                    // Ignore the .txt file
                    if (fileName.endsWith(".json")) {
                        JSONObject jsonObject = readJSON(zipInputStream);

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

                        try {
                            loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                                    loyaltyCardHashMap,
                                    cardName,
                                    "barcodeType",
                                    jsonObject.getString("input_barcode_format")
                            );
                        } catch (JSONException ignored) {}
                    }
                } else if (fileName.endsWith("notes/default.json")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "note",
                            readJSON(zipInputStream)
                                .getString("content")
                    );
                } else if (fileName.endsWith("/images/front.png")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "frontImage",
                            read(zipInputStream)
                    );
                } else if (fileName.endsWith("/images/back.png")) {
                    loyaltyCardHashMap = appendToLoyaltyCardHashMap(
                            loyaltyCardHashMap,
                            cardName,
                            "backImage",
                            read(zipInputStream)
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
            String store = providers.get(loyaltyCardData.get("_providerId").toString());
            String note = (String) loyaltyCardData.getOrDefault("note", "");
            String cardId = (String) loyaltyCardData.get("cardId");
            String barcodeTypeString = (String) loyaltyCardData.getOrDefault("barcodeType", null);
            BarcodeFormat barcodeType = null;
            if (barcodeTypeString != null) {
                if (barcodeTypeString.equals("RSS_DATABAR_EXPANDED")) {
                    barcodeType = BarcodeFormat.RSS_EXPANDED;
                } else {
                    barcodeType = BarcodeFormat.valueOf(barcodeTypeString);
                }
            }

            long loyaltyCardInternalId = db.insertLoyaltyCard(database, store, note, null, BigDecimal.valueOf(0), null, cardId, null, barcodeType, null, 0);

            if (loyaltyCardData.containsKey("frontImage")) {
                byte[] byteArray = ((String) loyaltyCardData.get("frontImage")).getBytes();
                Utils.saveCardImage(context, BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), (int) loyaltyCardInternalId, true);
            }
            if (loyaltyCardData.containsKey("backImage")) {
                byte[] byteArray = ((String) loyaltyCardData.get("backImage")).getBytes();
                Utils.saveCardImage(context, BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), (int) loyaltyCardInternalId, false);
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

    private String read(ZipInputStream zipInputStream) throws IOException {
        int read;
        byte[] buffer = new byte[4096];

        StringBuilder stringBuilder = new StringBuilder();
        while ((read = zipInputStream.read(buffer, 0, 4096)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }

        return stringBuilder.toString();
    }

    private JSONObject readJSON(ZipInputStream zipInputStream) throws IOException, JSONException {
        return new JSONObject(read(zipInputStream));
    }

    private HashMap<String, HashMap<String, Object>> appendToLoyaltyCardHashMap(HashMap<String, HashMap<String, Object>> loyaltyCardHashMap, String cardID, String key, Object value) {
        HashMap<String, Object> loyaltyCardData = loyaltyCardHashMap.getOrDefault(cardID, new HashMap<>());

        loyaltyCardData.put(key, value);
        loyaltyCardHashMap.put(cardID, loyaltyCardData);

        return loyaltyCardHashMap;
    }

    private HashMap<String, String> parseProviders(ZipInputStream zipInputStream) throws IOException, JSONException {
        // FIXME: This is probably completely wrong, but it works for the one and only test file I have
        JSONObject jsonObject = readJSON(zipInputStream);

        JSONArray providerIdList = jsonObject.getJSONArray("provider_id_list");
        JSONArray providerList = jsonObject.getJSONArray("provider_list");

        // Resort, put IDs with - in them after IDs without any -
        List<String> providerIds = new ArrayList<>();
        List<String> customProviderIds = new ArrayList<>();

        for (int i = 0; i < providerIdList.length(); i++) {
            String providerId = providerIdList.get(i).toString();
            if (providerId.contains("-")) {
                customProviderIds.add(providerId);
            } else {
                providerIds.add(providerId);
            }
        }
        providerIds.addAll(customProviderIds);

        HashMap<String, String> providers = new HashMap<>();
        for (int i = 0; i < jsonObject.getInt("number_of_cards"); i++) {
            providers.put(providerIds.get(i), providerList.get(i).toString());
        }

        return providers;
    }
}