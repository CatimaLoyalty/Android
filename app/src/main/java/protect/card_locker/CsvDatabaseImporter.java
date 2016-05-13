package protect.card_locker;

import android.database.sqlite.SQLiteDatabase;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Class for importing a database from CSV (Comma Separate Values)
 * formatted data.
 *
 * The database's loyalty cards are expected to appear in the CSV data.
 * A header is expected for the each table showing the names of the columns.
 */
public class CsvDatabaseImporter implements DatabaseImporter
{
    public void importData(DBHelper db, InputStreamReader input) throws IOException, FormatException, InterruptedException
    {
        final CSVParser parser = new CSVParser(input, CSVFormat.RFC4180.withHeader());

        SQLiteDatabase database = db.getWritableDatabase();
        database.beginTransaction();

        try
        {
            for (CSVRecord record : parser)
            {
                importLoyaltyCard(database, db, record);

                if(Thread.currentThread().isInterrupted())
                {
                    throw new InterruptedException();
                }
            }

            parser.close();
            database.setTransactionSuccessful();
        }
        catch(IllegalArgumentException e)
        {
            throw new FormatException("Issue parsing CSV data", e);
        }
        finally
        {
            database.endTransaction();
            database.close();
        }
    }

    /**
     * Extract a string from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, defaultValue is returned
     * if it is not null. Otherwise, a FormatException is thrown.
     */
    private String extractString(String key, CSVRecord record, String defaultValue)
            throws FormatException
    {
        String toReturn = defaultValue;

        if(record.isMapped(key))
        {
            toReturn = record.get(key);
        }
        else
        {
            if(defaultValue == null)
            {
                throw new FormatException("Field not used but expected: " + key);
            }
        }

        return toReturn;
    }

    /**
     * Extract an integer from the items array. The index into the array
     * is determined by looking up the index in the fields map using the
     * "key" as the key. If no such key exists, or the data is not a valid
     * int, a FormatException is thrown.
     */
    private int extractInt(String key, CSVRecord record)
            throws FormatException
    {
        if(record.isMapped(key) == false)
        {
            throw new FormatException("Field not used but expected: " + key);
        }

        try
        {
            return Integer.parseInt(record.get(key));
        }
        catch(NumberFormatException e)
        {
            throw new FormatException("Failed to parse field: " + key, e);
        }
    }

    /**
     * Import a single loyalty card into the database using the given
     * session.
     */
    private void importLoyaltyCard(SQLiteDatabase database, DBHelper helper, CSVRecord record)
            throws IOException, FormatException
    {
        int id = extractInt(DBHelper.LoyaltyCardDbIds.ID, record);

        String store = extractString(DBHelper.LoyaltyCardDbIds.STORE, record, "");
        if(store.isEmpty())
        {
            throw new FormatException("No store listed, but is required");
        }

        String note = extractString(DBHelper.LoyaltyCardDbIds.NOTE, record, "");

        String cardId = extractString(DBHelper.LoyaltyCardDbIds.CARD_ID, record, "");
        if(cardId.isEmpty())
        {
            throw new FormatException("No card ID listed, but is required");
        }

        String barcodeType = extractString(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE, record, "");
        if(barcodeType.isEmpty())
        {
            throw new FormatException("No barcode type listed, but is required");
        }

        helper.insertLoyaltyCard(database, id, store, note, cardId, barcodeType);
    }
}
