package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.zxing.BarcodeFormat;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import androidx.core.content.res.ResourcesCompat;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

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
    private final BarcodeFormat BARCODE_TYPE = BarcodeFormat.UPC_A;

    @Before
    public void setUp()
    {
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(MainActivity.class);
        db = TestHelpers.getEmptyDb(activity);
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
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(cardsToAdd, db.getLoyaltyCardCount());
    }

    private void addLoyaltyCardsFiveStarred()
    {
        int cardsToAdd = 9;
        // Add in reverse order to test sorting
        for(int index = cardsToAdd; index > 4; index--)
        {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 1);
            boolean result = (id != -1);
            assertTrue(result);
        }
        for(int index = cardsToAdd-5; index > 0; index--)
        {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            //if index is even
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0);
            boolean result = (id != -1);
            assertTrue(result);
        }
        assertEquals(cardsToAdd, db.getLoyaltyCardCount());
    }

    @Test
    public void addLoyaltyCardsWithExpiryNeverPastTodayFuture()
    {
        long id = db.insertLoyaltyCard("No Expiry", "", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0);
        boolean result = (id != -1);
        assertTrue(result);

        LoyaltyCard card = db.getLoyaltyCard((int) id);
        assertEquals("No Expiry", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE, card.barcodeType);
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = db.insertLoyaltyCard("Past", "", new Date((long) 1), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0);
        result = (id != -1);
        assertTrue(result);

        card = db.getLoyaltyCard((int) id);
        assertEquals("Past", card.store);
        assertEquals("", card.note);
        assertTrue(card.expiry.before(new Date()));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE, card.barcodeType);
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = db.insertLoyaltyCard("Today", "", new Date(), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0);
        result = (id != -1);
        assertTrue(result);

        card = db.getLoyaltyCard((int) id);
        assertEquals("Today", card.store);
        assertEquals("", card.note);
        assertTrue(card.expiry.before(new Date(new Date().getTime()+86400)));
        assertTrue(card.expiry.after(new Date(new Date().getTime()-86400)));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE, card.barcodeType);
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        // This will break after 19 January 2038
        // If someone is still maintaining this code base by then: I love you
        id = db.insertLoyaltyCard("Future", "", new Date(2147483648000L), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0);
        result = (id != -1);
        assertTrue(result);

        card = db.getLoyaltyCard((int) id);
        assertEquals("Future", card.store);
        assertEquals("", card.note);
        assertTrue(card.expiry.after(new Date(new Date().getTime()+86400)));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE, card.barcodeType);
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        assertEquals(4, db.getLoyaltyCardCount());
    }

    private void addGroups(int groupsToAdd)
    {
        // Add in reverse order to test sorting
        for(int index = groupsToAdd; index > 0; index--)
        {
            String groupName = String.format("group, \"%4d", index);
            long id = db.insertGroup(groupName);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(groupsToAdd, db.getGroupCount());
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
            assertEquals(null, card.expiry);
            assertEquals(new BigDecimal(String.valueOf(index)), card.balance);
            assertEquals(null, card.balanceType);
            assertEquals(BARCODE_DATA, card.cardId);
            assertEquals(null, card.barcodeId);
            assertEquals(BARCODE_TYPE, card.barcodeType);
            assertEquals(Integer.valueOf(index), card.headerColor);
            assertEquals(0, card.starStatus);

            index++;
        }
        cursor.close();
    }

    /**
     * Check that all of the cards follow the pattern
     * specified in addLoyaltyCardsSomeStarred(), and are in sequential order
     * with starred ones first
     */
    private void checkLoyaltyCardsFiveStarred()
        {
            Cursor cursor = db.getLoyaltyCardCursor();
            int index = 5;

            while(index<10)
        {
            cursor.moveToNext();
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

            String expectedStore = String.format("store, \"%4d", index);
            String expectedNote = String.format("note, \"%4d", index);

            assertEquals(expectedStore, card.store);
            assertEquals(expectedNote, card.note);
            assertEquals(null, card.expiry);
            assertEquals(new BigDecimal(String.valueOf(index)), card.balance);
            assertEquals(null, card.balanceType);
            assertEquals(BARCODE_DATA, card.cardId);
            assertEquals(null, card.barcodeId);
            assertEquals(BARCODE_TYPE, card.barcodeType);
            assertEquals(Integer.valueOf(index), card.headerColor);
            assertEquals(1, card.starStatus);

            index++;
        }

        index = 1;
        while(cursor.moveToNext() && index<5)
    {
        LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

        String expectedStore = String.format("store, \"%4d", index);
        String expectedNote = String.format("note, \"%4d", index);

        assertEquals(expectedStore, card.store);
        assertEquals(expectedNote, card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal(String.valueOf(index)), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE, card.barcodeType);
        assertEquals(Integer.valueOf(index), card.headerColor);
        assertEquals(0, card.starStatus);

        index++;
    }

        cursor.close();
    }

    /**
     * Check that all of the groups follow the pattern
     * specified in addGroups(), and are in sequential order
     * where the smallest group's index is 1
     */
    private void checkGroups()
    {
        Cursor cursor = db.getGroupCursor();
        int index = db.getGroupCount();

        while(cursor.moveToNext())
        {
            Group group = Group.toGroup(cursor);

            String expectedGroupName = String.format("group, \"%4d", index);

            assertEquals(expectedGroupName, group._id);

            index--;
        }
        cursor.close();
    }

    @Test
    public void multipleCardsExportImport() throws IOException
    {
        final int NUM_CARDS = 10;

        addLoyaltyCards(NUM_CARDS);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        boolean result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outStream, DataFormat.Catima);
        assertTrue(result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima);
        assertTrue(result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void multipleCardsExportImportSomeStarred() throws IOException
    {
        final int NUM_CARDS = 9;

        addLoyaltyCardsFiveStarred();

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        boolean result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outStream, DataFormat.Catima);
        assertTrue(result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima);
        assertTrue(result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCardsFiveStarred();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    private List<String> groupsToGroupNames(List<Group> groups)
    {
        List<String> groupNames = new ArrayList<>();

        for (Group group : groups) {
            groupNames.add(group._id);
        }

        return groupNames;
    }

    @Test
    public void multipleCardsExportImportWithGroups() throws IOException
    {
        final int NUM_CARDS = 10;
        final int NUM_GROUPS = 3;

        addLoyaltyCards(NUM_CARDS);
        addGroups(NUM_GROUPS);

        List<Group> emptyGroup = new ArrayList<>();

        List<Group> groupsForOne = new ArrayList<>();
        groupsForOne.add(db.getGroup("group, \"   1"));

        List<Group> groupsForTwo = new ArrayList<>();
        groupsForTwo.add(db.getGroup("group, \"   1"));
        groupsForTwo.add(db.getGroup("group, \"   2"));

        List<Group> groupsForThree = new ArrayList<>();
        groupsForThree.add(db.getGroup("group, \"   1"));
        groupsForThree.add(db.getGroup("group, \"   2"));
        groupsForThree.add(db.getGroup("group, \"   3"));

        List<Group> groupsForFour = new ArrayList<>();
        groupsForFour.add(db.getGroup("group, \"   1"));
        groupsForFour.add(db.getGroup("group, \"   2"));
        groupsForFour.add(db.getGroup("group, \"   3"));

        List<Group> groupsForFive = new ArrayList<>();
        groupsForFive.add(db.getGroup("group, \"   1"));
        groupsForFive.add(db.getGroup("group, \"   3"));

        db.setLoyaltyCardGroups(1, groupsForOne);
        db.setLoyaltyCardGroups(2, groupsForTwo);
        db.setLoyaltyCardGroups(3, groupsForThree);
        db.setLoyaltyCardGroups(4, groupsForFour);
        db.setLoyaltyCardGroups(5, groupsForFive);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        boolean result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outStream, DataFormat.Catima);
        assertTrue(result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima);
        assertTrue(result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());
        assertEquals(NUM_GROUPS, db.getGroupCount());

        checkLoyaltyCards();
        checkGroups();

        assertEquals(groupsToGroupNames(groupsForOne), groupsToGroupNames(db.getLoyaltyCardGroups(1)));
        assertEquals(groupsToGroupNames(groupsForTwo), groupsToGroupNames(db.getLoyaltyCardGroups(2)));
        assertEquals(groupsToGroupNames(groupsForThree), groupsToGroupNames(db.getLoyaltyCardGroups(3)));
        assertEquals(groupsToGroupNames(groupsForFour), groupsToGroupNames(db.getLoyaltyCardGroups(4)));
        assertEquals(groupsToGroupNames(groupsForFive), groupsToGroupNames(db.getLoyaltyCardGroups(5)));
        assertEquals(emptyGroup, db.getLoyaltyCardGroups(6));
        assertEquals(emptyGroup, db.getLoyaltyCardGroups(7));
        assertEquals(emptyGroup, db.getLoyaltyCardGroups(8));
        assertEquals(emptyGroup, db.getLoyaltyCardGroups(9));
        assertEquals(emptyGroup, db.getLoyaltyCardGroups(10));

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importExistingCardsNotReplace() throws IOException
    {
        final int NUM_CARDS = 10;

        addLoyaltyCards(NUM_CARDS);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export into CSV data
        boolean result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outStream, DataFormat.Catima);
        assertTrue(result);
        outStream.close();

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data on top of the existing database
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima);
        assertTrue(result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
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
            boolean result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outStream, DataFormat.Catima);
            assertTrue(result);

            TestHelpers.getEmptyDb(activity);

            // commons-csv would throw a RuntimeException if an entry was quotes but had
            // content after. For example:
            //   abc,def,""abc,abc
            //             ^ after the quote there should only be a , \n or EOF
            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat,\"\"a";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());

            // Attempt to import the data
            result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, format);
            assertEquals(false, result);

            assertEquals(0, db.getLoyaltyCardCount());

            TestHelpers.getEmptyDb(activity);
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
    @LooperMode(LooperMode.Mode.LEGACY)
    public void useImportExportTask() throws FileNotFoundException
    {
        final int NUM_CARDS = 10;

        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File exportFile = new File(sdcardDir, "Catima.csv");

        addLoyaltyCards(NUM_CARDS);

        TestTaskCompleteListener listener = new TestTaskCompleteListener();

        // Export to the file
        FileOutputStream fileOutputStream = new FileOutputStream(exportFile);
        ImportExportTask task = new ImportExportTask(activity, DataFormat.Catima, fileOutputStream, listener);
        task.execute();

        // Actually run the task to completion
        Robolectric.flushBackgroundThreadScheduler();

        // Check that the listener was executed
        assertNotNull(listener.success);
        assertEquals(true, listener.success);

        TestHelpers.getEmptyDb(activity);

        // Import everything back from the default location

        listener = new TestTaskCompleteListener();

        FileInputStream fileStream = new FileInputStream(exportFile);

        task = new ImportExportTask(activity, DataFormat.Catima, fileStream, listener);
        task.execute();

        // Actually run the task to completion
        Robolectric.flushBackgroundThreadScheduler();

        // Check that the listener was executed
        assertNotNull(listener.success);
        assertEquals(true, listener.success);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutColorsV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,0";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType);
        assertNull(card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutNullColorsV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,,,0";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType);
        assertNull(card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutInvalidColorsV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,type,not a number,invalid,0";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertEquals(false, result);
        assertEquals(0, db.getLoyaltyCardCount());

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithNoBarcodeTypeV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,,1,1,0";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertEquals(true, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(1, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithStarredFieldV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,1,1,1";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertEquals(true, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType);
        assertEquals(1, (long) card.headerColor);
        assertEquals(1, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithNoStarredFieldV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,1,1,";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertEquals(true, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType);
        assertEquals(1, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithInvalidStarFieldV1() throws IOException
    {
        String csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,1,1,2";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        csvText = "";
        csvText += DBHelper.LoyaltyCardDbIds.ID + "," +
                DBHelper.LoyaltyCardDbIds.STORE + "," +
                DBHelper.LoyaltyCardDbIds.NOTE + "," +
                DBHelper.LoyaltyCardDbIds.CARD_ID + "," +
                DBHelper.LoyaltyCardDbIds.BARCODE_TYPE + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.HEADER_TEXT_COLOR + "," +
                DBHelper.LoyaltyCardDbIds.STAR_STATUS + "\n";

        csvText += "1,store,note,12345,AZTEC,1,1,text";

        inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertTrue(result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType);
        assertEquals(1, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void exportV2() throws FileNotFoundException
    {
        db.insertGroup("Example");

        BitmapDrawable launcher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());
        BitmapDrawable roundLauncher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher_round, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());

        Bitmap frontImage = launcher.getBitmap();
        Bitmap backImage = roundLauncher.getBitmap();

        int loyaltyCard = (int) db.insertLoyaltyCard("Card 1", "Note 1", new Date(1618053234), new BigDecimal("100"), Currency.getInstance("USD"), "1234", "5432", BarcodeFormat.QR_CODE, 1, 0);

        Utils.saveCardImage(activity.getApplicationContext(), Utils.resizeBitmap(frontImage), loyaltyCard, true);
        Utils.saveCardImage(activity.getApplicationContext(), Utils.resizeBitmap(backImage), loyaltyCard, false);

        db.setLoyaltyCardGroups(loyaltyCard, Arrays.asList(db.getGroup("Example")));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);

        MultiFormatExporter.exportData(activity.getApplicationContext(), db, outputStreamWriter, DataFormat.Catima);

        String outputCsv = "2\r\n" +
                "\r\n" +
                "_id\r\n" +
                "Example\r\n" +
                "\r\n" +
                "_id,store,note,expiry,balance,balancetype,cardid,barcodeid,barcodetype,headercolor,starstatus,frontimage,backimage\r\n" +
                "1,Card 1,Note 1,1618053234,100,USD,1234,5432,QR_CODE,1,0,\"iVBORw0KGgoAAAANSUhEUgAAAgAAAAIAAQAAAADcA-lXAAAANklEQVR42u3BAQEAAACCIP-vbkhA\n" +
                "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB8G4IAAAFjdVCkAAAAAElFTkSuQmCC\n\",\"iVBORw0KGgoAAAANSUhEUgAAAgAAAAIAAQAAAADcA-lXAAAANklEQVR42u3BAQEAAACCIP-vbkhA\n" +
                "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB8G4IAAAFjdVCkAAAAAElFTkSuQmCC\n\"\r\n" +
                "\r\n" +
                "cardId,groupId\r\n" +
                "1,Example\r\n";

        assertEquals(outputCsv, outputStream.toString());
    }

    @Test
    public void importV2()
    {
        String csvText = "2\n" +
                "\n" +
                "_id\n" +
                "Health\n" +
                "Food\n" +
                "Fashion\n" +
                "\n" +
                "_id,store,note,expiry,balance,balancetype,cardid,barcodeid,headercolor,barcodetype,starstatus,frontimage,backimage\n" +
                "1,Card 1,Note 1,1618053234,100,USD,1234,5432,1,QR_CODE,0,\"iVBORw0KGgoAAAANSUhEUgAAAgAAAAIAAQAAAADcA-lXAAAANklEQVR42u3BAQEAAACCIP-vbkhA\n" +
                "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB8G4IAAAFjdVCkAAAAAElFTkSuQmCC\n\",\"iVBORw0KGgoAAAANSUhEUgAAAgAAAAIAAQAAAADcA-lXAAAANklEQVR42u3BAQEAAACCIP-vbkhA\n" +
                "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAB8G4IAAAFjdVCkAAAAAElFTkSuQmCC\n\"\r\n" +
                "8,Clothes Store,Note about store,,0,,a,,-5317,,0,,\n" +
                "2,Department Store,,1618041729,0,,A,,-9977996,,0,,\n" +
                "3,Grocery Store,,,150,,dhd,,-9977996,,0,,\n" +
                "4,Pharmacy,,,0,,dhshsvshs,,-10902850,,1,,\n" +
                "5,Restaurant,Note about restaurant here,,0,,98765432,23456,-10902850,CODE_128,0,,\n" +
                "6,Shoe Store,,,12.50,EUR,a,-5317,,AZTEC,0,,\n" +
                "\n" +
                "cardId,groupId\n" +
                "8,Fashion\n" +
                "3,Food\n" +
                "4,Health\n" +
                "5,Food\n" +
                "6,Fashion\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima);
        assertEquals(true, result);
        assertEquals(7, db.getLoyaltyCardCount());
        assertEquals(3, db.getGroupCount());

        // Check all groups
        Group healthGroup = db.getGroup("Health");
        assertNotNull(healthGroup);
        assertEquals(1, db.getGroupCardCount("Health"));
        assertEquals(Arrays.asList(4), db.getGroupCardIds("Health"));

        Group foodGroup = db.getGroup("Food");
        assertNotNull(foodGroup);
        assertEquals(2, db.getGroupCardCount("Food"));
        assertEquals(Arrays.asList(3, 5), db.getGroupCardIds("Food"));

        Group fashionGroup = db.getGroup("Fashion");
        assertNotNull(fashionGroup);
        assertEquals(2, db.getGroupCardCount("Fashion"));
        assertEquals(Arrays.asList(8, 6), db.getGroupCardIds("Fashion"));

        // Check all cards
        BitmapDrawable launcher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());
        BitmapDrawable roundLauncher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher_round, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());

        Bitmap frontImage = launcher.getBitmap();
        Bitmap backImage = roundLauncher.getBitmap();

        LoyaltyCard card1 = db.getLoyaltyCard(1);

        assertEquals("Card 1", card1.store);
        assertEquals("Note 1", card1.note);
        assertEquals(new Date(1618053234), card1.expiry);
        assertEquals(new BigDecimal("100"), card1.balance);
        assertEquals(Currency.getInstance("USD"), card1.balanceType);
        assertEquals("1234", card1.cardId);
        assertEquals("5432", card1.barcodeId);
        assertEquals(BarcodeFormat.QR_CODE, card1.barcodeType);
        assertEquals(1, (long) card1.headerColor);
        assertEquals(0, card1.starStatus);
        assertTrue(Utils.resizeBitmap(frontImage).sameAs(Utils.retrieveCardImage(activity.getApplicationContext(), card1.id, true)));
        assertTrue(Utils.resizeBitmap(backImage).sameAs(Utils.retrieveCardImage(activity.getApplicationContext(), card1.id, false)));

        LoyaltyCard card8 = db.getLoyaltyCard(8);

        assertEquals("Clothes Store", card8.store);
        assertEquals("Note about store", card8.note);
        assertEquals(null, card8.expiry);
        assertEquals(new BigDecimal("0"), card8.balance);
        assertEquals(null, card8.balanceType);
        assertEquals("a", card8.cardId);
        assertEquals(null, card8.barcodeId);
        assertEquals(null, card8.barcodeType);
        assertEquals(-5317, (long) card8.headerColor);
        assertEquals(0, card8.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card8.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card8.id, false));

        LoyaltyCard card2 = db.getLoyaltyCard(2);

        assertEquals("Department Store", card2.store);
        assertEquals("", card2.note);
        assertEquals(new Date(1618041729), card2.expiry);
        assertEquals(new BigDecimal("0"), card2.balance);
        assertEquals(null, card2.balanceType);
        assertEquals("A", card2.cardId);
        assertEquals(null, card2.barcodeId);
        assertEquals(null, card2.barcodeType);
        assertEquals(-9977996, (long) card2.headerColor);
        assertEquals(0, card2.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card2.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card2.id, false));

        LoyaltyCard card3 = db.getLoyaltyCard(3);

        assertEquals("Grocery Store", card3.store);
        assertEquals("", card3.note);
        assertEquals(null, card3.expiry);
        assertEquals(new BigDecimal("150"), card3.balance);
        assertEquals(null, card3.balanceType);
        assertEquals("dhd", card3.cardId);
        assertEquals(null, card3.barcodeId);
        assertEquals(null, card3.barcodeType);
        assertEquals(-9977996, (long) card3.headerColor);
        assertEquals(0, card3.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card3.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card3.id, false));

        LoyaltyCard card4 = db.getLoyaltyCard(4);

        assertEquals("Pharmacy", card4.store);
        assertEquals("", card4.note);
        assertEquals(null, card4.expiry);
        assertEquals(new BigDecimal("0"), card4.balance);
        assertEquals(null, card4.balanceType);
        assertEquals("dhshsvshs", card4.cardId);
        assertEquals(null, card4.barcodeId);
        assertEquals(null, card4.barcodeType);
        assertEquals(-10902850, (long) card4.headerColor);
        assertEquals(1, card4.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card4.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card4.id, false));

        LoyaltyCard card5 = db.getLoyaltyCard(5);

        assertEquals("Restaurant", card5.store);
        assertEquals("Note about restaurant here", card5.note);
        assertEquals(null, card5.expiry);
        assertEquals(new BigDecimal("0"), card5.balance);
        assertEquals(null, card5.balanceType);
        assertEquals("98765432", card5.cardId);
        assertEquals("23456", card5.barcodeId);
        assertEquals(BarcodeFormat.CODE_128, card5.barcodeType);
        assertEquals(-10902850, (long) card5.headerColor);
        assertEquals(0, card5.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card5.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card5.id, false));

        LoyaltyCard card6 = db.getLoyaltyCard(6);

        assertEquals("Shoe Store", card6.store);
        assertEquals("", card6.note);
        assertEquals(null, card6.expiry);
        assertEquals(new BigDecimal("12.50"), card6.balance);
        assertEquals(Currency.getInstance("EUR"), card6.balanceType);
        assertEquals("a", card6.cardId);
        assertEquals("-5317", card6.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card6.barcodeType);
        assertEquals(null, card6.headerColor);
        assertEquals(0, card6.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card6.id, true));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card6.id, false));

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importVoucherVault() throws IOException, FormatException, JSONException, ParseException {
        String jsonText = "[\n" +
                "  {\n" +
                "    \"uuid\": \"ae1ae525-3f27-481e-853a-8c30b7fa12d8\",\n" +
                "    \"description\": \"Clothes Store\",\n" +
                "    \"code\": \"123456\",\n" +
                "    \"codeType\": \"CODE128\",\n" +
                "    \"expires\": null,\n" +
                "    \"removeOnceExpired\": true,\n" +
                "    \"balance\": null,\n" +
                "    \"color\": \"GREY\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"uuid\": \"29a5d3b3-eace-4311-a15c-4c7e6a010531\",\n" +
                "    \"description\": \"Department Store\",\n" +
                "    \"code\": \"26846363\",\n" +
                "    \"codeType\": \"CODE39\",\n" +
                "    \"expires\": \"2021-03-26T00:00:00.000\",\n" +
                "    \"removeOnceExpired\": true,\n" +
                "    \"balance\": 3.5,\n" +
                "    \"color\": \"PURPLE\"\n" +
                "  }\n" +
                "]";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8));

        // Import the Voucher Vault data
        boolean result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.VoucherVault);
        assertTrue(result);
        assertEquals(2, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("Clothes Store", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(Currency.getInstance("USD"), card.balanceType);
        assertEquals("123456", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.CODE_128, card.barcodeType);
        assertEquals(Color.GRAY, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        card = db.getLoyaltyCard(2);

        assertEquals("Department Store", card.store);
        assertEquals("", card.note);
        assertEquals(new Date(1616716800000L), card.expiry);
        assertEquals(new BigDecimal("3.5"), card.balance);
        assertEquals(Currency.getInstance("USD"), card.balanceType);
        assertEquals("26846363", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.CODE_39, card.barcodeType);
        assertEquals(Color.rgb(128, 0, 128), (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }
}
