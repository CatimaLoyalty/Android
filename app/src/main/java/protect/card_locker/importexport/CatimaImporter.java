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
    public void importData(Context context, SQLiteDatabase database, InputStream input, char[] password) throws IOException, FormatException, InterruptedException {
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
                importCSV(context, database, zipInputStream);
            } else if (fileName.endsWith(".png")) {
                Utils.saveCardImage(context, ZipUtils.readImage(zipInputStream), fileName);
            } else {
                throw new FormatException("Unexpected file in import: " + fileName);
            }
        }

        if (!isZipFile) {
            // This is not a zip file, try importing as bare CSV
            bufferedInputStream.reset();
            importCSV(context, database, bufferedInputStream);
        }

        input.close();
    }

    public void importCSV(Context context, SQLiteDatabase database, InputStream input) throws IOException, FormatException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        int version = parseVersion(bufferedReader);
        switch (version) {
            case 1:
                parseV1(database, bufferedReader);
                break;
            case 2:
                parseV2(context, database, bufferedReader);
                break;
            default:
                throw new FormatException(String.format("No code to parse version %s", version));
        }
    }

    public void parseV1(SQLiteDatabase database, BufferedReader input) throws IOException, FormatException, InterruptedException {
        final CSVParser parser = new CSVParser(input, CSVFormat.RFC4180.builder().setHeader().build());

        try {
            for (CSVRecord record : parser) {
                importLoyaltyCard(database, record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            parser.close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        }
    }

    public void parseV2(Context context, SQLiteDatabase database, BufferedReader input) throws IOException, FormatException, InterruptedException {
        int part = 0;
        StringBuilder stringPart = new StringBuilder();

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
                                parseV2Groups(database, stringPart.toString());
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 2:
                            try {
                                parseV2Cards(context, database, stringPart.toString());
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 3:
                            try {
                                parseV2CardGroups(database, stringPart.toString());
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
                        stringPart = new StringBuilder();
                    } else {
                        stringPart.append(tmp).append('\n');
                    }
                } else {
                    stringPart.append(tmp).append('\n');
                }
            }
        } catch (FormatException e) {
            throw new FormatException("Issue parsing CSV data", e);
        }
    }

    public void parseV2Groups(SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
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
            importGroup(database, record);
        }
    }

    public void parseV2Cards(Context context, SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
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
            importLoyaltyCard(database, record);
        }
    }

    public void parseV2CardGroups(SQLiteDatabase database, String data) throws IOException, FormatException, InterruptedException {
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
            importCardGroupMapping(database, record);
        }
    }

    /**
     * Parse the version number of the import file
     *
     * @param reader the reader containing the import file
     * @return the parsed version number, defaulting to 1 if none is found
     * @throws IOException there was a problem reading the file
     */
    private int parseVersion(BufferedReader reader) throws IOException {
        reader.mark(10); // slightly over the search limit just to be sure
        StringBuilder sb = new StringBuilder();
        int searchLimit = 5; // gives you version numbers up to 99999
        int codePoint;
        // search until the next whitespace, indicating the end of the version
        while (!Character.isWhitespace(codePoint = reader.read())) {
            // we found something that isn't a digit, or we ran out of chars
            if (!Character.isDigit(codePoint) || searchLimit <= 0) {
                reader.reset();
                return 1; // default value
            }
            sb.append((char) codePoint);
            searchLimit--;
        }
        reader.reset();
        if (sb.length() == 0) {
            return 1;
        }
        return Integer.parseInt(sb.toString());
    }

    /**
     * Import a single loyalty card into the database using the given
     * session.
     */
    private void importLoyaltyCard(SQLiteDatabase database, CSVRecord record)
            throws FormatException {
        int id = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.ID, record);

        String store = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.STORE, record, "");
        if (store.isEmpty()) {
            throw new FormatException("No store listed, but is required");
        }

        String note = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.NOTE, record, "");

        Date validFrom = null;
        Long validFromLong;
        try {
            validFromLong = CSVHelpers.extractLong(DBHelper.LoyaltyCardDbIds.VALID_FROM, record);
        } catch (FormatException ignored) {
            validFromLong = null;
        }
        if (validFromLong != null) {
            validFrom = new Date(validFromLong);
        }

        Date expiry = null;
        Long expiryLong;
        try {
            expiryLong = CSVHelpers.extractLong(DBHelper.LoyaltyCardDbIds.EXPIRY, record);
        } catch (FormatException ignored) {
            expiryLong = null;
        }
        if (expiryLong != null) {
            expiry = new Date(expiryLong);
        }

        // These fields did not exist in versions 1.8.1 and before
        // We default to 0 so we can still import old backups
        BigDecimal balance = new BigDecimal("0");
        String balanceString = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BALANCE, record, null);
        if (balanceString != null) {
            try {
                balance = new BigDecimal(CSVHelpers.extractString(DBHelper.LoyaltyCardDbIds.BALANCE, record, null));
            } catch (NumberFormatException ignored) {
            }
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
        try {
            headerColor = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.HEADER_COLOR, record);
        } catch (FormatException ignored) {
        }

        int starStatus = 0;
        try {
            starStatus = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.STAR_STATUS, record);
        } catch (FormatException _e) {
            // This field did not exist in versions 0.28 and before
            // We catch this exception so we can still import old backups
        }
        if (starStatus != 1) starStatus = 0;

        int archiveStatus = 0;
        try {
            archiveStatus = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS, record);
        } catch (FormatException _e) {
            // This field did not exist in versions 2.16.3 and before
            // We catch this exception so we can still import old backups
        }
        if (archiveStatus != 1) archiveStatus = 0;

        Long lastUsed = 0L;
        try {
            lastUsed = CSVHelpers.extractLong(DBHelper.LoyaltyCardDbIds.LAST_USED, record);
        } catch (FormatException _e) {
            // This field did not exist in versions 2.5.0 and before
            // We catch this exception so we can still import old backups
        }

        DBHelper.insertLoyaltyCard(database, id, store, note, validFrom, expiry, balance, balanceType, cardId, barcodeId, barcodeType, headerColor, starStatus, lastUsed, archiveStatus);
    }

    /**
     * Import a single group into the database using the given
     * session.
     */
    private void importGroup(SQLiteDatabase database, CSVRecord record) throws FormatException {
        String id = CSVHelpers.extractString(DBHelper.LoyaltyCardDbGroups.ID, record, null);

        if (id == null) {
            throw new FormatException("Group has no ID: " + record);
        }

        DBHelper.insertGroup(database, id);
    }

    /**
     * Import a single card to group mapping into the database using the given
     * session.
     */
    private void importCardGroupMapping(SQLiteDatabase database, CSVRecord record) throws FormatException {
        int cardId = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIdsGroups.cardID, record);
        String groupId = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIdsGroups.groupID, record, null);

        if (groupId == null) {
            throw new FormatException("Group has no ID: " + record);
        }

        List<Group> cardGroups = DBHelper.getLoyaltyCardGroups(database, cardId);
        cardGroups.add(DBHelper.getGroup(database, groupId));
        DBHelper.setLoyaltyCardGroups(database, cardId, cardGroups);
    }
}
