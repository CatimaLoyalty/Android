package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.zxing.BarcodeFormat;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.ImageLocationType;
import protect.card_locker.LoyaltyCard;
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
    public static class StocardProvider {
        public String name = null;
        public String barcodeFormat = null;
        public Bitmap logo = null;
    }

    public static class StocardRecord {
        public String providerId = null;
        public String store = null;
        public String label = null;
        public String note = null;
        public String cardId = null;
        public String barcodeType = null;
        public Long lastUsed = null;
        public Bitmap frontImage = null;
        public Bitmap backImage = null;

        @NonNull
        @Override
        public String toString() {
            return String.format(
                    "StocardRecord{%n  providerId=%s,%n  store=%s,%n  label=%s,%n  note=%s,%n  cardId=%s,%n"
                            + "  barcodeType=%s,%n  lastUsed=%s,%n  frontImage=%s,%n  backImage=%s%n}",
                    this.providerId,
                    this.store,
                    this.label,
                    this.note,
                    this.cardId,
                    this.barcodeType,
                    this.lastUsed,
                    this.frontImage,
                    this.backImage
            );
        }
    }

    public static class ZIPData {
        public final Map<String, StocardRecord> cards;
        public final Map<String, StocardProvider> providers;

        ZIPData(final Map<String, StocardRecord> cards, final Map<String, StocardProvider> providers) {
            this.cards = cards;
            this.providers = providers;
        }
    }

    public static class ImportedData {
        public final List<LoyaltyCard> cards;
        public final Map<Integer, Map<ImageLocationType, Bitmap>> images;

        ImportedData(final List<LoyaltyCard> cards, final Map<Integer, Map<ImageLocationType, Bitmap>> images) {
            this.cards = cards;
            this.images = images;
        }
    }

    public static final String PROVIDER_PREFIX = "/loyalty-card-providers/";

    private static final String TAG = "Catima";

    public void importData(Context context, SQLiteDatabase database, File inputFile, char[] password) throws IOException, FormatException, JSONException, ParseException {
        ZIPData zipData = new ZIPData(new HashMap<>(), new HashMap<>());

        final CSVParser parser = new CSVParser(new InputStreamReader(context.getResources().openRawResource(R.raw.stocard_stores), StandardCharsets.UTF_8), CSVFormat.RFC4180.builder().setHeader().build());

        try {
            for (CSVRecord record : parser) {
                StocardProvider provider = new StocardProvider();
                provider.name = record.get("name").trim();
                provider.barcodeFormat = record.get("barcodeFormat").trim();

                zipData.providers.put(record.get("_id").trim(), provider);
            }

            parser.close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        }

        ZipFile zipFile = new ZipFile(inputFile, password);
        zipData = importZIP(zipFile, zipData);
        zipFile.close();

        if (zipData.cards.keySet().size() == 0) {
            throw new FormatException("Couldn't find any loyalty cards in this Stocard export.");
        }

        ImportedData importedData = importLoyaltyCardHashMap(context, zipData);
        saveAndDeduplicate(context, database, importedData);
    }

    public ZIPData importZIP(ZipFile zipFile, final ZIPData zipData) throws IOException, FormatException, JSONException {
        Map<String, StocardRecord> cards = zipData.cards;
        Map<String, StocardProvider> providers = zipData.providers;

        String[] customProvidersBaseName = null;
        String[] cardBaseName = null;
        String customProviderId = "";
        String cardName = "";
        for (FileHeader fileHeader : zipFile.getFileHeaders()) {
            String fileName = fileHeader.getFileName();
            String[] nameParts = fileName.split("/");

            if (nameParts.length < 2) {
                continue;
            }

            String userId = nameParts[1];
            ZipInputStream zipInputStream = zipFile.getInputStream(fileHeader);

            if (customProvidersBaseName == null) {
                // FIXME: can we use the points-account/statement/content.json balance info somehow?
                /*
                  Known files:
                    extracts/<user-UUID>/users/<user-UUID>/
                      analytics-properties/content.json
                      devices/<device-UUID>/
                        analytics-properties/content.json
                        content.json
                        ip-location-wifi/content.json
                      enabled-regions/<UUID>/content.json
                      loyalty-card-custom-providers/<provider-UUID>/content.json - custom providers
                      loyalty-cards/<card-UUID>/
                        card-linked-coupons/accounts/default/
                          content.json
                          user-coupons/<UUID>/content.json
                        content.json - card itself
                        images/back.png - back image (legacy)
                        images/back/back.jpg - back image
                        images/back/content.json
                        images/front.png - front image (legacy)
                        images/front/content.json
                        images/front/front.jpg - front image
                        notes/default/content.json - note
                        points-account/
                          content.json
                          statement/content.json
                        usages/<UUID>/content.json - timestamps
                        usage-statistics/content.json - timestamps
                      reward-program-balances/<UUID>/content.json
                */
                customProvidersBaseName = new String[]{
                        "extracts",
                        userId,
                        "users",
                        userId,
                        "loyalty-card-custom-providers"
                };
                cardBaseName = new String[]{
                        "extracts",
                        userId,
                        "users",
                        userId,
                        "loyalty-cards"
                };
            }

            if (startsWith(nameParts, customProvidersBaseName, 1)) {
                // Extract providerId
                customProviderId = nameParts[customProvidersBaseName.length];

                StocardProvider provider = providers.get(customProviderId);
                if (provider == null) {
                    provider = new StocardProvider();
                    providers.put(customProviderId, provider);
                }

                // Name file
                if (fileName.endsWith(customProviderId + "/content.json")) {
                    JSONObject jsonObject = ZipUtils.readJSON(zipInputStream);
                    provider.name = jsonObject.getString("name");
                } else if (fileName.endsWith("logo.png")) {
                    provider.logo = ZipUtils.readImage(zipInputStream);
                } else if (!fileName.endsWith("/")) {
                    Log.d(TAG, "Unknown or unused loyalty-card-custom-providers file " + fileName + ", skipping...");
                }
            } else if (startsWith(nameParts, cardBaseName, 1)) {
                // Extract cardName
                cardName = nameParts[cardBaseName.length];

                StocardRecord record = cards.get(cardName);
                if (record == null) {
                    record = new StocardRecord();
                    cards.put(cardName, record);
                }

                // This is the card itself
                if (fileName.endsWith(cardName + "/content.json")) {
                    JSONObject jsonObject = ZipUtils.readJSON(zipInputStream);
                    record.cardId = jsonObject.getString("input_id");

                    if (jsonObject.has("input_provider_name")) {
                        record.store = jsonObject.getString("input_provider_name");
                    }

                    if (jsonObject.has("label")) {
                        String label = jsonObject.getString("label");
                        if (!label.isBlank()) {
                            record.label = label;
                        }
                    }

                    // Provider ID can be either custom or not, extract whatever version is relevant
                    String customProviderPrefix = "/users/" + userId + "/loyalty-card-custom-providers/";
                    String providerId = jsonObject
                            .getJSONObject("input_provider_reference")
                            .getString("identifier");
                    if (providerId.startsWith(customProviderPrefix)) {
                        providerId = providerId.substring(customProviderPrefix.length());
                    } else if (providerId.startsWith(PROVIDER_PREFIX)) {
                        providerId = providerId.substring(PROVIDER_PREFIX.length());
                    } else {
                        throw new FormatException("Unsupported provider ID format: " + providerId);
                    }

                    record.providerId = providerId;

                    if (jsonObject.has("input_barcode_format")) {
                        record.barcodeType = jsonObject.getString("input_barcode_format");
                    }
                } else if (fileName.endsWith("notes/default/content.json")) {
                    record.note = ZipUtils.readJSON(zipInputStream).getString("content");
                } else if (fileName.endsWith("usage-statistics/content.json")) {
                    JSONArray usages = ZipUtils.readJSON(zipInputStream).getJSONArray("usages");
                    for (int i = 0; i < usages.length(); i++) {
                        JSONObject lastUsedObject = usages.getJSONObject(i);
                        String lastUsedString = lastUsedObject.getJSONObject("time").getString("value");
                        long timeStamp = Instant.parse(lastUsedString).getEpochSecond();
                        if (record.lastUsed == null || timeStamp > record.lastUsed) {
                            record.lastUsed = timeStamp;
                        }
                    }
                } else if (fileName.matches(".*/usages/[^/]+/content.json")) {
                    JSONObject lastUsedObject = ZipUtils.readJSON(zipInputStream);
                    String lastUsedString = lastUsedObject.getJSONObject("time").getString("value");
                    long timeStamp = Instant.parse(lastUsedString).getEpochSecond();
                    if (record.lastUsed == null || timeStamp > record.lastUsed) {
                        record.lastUsed = timeStamp;
                    }
                } else if (fileName.endsWith("/images/front.png") || fileName.endsWith("/images/front/front.jpg")) {
                    record.frontImage = ZipUtils.readImage(zipInputStream);
                } else if (fileName.endsWith("/images/back.png") || fileName.endsWith("/images/back/back.jpg")) {
                    record.backImage = ZipUtils.readImage(zipInputStream);
                } else if (!fileName.endsWith("/")) {
                    Log.d(TAG, "Unknown or unused loyalty-cards file " + fileName + ", skipping...");
                }
            } else if (!fileName.endsWith("/")) {
                Log.d(TAG, "Unknown or unused file " + fileName + ", skipping...");
            }

            zipInputStream.close();
        }

        return new ZIPData(cards, providers);
    }

    public ImportedData importLoyaltyCardHashMap(Context context, final ZIPData zipData) throws FormatException {
        ImportedData importedData = new ImportedData(new ArrayList<>(), new HashMap<>());
        int tempID = 0;

        List<String> cardKeys = new ArrayList<>(zipData.cards.keySet());
        Collections.sort(cardKeys);

        for (String key : cardKeys) {
            StocardRecord record = zipData.cards.get(key);

            if (record.providerId == null) {
                Log.d(TAG, "Missing providerId for card " + record + ", ignoring...");
                continue;
            }

            if (record.cardId == null) {
                throw new FormatException("No card ID listed, but is required");
            }

            StocardProvider provider = zipData.providers.get(record.providerId);

            // Read store from card, if not available (old export), fall back to providerData
            String store = record.store != null ? record.store : provider != null ? provider.name : record.providerId;
            String note = record.note != null ? record.note : "";
            String barcodeTypeString = record.barcodeType != null ? record.barcodeType : provider != null ? provider.barcodeFormat : null;

            if (record.label != null && !record.label.equals(store) && !record.label.equals(note)) {
                note = note.isEmpty() ? record.label : note + "\n" + record.label;
            }

            CatimaBarcode barcodeType = null;
            if (barcodeTypeString != null && !barcodeTypeString.isEmpty()) {
                if (barcodeTypeString.equals("RSS_DATABAR_EXPANDED")) {
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.RSS_EXPANDED);
                } else if (barcodeTypeString.equals("GS1_128")) {
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.CODE_128);
                } else {
                    barcodeType = CatimaBarcode.fromName(barcodeTypeString);
                }
            }

            int headerColor = Utils.getRandomHeaderColor(context);
            if (provider != null && provider.logo != null) {
                headerColor = Utils.getHeaderColorFromImage(provider.logo, headerColor);
            }

            long lastUsed = record.lastUsed != null ? record.lastUsed : Utils.getUnixTime();

            LoyaltyCard card = new LoyaltyCard(
                    tempID,
                    store,
                    note,
                    null,
                    null,
                    BigDecimal.valueOf(0),
                    null,
                    record.cardId,
                    null,
                    barcodeType,
                    headerColor,
                    0,
                    lastUsed,
                    DBHelper.DEFAULT_ZOOM_LEVEL,
                    DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            importedData.cards.add(card);

            Map<ImageLocationType, Bitmap> images = new HashMap<>();

            if (provider != null && provider.logo != null) {
                images.put(ImageLocationType.icon, provider.logo);
            }
            if (record.frontImage != null) {
                images.put(ImageLocationType.front, record.frontImage);
            }
            if (record.backImage != null) {
                images.put(ImageLocationType.back, record.backImage);
            }

            importedData.images.put(tempID, images);
            tempID++;
        }

        return importedData;
    }

    public void saveAndDeduplicate(Context context, SQLiteDatabase database, final ImportedData data) throws IOException {
        // This format does not have IDs that can cause conflicts
        // Proper deduplication for all formats will be implemented later
        for (LoyaltyCard card : data.cards) {
            // card.id is temporary and only used to index the images Map
            long id = DBHelper.insertLoyaltyCard(database, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                    card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
            for (Map.Entry<ImageLocationType, Bitmap> entry : data.images.get(card.id).entrySet()) {
                Utils.saveCardImage(context, entry.getValue(), (int) id, entry.getKey());
            }
        }
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
}
