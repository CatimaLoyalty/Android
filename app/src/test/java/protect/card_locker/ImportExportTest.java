package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;

import com.google.zxing.BarcodeFormat;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class ImportExportTest
{
    private Activity activity;
    private DBHelper db;
    private long nowMs;
    private long lastYearMs;
    private final int MONTHS_PER_YEAR = 12;

    private final String BARCODE_DATA = "428311627547";
    private final String BARCODE_TYPE = BarcodeFormat.UPC_A.name();

    @Before
    public void setUp()
    {
        activity = Robolectric.setupActivity(MainActivity.class);
        db = new DBHelper(activity);
        nowMs = System.currentTimeMillis();

        Calendar lastYear = Calendar.getInstance();
        lastYear.set(Calendar.YEAR, lastYear.get(Calendar.YEAR)-1);
        lastYearMs = lastYear.getTimeInMillis();
    }

    /**
     * Add the given number of cards, each with
     * an index in the store name.
     * @param cardsToAdd
     */
    private void addLoyaltyCards(int cardsToAdd)
    {
        // Add in reverse order to test sorting
        for(int index = cardsToAdd; index > 0; index--)
        {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = db.insertLoyaltyCard(storeName, note, BARCODE_DATA, BARCODE_TYPE, index, index*2);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(cardsToAdd, db.getLoyaltyCardCount());
    }

    /**
     * Check that all of the cards follow the pattern
     * specified in addLoyaltyCards(), and are in sequential order
     * where the smallest card's index is 1
     */
    private void checkLoyaltyCards()
    {
        Cursor cursor = db.getLoyaltyCardCursor();
        int index = 1;

        while(cursor.moveToNext())
        {
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

            String expectedStore = String.format("store, \"%4d", index);
            String expectedNote = String.format("note, \"%4d", index);

            assertEquals(expectedStore, card.store);
            assertEquals(expectedNote, card.note);
            assertEquals(BARCODE_DATA, card.cardId);
            assertEquals(BARCODE_TYPE, card.barcodeType);
            assertEquals(Integer.valueOf(index), card.headerColor);
            assertEquals(Integer.valueOf(index*2), card.headerTextColor);

            index++;
        }
        cursor.close();
    }

    /**
     * Delete the contents of the database
     */
    private void clearDatabase()
    {
        SQLiteDatabase database = db.getWritableDatabase();
        database.execSQL("delete from " + DBHelper.LoyaltyCardDbIds.TABLE);
        database.close();

        assertEquals(0, db.getLoyaltyCardCount());
    }

    @Test
    public void multipleCardsExportImport() throws IOException
    {
        final int NUM_CARDS = 10;

        for(DataFormat format : DataFormat.values())
        {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            clearDatabase();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

            checkLoyaltyCards();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void importExistingCardsNotReplace() throws IOException
    {
        final int NUM_CARDS = 10;

        for(DataFormat format : DataFormat.values())
        {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export into CSV data
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);
            outStream.close();

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Import the CSV data on top of the existing database
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertTrue(result);

            assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

            checkLoyaltyCards();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void corruptedImportNothingSaved() throws IOException
    {
        final int NUM_CARDS = 10;

        for(DataFormat format : DataFormat.values())
        {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            boolean result = MultiFormatExporter.exportData(db, outStream, format);
            assertTrue(result);

            clearDatabase();

            // commons-csv would throw a RuntimeException if an entry was quotes but had
            // content after. For example:
            //   abc,def,""abc,abc
            //             ^ after the quote there should only be a , \n or EOF
            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat,\"\"a";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());
            InputStreamReader inStream = new InputStreamReader(inData);

            // Attempt to import the CSV data
            result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
            assertEquals(false, result);

            assertEquals(0, db.getLoyaltyCardCount());
        }
    }

    class TestTaskCompleteListener implements ImportExportTask.TaskCompleteListener
    {
        Boolean success;

        public void onTaskComplete(boolean success)
        {
            this.success = success;
        }
    }

    @Test
    public void useImportExportTask() throws FileNotFoundException
    {
        final int NUM_CARDS = 10;

        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File exportFile = new File(sdcardDir, "LoyaltyCardLocker.csv");

        for(DataFormat format : DataFormat.values())
        {
            addLoyaltyCards(NUM_CARDS);

            TestTaskCompleteListener listener = new TestTaskCompleteListener();

            // Export to the file
            FileOutputStream fileOutputStream = new FileOutputStream(exportFile);
            ImportExportTask task = new ImportExportTask(activity, format, fileOutputStream, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            // Check that the listener was executed
            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            clearDatabase();

            // Import everything back from the default location

            listener = new TestTaskCompleteListener();

            FileInputStream fileStream = new FileInputStream(exportFile);

            task = new ImportExportTask(activity, format, fileStream, listener);
            task.execute();

            // Actually run the task to completion
            Robolectric.flushBackgroundThreadScheduler();

            // Check that the listener was executed
            assertNotNull(listener.success);
            assertEquals(true, listener.success);

            assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

            checkLoyaltyCards();

            // Clear the database for the next format under test
            clearDatabase();
        }
    }

    @Test
    public void importWithoutColors() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                       DBHelper.LoyaltyCardDbIds.STORE + "," +
                       DBHelper.LoyaltyCardDbIds.NOTE + "," +
                       DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                       DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "\n";
        csvText += "1,store,note,12345,type";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));
        InputStreamReader inStream = new InputStreamReader(inputStream);

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals("12345", card.cardId);
        assertEquals("type", card.barcodeType);
        assertNull(card.headerColor);
        assertNull(card.headerTextColor);
    }

    @Test
    public void importWithoutNullColors() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "\n";
        csvText += "1,store,note,12345,type,,";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));
        InputStreamReader inStream = new InputStreamReader(inputStream);

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals("12345", card.cardId);
        assertEquals("type", card.barcodeType);
        assertNull(card.headerColor);
        assertNull(card.headerTextColor);
    }

    @Test
    public void importWithoutInvalidColors() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "\n";
        csvText += "1,store,note,12345,type,not a number,invalid";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));
        InputStreamReader inStream = new InputStreamReader(inputStream);

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
        assertEquals(false, result);
        assertEquals(0, db.getLoyaltyCardCount());
    }

    @Test
    public void importWithNoBarcodeType() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "\n";
        csvText += "1,store,note,12345,,1,1";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));
        InputStreamReader inStream = new InputStreamReader(inputStream);

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(db, inStream, DataFormat.CSV);
        assertEquals(true, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals("12345", card.cardId);
        assertEquals("", card.barcodeType);
        assertEquals(1, (long) card.headerColor);
        assertEquals(1, (long) card.headerTextColor);
    }
}
