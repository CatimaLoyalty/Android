package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.Group;
import protect.card_locker.Utils;
import protect.card_locker.ZipUtils;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class CatimaImporter implements Importer {
    public void importData(Context context, DBHelper db, InputStream input, char[] password) throws IOException, FormatException, InterruptedException {
        InputStream bufferedInputStream = new BufferedInputStream(input);
        bufferedInputStream.mark(100);

        // First, check if this is a zip file
        ZipInputStream zipInputStream = new ZipInputStream(bufferedInputStream, password);

        boolean isZipFile = false;

        LocalFileHeader localFileHeader;
        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
            isZipFile = true;

            String fileName = Uri.parse(localFileHeader.getFileName()).getLastPathSegment();
            if (fileName.equals("catima.csv")) {
                importCSV(context, db, new ByteArrayInputStream(ZipUtils.read(zipInputStream).getBytes(StandardCharsets.UTF_8)));
            } else if (fileName.endsWith(".png")) {
                Utils.saveCardImage(context, ZipUtils.readImage(zipInputStream), fileName);
            } else {
                throw new FormatException("Unexpected file in import: " + fileName);
            }
        }

        if (!isZipFile) {
            // This is not a zip file, try importing as bare CSV
            bufferedInputStream.reset();
            importCSV(context, db, bufferedInputStream);
        }
    }

    public void importCSV(Context context, DBHelper db, InputStream input) throws IOException, FormatException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        bufferedReader.mark(100);

        Integer version = 1;

        try {
            version = Integer.parseInt(bufferedReader.readLine());
        } catch (NumberFormatException _e) {
            // Assume version 1
        }

        bufferedReader.reset();

        switch (version) {
            case 1:
                parseV1(context, db, bufferedReader);
                break;
            case 2:
                parseV2(context, db, bufferedReader);
                break;
            default:
                throw new FormatException(String.format("No code to parse version %s", version));
        }

        bufferedReader.close();
    }

    public void parseV1(Context context, DBHelper db, BufferedReader input) throws IOException, FormatException, InterruptedException {
        final CSVParser parser = new CSVParser(input, CSVFormat.RFC4180.builder().setHeader().build());

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        try {
            for (CSVRecord record : parser) {
                importLoyaltyCard(context, database, db, record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            parser.close();
            database.setTransactionSuccessful();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            database.endTransaction();
            database.close();
        }
    }

    public void parseV2(Context context, DBHelper db, BufferedReader input) throws IOException, FormatException, InterruptedException {
        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        Integer part = 0;
        String stringPart = "";

        try {
            while (true) {
                String tmp = input.readLine();

                if (tmp == null || tmp.isEmpty()) {
                    boolean sectionParsed = false;

                    switch (part) {
                        case 0:
                            // This is the version info, ignore
                            sectionParsed = true;
                            break;
                        case 1:
                            try {
                                parseV2Groups(db, database, stringPart);
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 2:
                            try {
                                parseV2Cards(context, db, database, stringPart);
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 3:
                            try {
                                parseV2CardGroups(db, database, stringPart);
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        default:
                            throw new FormatException("Issue parsing CSV data, too many parts for v2 parsing");
                    }

                    if (tmp == null) {
                        break;
                    }

                    if (sectionParsed) {
                        part += 1;
                        stringPart = "";
                    } else {
                        stringPart += tmp + "\n";
                    }
                } else {
                    stringPart += tmp + "\n";
                }
            }
            database.setTransactionSuccessful();
        } catch (FormatException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            database.endTransaction();
            database.close();
        }
    }

    public void parseV2Groups(DBHelper db, SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
        // Parse groups
        final CSVParser groupParser = new CSVParser(new StringReader(data), CSVFormat.RFC4180.builder().setHeader().build());

        List<CSVRecord> records = new ArrayList<>();

        try {
            for (CSVRecord record : groupParser) {
                records.add(record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            groupParser.close();
        }

        for (CSVRecord record : records) {
            importGroup(database, db, record);
        }
    }

    public void parseV2Cards(Context context, DBHelper db, SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
        // Parse cards
        final CSVParser cardParser = new CSVParser(new StringReader(data), CSVFormat.RFC4180.builder().setHeader().build());

        List<CSVRecord> records = new ArrayList<>();

        try {
            for (CSVRecord record : cardParser) {
                records.add(record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            cardParser.close();
        }

        for (CSVRecord record : records) {
            importLoyaltyCard(context, database, db, record);
        }
    }

    public void parseV2CardGroups(DBHelper db, SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
        // Parse card group mappings
        final CSVParser cardGroupParser = new CSVParser(new StringReader(data), CSVFormat.RFC4180.builder().setHeader().build());

        List<CSVRecord> records = new ArrayList<>();

        try {
            for (CSVRecord record : cardGroupParser) {
                records.add(record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            cardGroupParser.close();
        }

        for (CSVRecord record : records) {
            importCardGroupMapping(database, db, record);
        }
    }

    /**
     * Import a single loyalty card into the database using the given
     * session.
     */
    private void importLoyaltyCard(Context context, SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws IOException, FormatException {
        int id = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.ID, record, false);

        String store = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.STORE, record, "");
        if (store.isEmpty()) {
            throw new FormatException("No store listed, but is required");
        }

        String note = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.NOTE, record, "");
        Date expiry = null;
        try {
            expiry = new Date(CSVHelpers.extractLong(DBHelper.LoyaltyCardDbIds.EXPIRY, record, true));
        } catch (NullPointerException | FormatException e) {
        }

        BigDecimal balance;
        try {
            balance = new BigDecimal(CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BALANCE, record, null));
        } catch (FormatException _e) {
            // These fields did not exist in versions 1.8.1 and before
            // We catch this exception so we can still import old backups
            balance = new BigDecimal("0");
        }

        Currency balanceType = null;
        String unparsedBalanceType = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BALANCE_TYPE, record, "");
        if (!unparsedBalanceType.isEmpty()) {
            balanceType = Currency.getInstance(unparsedBalanceType);
        }

        String cardId = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.CARD_ID, record, "");
        if (cardId.isEmpty()) {
            throw new FormatException("No card ID listed, but is required");
        }

        String barcodeId = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BARCODE_ID, record, "");
        if (barcodeId.isEmpty()) {
            barcodeId = null;
        }

        CatimaBarcode barcodeType = null;
        String unparsedBarcodeType = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE, record, "");
        if (!unparsedBarcodeType.isEmpty()) {
            barcodeType = CatimaBarcode.fromName(unparsedBarcodeType);
        }

        Integer headerColor = null;

        if (record.isMapped(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)) {
            headerColor = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.HEADER_COLOR, record, true);
        }

        int starStatus = 0;
        try {
            starStatus = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.STAR_STATUS, record, false);
        } catch (FormatException _e) {
            // This field did not exist in versions 0.28 and before
            // We catch this exception so we can still import old backups
        }
        if (starStatus != 1) starStatus = 0;

        Long lastUsed = 0L;
        try {
            lastUsed = CSVHelpers.extractLong(DBHelper.LoyaltyCardDbIds.LAST_USED, record, false);
        } catch (FormatException _e) {
            // This field did not exist in versions 2.5.0 and before
            // We catch this exception so we can still import old backups
        }

        helper.insertLoyaltyCard(database, id, store, note, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, starStatus, lastUsed);
    }

    /**
     * Import a single group into the database using the given
     * session.
     */
    private void importGroup(SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws IOException, FormatException {
        String id = CSVHelpers.extractString(DBHelper.LoyaltyCardDbGroups.ID, record, null);

        helper.insertGroup(database, id);
    }

    /**
     * Import a single card to group mapping into the database using the given
     * session.
     */
    private void importCardGroupMapping(SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws IOException, FormatException {
        Integer cardId = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIdsGroups.cardID, record, false);
        String groupId = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIdsGroups.groupID, record, null);

        List<Group> cardGroups = helper.getLoyaltyCardGroups(cardId);
        cardGroups.add(helper.getGroup(groupId));
        helper.setLoyaltyCardGroups(database, cardId, cardGroups);
    }
}