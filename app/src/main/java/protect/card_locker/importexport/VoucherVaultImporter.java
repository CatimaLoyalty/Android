package protect.card_locker.importexport;

import android.annotation.SuppressLint;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.TimeZone;

import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class VoucherVaultImporter implements DatabaseImporter
{
    public void importData(DBHelper db, InputStream input) throws IOException, FormatException, JSONException, ParseException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        JSONArray jsonArray = new JSONArray(sb.toString());

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

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

            BigDecimal balance;
            if (!jsonCard.isNull("balance")) {
                balance = new BigDecimal(String.valueOf(jsonCard.getDouble("balance")));
            } else {
                balance = new BigDecimal("0");
            }

            Currency balanceType = Currency.getInstance("USD");

            String cardId = jsonCard.getString("code");

            BarcodeFormat barcodeType = null;

            String codeTypeFromJSON = jsonCard.getString("codeType");
            switch (codeTypeFromJSON) {
                case "CODE128":
                    barcodeType = BarcodeFormat.CODE_128;
                    break;
                case "CODE39":
                    barcodeType = BarcodeFormat.CODE_39;
                    break;
                case "EAN13":
                    barcodeType = BarcodeFormat.EAN_13;
                    break;
                case "PDF417":
                    barcodeType = BarcodeFormat.PDF_417;
                    break;
                case "QR":
                    barcodeType = BarcodeFormat.QR_CODE;
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

            db.insertLoyaltyCard(store, "", expiry, balance, balanceType, cardId, null, barcodeType, headerColor, 0, null, null);
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();

        bufferedReader.close();
    }
}