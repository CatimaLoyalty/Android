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
import java.util.Set;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;
import protect.card_locker.Group;
import protect.card_locker.ImageLocationType;
import protect.card_locker.LoyaltyCard;
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
        // Pass #1: get hashes and parse CSV
        InputStream input1 = new FileInputStream(inputFile);
        InputStream bufferedInputStream1 = new BufferedInputStream(input1);
        bufferedInputStream1.mark(100);
        ZipInputStream zipInputStream1 = new ZipInputStream(bufferedInputStream1, password);

        // First, check if this is a zip file
        boolean isZipFile = false;
        LocalFileHeader localFileHeader;
        Map<String, String> imageChecksums = new HashMap<>();
        ImportedData importedData = null;

        while ((localFileHeader = zipInputStream1.getNextEntry()) != null) {
            isZipFile = true;

            String fileName = Uri.parse(localFileHeader.getFileName()).getLastPathSegment();
            if (fileName.equals("catima.csv")) {
                importedData = importCSV(zipInputStream1);
            } else if (fileName.endsWith(".png")) {
                if (!fileName.matches(Utils.CARD_IMAGE_FILENAME_REGEX)) {
                    throw new FormatException("Unexpected PNG file in import: " + fileName);
                }
                imageChecksums.put(fileName, Utils.checksum(zipInputStream1));
            } else {
                throw new FormatException("Unexpected file in import: " + fileName);
            }
        }

        if (!isZipFile) {
            // This is not a zip file, try importing as bare CSV
            bufferedInputStream1.reset();
            importedData = importCSV(bufferedInputStream1);
        }

        input1.close();

        if (importedData == null) {
            throw new FormatException("No imported data");
        }

        Map<Integer, Integer> idMap = saveAndDeduplicate(context, database, importedData, imageChecksums);

        if (isZipFile) {
            // Pass #2: save images
            InputStream input2 = new FileInputStream(inputFile);
            InputStream bufferedInputStream2 = new BufferedInputStream(input2);
            ZipInputStream zipInputStream2 = new ZipInputStream(bufferedInputStream2, password);

            while ((localFileHeader = zipInputStream2.getNextEntry()) != null) {
                String fileName = Uri.parse(localFileHeader.getFileName()).getLastPathSegment();
                if (fileName.endsWith(".png")) {
                    String newFileName = Utils.getRenamedCardImageFileName(fileName, idMap);
                    Utils.saveCardImage(context, ZipUtils.readImage(zipInputStream2), newFileName);
                }
            }

            input2.close();
        }
    }

    public Map<Integer, Integer> saveAndDeduplicate(Context context, SQLiteDatabase database, final ImportedData data, final Map<String, String> imageChecksums) throws IOException {
        Map<Integer, Integer> idMap = new HashMap<>();
        Set<String> existingImages = DBHelper.imageFiles(context, database);

        for (LoyaltyCard card : data.cards) {
            LoyaltyCard existing = DBHelper.getLoyaltyCard(context, database, card.id);
            if (existing == null) {
                DBHelper.insertLoyaltyCard(database, card.id, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                        card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
            } else if (!isDuplicate(context, existing, card, existingImages, imageChecksums)) {
                long newId = DBHelper.insertLoyaltyCard(database, card.store, card.note, card.validFrom, card.expiry, card.balance, card.balanceType,
                        card.cardId, card.barcodeId, card.barcodeType, card.headerColor, card.starStatus, card.lastUsed, card.archiveStatus);
                idMap.put(card.id, (int) newId);
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

        return idMap;
    }

    public boolean isDuplicate(Context context, final LoyaltyCard existing, final LoyaltyCard card, final Set<String> existingImages, final Map<String, String> imageChecksums) throws IOException {
        if (!LoyaltyCard.isDuplicate(context, existing, card)) {
            return false;
        }
        for (ImageLocationType imageLocationType : ImageLocationType.values()) {
            String name = Utils.getCardImageFileName(existing.id, imageLocationType);
            boolean exists = existingImages.contains(name);
            if (exists != imageChecksums.containsKey(name)) {
                return false;
            }
            if (exists) {
                File file = Utils.retrieveCardImageAsFile(context, name);
                if (!imageChecksums.get(name).equals(Utils.checksum(new FileInputStream(file)))) {
                    return false;
                }
            }
        }
        return true;
    }

    public ImportedData importCSV(InputStream input) throws IOException, FormatException, InterruptedException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

        int version = parseVersion(bufferedReader);
        switch (version) {
            case 1:
                return parseV1(bufferedReader);
            case 2:
                return parseV2(bufferedReader);
            default:
                throw new FormatException(String.format("No code to parse version %s", version));
        }
    }

    public ImportedData parseV1(BufferedReader input) throws IOException, FormatException, InterruptedException {
        ImportedData data = new ImportedData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        final CSVParser parser = new CSVParser(input, CSVFormat.RFC4180.builder().setHeader().build());

        try {
            for (CSVRecord record : parser) {
                LoyaltyCard card = importLoyaltyCard(record);
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

    public ImportedData parseV2(BufferedReader input) throws IOException, FormatException, InterruptedException {
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
                                cards = parseV2Cards(stringPart.toString());
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

    public List<LoyaltyCard> parseV2Cards(String data) throws IOException, FormatException, InterruptedException {
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
            LoyaltyCard card = importLoyaltyCard(record);
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
    private LoyaltyCard importLoyaltyCard(CSVRecord record) throws FormatException {
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
                null,
                null,
                null,
                null,
                null,
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
