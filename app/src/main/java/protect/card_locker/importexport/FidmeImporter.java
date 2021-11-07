package protect.card_locker.importexport;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.LocalFileHeader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 * <p>
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class FidmeImporter implements Importer {
    public void importData(Context context, DBHelper db, InputStream input, char[] password) throws IOException, FormatException, JSONException, ParseException {
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

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        final CSVParser fidmeParser = new CSVParser(new StringReader(loyaltyCards.toString()), CSVFormat.RFC4180.builder().setDelimiter(';').setHeader().build());

        try {
            for (CSVRecord record : fidmeParser) {
                importLoyaltyCard(database, db, record);

                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException | InterruptedException e) {
            throw new FormatException("Issue parsing CSV data", e);
        } finally {
            fidmeParser.close();
        }

        database.setTransactionSuccessful();
        database.endTransaction();
        database.close();

        zipInputStream.close();
    }

    /**
     * Import a single loyalty card into the database using the given
     * session.
     */
    private void importLoyaltyCard(SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws IOException, FormatException {
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
            throw new FormatException("No card ID listed, but is required");
        }

        // Sadly, Fidme exports don't contain the card type
        // I guess they have an online DB of all the different companies and what type they use
        // TODO: Hook this into our own loyalty card DB if we ever get one
        CatimaBarcode barcodeType = null;

        // No favourite data in the export either
        int starStatus = 0;

        // TODO: Front and back image

        helper.insertLoyaltyCard(database, store, note, null, BigDecimal.valueOf(0), null, cardId, null, barcodeType, null, starStatus, null);
    }
}