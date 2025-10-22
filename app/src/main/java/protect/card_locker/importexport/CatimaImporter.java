package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.Group;
import protect.card_locker.ImageLocationType;
import protect.card_locker.LoyaltyCard;
import protect.card_locker.Utils;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class CatimaImporter implements Importer {
    public static class ImportedData {
        public final List<LoyaltyCard> cards;
        public final List<String> groups;
        public final List<Map.Entry<Integer, String>> cardGroups;

        ImportedData(final List<LoyaltyCard> cards, final List<String> groups, final List<Map.Entry<Integer, String>> cardGroups) {
            this.cards = cards;
            this.groups = groups;
            this.cardGroups = cardGroups;
        }
    }

    public void importData(Context context, SQLiteDatabase database, File inputFile, char[] password) throws IOException, FormatException, InterruptedException {
        ImportedData importedData = null;
        ZipFile zipFile = new ZipFile(inputFile, password);

        if (zipFile.isValidZipFile()) {
            importedData = importZIP(zipFile);
        } else {
            // This is not a zip file, try importing as bare CSV
            InputStream input = new FileInputStream(inputFile);
            InputStream bufferedInputStream = new BufferedInputStream(input);
            bufferedInputStream.mark(100);
            importedData = importCSV(bufferedInputStream);
        }
        zipFile.close();

        if (importedData == null) {
            throw new FormatException("No imported data");
        }
        saveAndDeduplicate(context, database, importedData);
    }

    public void saveAndDeduplicate(Context context, SQLiteDatabase database, final ImportedData data) throws IOException {
        Map<Integer, Integer> idMap = new HashMap<>();

        for (LoyaltyCard card : data.cards) {
            List<LoyaltyCard> candidates = DBHelper.getLoyaltyCardsByCardId(context, database, card.cardId);
            boolean duplicateFound = false;

            for (LoyaltyCard existing : candidates) {
                if (LoyaltyCard.isDuplicate(context, existing, card)) {
                    duplicateFound = true;
                    break;
                }
            }

            if (!duplicateFound) {
                LoyaltyCard existing = DBHelper.getLoyaltyCard(context, database, card.id);
                if (existing != null && existing.id == card.id) {
                    long newId = DBHelper.insertLoyaltyCard(database, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                            card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
                    idMap.put(card.id, (int) newId);
                    Utils.saveCardImage(context, card.getImageThumbnail(context), (int) newId, ImageLocationType.icon);
                    Utils.saveCardImage(context, card.getImageFront(context), (int) newId, ImageLocationType.front);
                    Utils.saveCardImage(context, card.getImageBack(context), (int) newId, ImageLocationType.back);
                }else{
                    DBHelper.insertLoyaltyCard(database, card.id, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                            card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
                    Utils.saveCardImage(context, card.getImageThumbnail(context), card.id, ImageLocationType.icon);
                    Utils.saveCardImage(context, card.getImageFront(context), card.id, ImageLocationType.front);
                    Utils.saveCardImage(context, card.getImageBack(context), card.id, ImageLocationType.back);
                }
            }
        }

        for (String group : data.groups) {
            DBHelper.insertGroup(database, group);
        }

        for (Map.Entry<Integer, String> entry : data.cardGroups) {
            int cardId = idMap.getOrDefault(entry.getKey(), entry.getKey());
            String groupId = entry.getValue();
            // For existing & newly imported cards, add the groups from the import to the internal state
            List<Group> cardGroups = DBHelper.getLoyaltyCardGroups(database, cardId);
            cardGroups.add(DBHelper.getGroup(database, groupId));
            DBHelper.setLoyaltyCardGroups(database, cardId, cardGroups);
        }
    }

    public ImportedData importCSV(InputStream input) throws IOException, FormatException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        int version = parseVersion(bufferedReader);
        switch (version) {
            case 1:
                return parseV1(bufferedReader, null);
            case 2:
                return parseV2(bufferedReader, null);
            default:
                throw new FormatException(String.format("No code to parse version %s", version));
        }
    }

    public ImportedData importZIP(ZipFile zipFile) throws IOException, FormatException, InterruptedException {
        FileHeader fileHeader = zipFile.getFileHeader("catima.csv");
        if (fileHeader == null) {
            throw new FormatException("No imported data");
        }

        InputStream inputStream = zipFile.getInputStream(fileHeader);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        int version = parseVersion(bufferedReader);
        switch (version) {
            case 1:
                return parseV1(bufferedReader, zipFile);
            case 2:
                return parseV2(bufferedReader, zipFile);
            default:
                throw new FormatException(String.format("No code to parse version %s", version));
        }
    }

    public ImportedData parseV1(BufferedReader bufferedInput, ZipFile zipFile) throws IOException, FormatException, InterruptedException {
        ImportedData data = new ImportedData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        final CSVParser parser = new CSVParser(bufferedInput, CSVFormat.RFC4180.builder().setHeader().build());

        try {
            for (CSVRecord record : parser) {
                LoyaltyCard card = importLoyaltyCard(record, zipFile);
                data.cards.add(card);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }

            parser.close();
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new FormatException("Issue parsing CSV data", e);
        }

        return data;
    }

    public ImportedData parseV2(BufferedReader input, ZipFile zipFile) throws IOException, FormatException, InterruptedException {
        List<LoyaltyCard> cards = new ArrayList<>();
        List<String> groups = new ArrayList<>();
        List<Map.Entry<Integer, String>> cardGroups = new ArrayList<>();

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
                                groups = parseV2Groups(stringPart.toString());
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 2:
                            try {
                                cards = parseV2Cards(stringPart.toString(), zipFile);
                                sectionParsed = true;
                            } catch (FormatException e) {
                                // We may have a multiline field, try again
                            }
                            break;
                        case 3:
                            try {
                                cardGroups = parseV2CardGroups(stringPart.toString());
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

        return new ImportedData(cards, groups, cardGroups);
    }

    public List<String> parseV2Groups(String data) throws IOException, FormatException, InterruptedException {
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

        List<String> groups = new ArrayList<>();
        for (CSVRecord record : records) {
            String group = importGroup(record);
            groups.add(group);
        }
        return groups;
    }

    public List<LoyaltyCard> parseV2Cards(String data, ZipFile zipFile) throws IOException, FormatException, InterruptedException {
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

        List<LoyaltyCard> cards = new ArrayList<>();
        for (CSVRecord record : records) {
            LoyaltyCard card = importLoyaltyCard(record, zipFile);
            cards.add(card);
        }
        return cards;
    }

    public List<Map.Entry<Integer, String>> parseV2CardGroups(String data) throws IOException, FormatException, InterruptedException {
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

        List<Map.Entry<Integer, String>> cardGroups = new ArrayList<>();
        for (CSVRecord record : records) {
            Map.Entry<Integer, String> entry = importCardGroupMapping(record);
            cardGroups.add(entry);
        }
        return cardGroups;
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
    private LoyaltyCard importLoyaltyCard(CSVRecord record, ZipFile zipFile) throws FormatException, IOException {
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

        Bitmap imgIcon = null;
        Bitmap imgFront = null;
        Bitmap imgBack = null;

        if (zipFile != null) {
            FileHeader headerIcon = zipFile.getFileHeader(Utils.getCardImageFileName(id, ImageLocationType.icon));
            FileHeader headerFront = zipFile.getFileHeader(Utils.getCardImageFileName(id, ImageLocationType.front));
            FileHeader headerBack = zipFile.getFileHeader(Utils.getCardImageFileName(id, ImageLocationType.back));

            if (headerIcon != null) {
                imgIcon = BitmapFactory.decodeStream(zipFile.getInputStream(headerIcon));
            }
            if (headerFront != null) {
                imgFront = BitmapFactory.decodeStream(zipFile.getInputStream(headerFront));
            }
            if (headerBack != null) {
                imgBack = BitmapFactory.decodeStream(zipFile.getInputStream(headerBack));
            }
        }

        return new LoyaltyCard(
                id,
                store,
                note,
                validFrom,
                expiry,
                balance,
                balanceType,
                cardId,
                barcodeId,
                barcodeType,
                headerColor,
                starStatus,
                lastUsed,
                DBHelper.DEFAULT_ZOOM_LEVEL,
                DBHelper.DEFAULT_ZOOM_LEVEL_WIDTH,
                archiveStatus,
                imgIcon,
                null,
                imgFront,
                null,
                imgBack,
                null
        );
    }

    /**
     * Import a single group into the database using the given
     * session.
     */
    private String importGroup(CSVRecord record) throws FormatException {
        String id = CSVHelpers.extractString(DBHelper.LoyaltyCardDbGroups.ID, record, null);

        if (id == null) {
            throw new FormatException("Group has no ID: " + record);
        }

        return id;
    }

    /**
     * Import a single card to group mapping into the database using the given
     * session.
     */
    private Map.Entry<Integer, String> importCardGroupMapping(CSVRecord record) throws FormatException {
        int cardId = CSVHelpers.extractInt(DBHelper.LoyaltyCardDbIdsGroups.cardID, record);
        String groupId = CSVHelpers.extractString(DBHelper.LoyaltyCardDbIdsGroups.groupID, record, null);

        if (groupId == null) {
            throw new FormatException("Group has no ID: " + record);
        }

        return Map.entry(cardId, groupId);
    }
}
