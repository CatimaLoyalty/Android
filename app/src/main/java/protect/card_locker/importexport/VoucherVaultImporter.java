package protect.card_locker.importexport;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.Utils;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class VoucherVaultImporter implements Importer {
    public static class ImportedData {
        public final List<LoyaltyCard> cards;

        ImportedData(final List<LoyaltyCard> cards) {
            this.cards = cards;
        }
    }

    public void importData(Context context, SQLiteDatabase database, File inputFile, char[] password) throws IOException, FormatException, JSONException, ParseException {
        InputStream input = new FileInputStream(inputFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        JSONArray jsonArray = new JSONArray(sb.toString());

        bufferedReader.close();
        input.close();

        ImportedData importedData = importJSON(jsonArray);
        saveAndDeduplicate(database, importedData);
    }

    public ImportedData importJSON(JSONArray jsonArray) throws FormatException, JSONException, ParseException {
        ImportedData importedData = new ImportedData(new ArrayList<>());

        // See https://github.com/tim-smart/vouchervault/issues/4#issuecomment-788226503 for more info
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonCard = jsonArray.getJSONObject(i);

            String store = jsonCard.getString("description");

            Date expiry = null;
            if (!jsonCard.isNull("expires")) {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                expiry = dateFormat.parse(jsonCard.getString("expires"));
            }

            BigDecimal balance = new BigDecimal("0");
            if (jsonCard.has("balanceMilliunits")) {
                if (!jsonCard.isNull("balanceMilliunits")) {
                    balance = new BigDecimal(String.valueOf(jsonCard.getInt("balanceMilliunits") / 1000.0));
                }
            } else if (!jsonCard.isNull("balance")) {
                balance = new BigDecimal(String.valueOf(jsonCard.getDouble("balance")));
            }

            Currency balanceType = Currency.getInstance("USD");

            String cardId = jsonCard.getString("code");

            CatimaBarcode barcodeType = null;

            String codeTypeFromJSON = jsonCard.getString("codeType");
            switch (codeTypeFromJSON) {
                case "CODE128":
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.CODE_128);
                    break;
                case "CODE39":
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.CODE_39);
                    break;
                case "EAN13":
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.EAN_13);
                    break;
                case "PDF417":
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.PDF_417);
                    break;
                case "QR":
                    barcodeType = CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE);
                    break;
                case "TEXT":
                    break;
                default:
                    throw new FormatException("Unknown barcode type found: " + codeTypeFromJSON);
            }

            int headerColor;

            String colorFromJSON = jsonCard.getString("color");
            switch (colorFromJSON) {
                case "GREY":
                    headerColor = Color.GRAY;
                    break;
                case "BLUE":
                    headerColor = Color.BLUE;
                    break;
                case "GREEN":
                    headerColor = Color.GREEN;
                    break;
                case "ORANGE":
                    headerColor = Color.rgb(255, 165, 0);
                    break;
                case "PURPLE":
                    headerColor = Color.rgb(128, 0, 128);
                    break;
                case "RED":
                    headerColor = Color.RED;
                    break;
                case "YELLOW":
                    headerColor = Color.YELLOW;
                    break;
                default:
                    throw new FormatException("Unknown colour type found: " + colorFromJSON);
            }

            // use -1 for the ID, it will be ignored when inserting the card into the DB
            importedData.cards.add(new LoyaltyCard(
                    -1,
                    store,
                    "",
                    null,
                    expiry,
                    balance,
                    balanceType,
                    cardId,
                    null,
                    barcodeType,
                    headerColor,
                    0,
                    Utils.getUnixTime(),
                    DBHelper.DEFAULT_ZOOM_LEVEL,
                    DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }

        return importedData;
    }

    public void saveAndDeduplicate(SQLiteDatabase database, final ImportedData data) {
        // This format does not have IDs that can cause conflicts
        // Proper deduplication for all formats will be implemented later
        for (LoyaltyCard card : data.cards) {
            // Do not use card.id which is set to -1
            DBHelper.insertLoyaltyCard(database, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                    card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
        }
    }
}