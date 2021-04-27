package protect.card_locker.importexport;

import android.database.sqlite.SQLiteDatabase;

import com.google.zxing.BarcodeFormat;

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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import protect.card_locker.DBHelper;
import protect.card_locker.FormatException;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class FidmeImporter implements DatabaseImporter
{
    public void importData(DBHelper db, InputStream input) throws IOException, FormatException, JSONException, ParseException {
        // We actually retrieve a .zip file
        ZipInputStream zipInputStream = new ZipInputStream(input);

        StringBuilder loyaltyCards = new StringBuilder();
        byte[] buffer = new byte[1024];
        int read = 0;

        ZipEntry zipEntry;

        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            if (zipEntry.getName().equals("loyalty_programs.csv")) {
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

        final CSVParser fidmeParser = new CSVParser(new StringReader(loyaltyCards.toString()), CSVFormat.RFC4180.withDelimiter(';').withHeader());

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
            throws IOException, FormatException
    {
        // A loyalty card export from Fidme contains the following fields:
        // Retailer (store name)
        // Program (program name)
        // Added at (YYYY-MM-DD HH:MM:SS UTC)
        // Reference (card ID)
        // Firstname (card holder first name)
        // Lastname (card holder last name)

        // The store is called Retailer
        String store = CSVHelpers.extractString("Retailer", record, "");

        if (store.isEmpty())
        {
            throw new FormatException("No store listed, but is required");
        }

        // There seems to be no note field in the CSV? So let's combine other fields instead...
        String program = CSVHelpers.extractString("Program", record, "");
        String addedAt = CSVHelpers.extractString("Added At", record, "");
        String firstName = CSVHelpers.extractString("Firstname", record, "");
        String lastName = CSVHelpers.extractString("Lastname", record, "");

        String combinedName = String.format("%s %s", firstName, lastName);

        StringBuilder noteBuilder = new StringBuilder();
        if (!program.isEmpty()) noteBuilder.append(program).append('\n');
        if (!addedAt.isEmpty()) noteBuilder.append(addedAt).append('\n');
        if (!combinedName.isEmpty()) noteBuilder.append(combinedName).append('\n');
        String note = noteBuilder.toString();

        // The ID is called reference
        String cardId = CSVHelpers.extractString("Reference", record, "");
        if(cardId.isEmpty())
        {
            throw new FormatException("No card ID listed, but is required");
        }

        // Sadly, Fidme exports don't contain the card type
        // I guess they have an online DB of all the different companies and what type they use
        // TODO: Hook this into our own loyalty card DB if we ever get one
        BarcodeFormat barcodeType = null;

        // No favourite data in the export either
        int starStatus = 0;

        // TODO: Front and back image

        helper.insertLoyaltyCard(database, store, note, null, BigDecimal.valueOf(0), null, cardId, null, barcodeType, null, starStatus, null, null);
    }
}