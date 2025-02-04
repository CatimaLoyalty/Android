package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

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
public class FidmeImporter implements Importer {
    public static class ImportedData {
        public final List<LoyaltyCard> cards;

        ImportedData(final List<LoyaltyCard> cards) {
            this.cards = cards;
        }
    }

    public void importData(Context context, SQLiteDatabase database, File inputFile, char[] password) throws IOException, FormatException, JSONException, ParseException {
        InputStream input = new FileInputStream(inputFile);
        // We actually retrieve a .zip file
        ZipInputStream zipInputStream = new ZipInputStream(input, password);

        StringBuilder loyaltyCards = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read = 0;

        LocalFileHeader localFileHeader;

        while ((localFileHeader = zipInputStream.getNextEntry()) != null) {
            if (localFileHeader.getFileName().equals("loyalty_programs.csv")) {
                while ((read = zipInputStream.read(buffer, 0, 1024)) >= 0) {
                    loyaltyCards.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
            }
        }

        if (loyaltyCards.length() == 0) {
            throw new FormatException("Couldn't find loyalty_programs.csv in zip file or it is empty");
        }

        final CSVParser fidmeParser = new CSVParser(new StringReader(loyaltyCards.toString()), CSVFormat.RFC4180.builder().setDelimiter(';').setHeader().build());
        ImportedData importedData = new ImportedData(new ArrayList<>());

        try {
            for (CSVRecord record : fidmeParser) {
                LoyaltyCard card = importLoyaltyCard(context, record);
                if (card != null) {
                    importedData.cards.add(card);
                }

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException | InterruptedException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            fidmeParser.close();
        }

        zipInputStream.close();
        input.close();

        saveAndDeduplicate(database, importedData);
    }

    /**
     * Import a single loyalty card into the database using the given
     * session.
     */
    private LoyaltyCard importLoyaltyCard(Context context, CSVRecord record) throws FormatException {
        // A loyalty card export from Fidme contains the following fields:
        // Retailer (store name)
        // Program (program name)
        // Added at (YYYY-MM-DD HH:MM:SS UTC)
        // Reference (card ID)
        // Firstname (card holder first name)
        // Lastname (card holder last name)

        // The store is called Retailer
        String store = CSVHelpers.extractString("Retailer", record, "");

        if (store.isEmpty()) {
            throw new FormatException("No store listed, but is required");
        }

        // There seems to be no note field in the CSV? So let's combine other fields instead...
        String program = CSVHelpers.extractString("Program", record, "").trim();
        String addedAt = CSVHelpers.extractString("Added At", record, "").trim();
        String firstName = CSVHelpers.extractString("Firstname", record, "").trim();
        String lastName = CSVHelpers.extractString("Lastname", record, "").trim();

        String combinedName = String.format("%s %s", firstName, lastName).trim();

        StringBuilder noteBuilder = new StringBuilder();
        if (!program.isEmpty()) noteBuilder.append(program).append('\n');
        if (!addedAt.isEmpty()) noteBuilder.append(addedAt).append('\n');
        if (!combinedName.isEmpty()) noteBuilder.append(combinedName).append('\n');
        String note = noteBuilder.toString().trim();

        // The ID is called reference
        String cardId = CSVHelpers.extractString("Reference", record, "");
        if (cardId.isEmpty()) {
            // Fidme deletes the card id if a card is expired
            // Because Catima considers the card id a required field, we ignore these expired cards
            // https://github.com/CatimaLoyalty/Android/issues/1005
            return null;
        }

        // Sadly, Fidme exports don't contain the card type
        // I guess they have an online DB of all the different companies and what type they use
        // TODO: Hook this into our own loyalty card DB if we ever get one
        CatimaBarcode barcodeType = null;

        // No favourite data or colour in the export either
        int starStatus = 0;
        int archiveStatus = 0;
        int headerColor = Utils.getRandomHeaderColor(context);

        // TODO: Front and back image

        // use -1 for the ID, it will be ignored when inserting the card into the DB
        return new LoyaltyCard(
                -1,
                store,
                note,
                null,
                null,
                BigDecimal.valueOf(0),
                null,
                cardId,
                null,
                barcodeType,
                headerColor,
                starStatus,
                Utils.getUnixTime(),
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