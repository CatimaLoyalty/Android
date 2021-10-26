package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Looper;
import android.util.DisplayMetrics;

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
import org.robolectric.shadows.ShadowLooper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import androidx.core.content.res.ResourcesCompat;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class ImportExportTest {
    private Activity activity;
    private DBHelper db;
    private long nowMs;
    private long lastYearMs;
    private final int MONTHS_PER_YEAR = 12;

    private final String BARCODE_DATA = "428311627547";
    private final CatimaBarcode BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A);

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(MainActivity.class);
        db = TestHelpers.getEmptyDb(activity);
        nowMs = System.currentTimeMillis();

        Calendar lastYear = Calendar.getInstance();
        lastYear.set(Calendar.YEAR, lastYear.get(Calendar.YEAR) - 1);
        lastYearMs = lastYear.getTimeInMillis();
    }

    /**
     * Add the given number of cards, each with
     * an index in the store name.
     *
     * @param cardsToAdd
     */
    private void addLoyaltyCards(int cardsToAdd) {
        // Add in reverse order to test sorting
        for (int index = cardsToAdd; index > 0; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0, null);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(cardsToAdd, db.getLoyaltyCardCount());
    }

    private void addLoyaltyCardsFiveStarred() {
        int cardsToAdd = 9;
        // Add in reverse order to test sorting
        for (int index = cardsToAdd; index > 4; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 1, null);
            boolean result = (id != -1);
            assertTrue(result);
        }
        for (int index = cardsToAdd - 5; index > 0; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            //if index is even
            long id = db.insertLoyaltyCard(storeName, note, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0, null);
            boolean result = (id != -1);
            assertTrue(result);
        }
        assertEquals(cardsToAdd, db.getLoyaltyCardCount());
    }

    @Test
    public void addLoyaltyCardsWithExpiryNeverPastTodayFuture() {
        long id = db.insertLoyaltyCard("No Expiry", "", null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null);
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
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = db.insertLoyaltyCard("Past", "", new Date((long) 1), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null);
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
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = db.insertLoyaltyCard("Today", "", new Date(), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null);
        result = (id != -1);
        assertTrue(result);

        card = db.getLoyaltyCard((int) id);
        assertEquals("Today", card.store);
        assertEquals("", card.note);
        assertTrue(card.expiry.before(new Date(new Date().getTime() + 86400)));
        assertTrue(card.expiry.after(new Date(new Date().getTime() - 86400)));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        // This will break after 19 January 2038
        // If someone is still maintaining this code base by then: I love you
        id = db.insertLoyaltyCard("Future", "", new Date(2147483648000L), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null);
        result = (id != -1);
        assertTrue(result);

        card = db.getLoyaltyCard((int) id);
        assertEquals("Future", card.store);
        assertEquals("", card.note);
        assertTrue(card.expiry.after(new Date(new Date().getTime() + 86400)));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        assertEquals(4, db.getLoyaltyCardCount());
    }

    private void addGroups(int groupsToAdd) {
        // Add in reverse order to test sorting
        for (int index = groupsToAdd; index > 0; index--) {
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
    private void checkLoyaltyCards() {
        Cursor cursor = db.getLoyaltyCardCursor();
        int index = 1;

        while (cursor.moveToNext()) {
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
            assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
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
    private void checkLoyaltyCardsFiveStarred() {
        Cursor cursor = db.getLoyaltyCardCursor();
        int index = 5;

        while (index < 10) {
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
            assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
            assertEquals(Integer.valueOf(index), card.headerColor);
            assertEquals(1, card.starStatus);

            index++;
        }

        index = 1;
        while (cursor.moveToNext() && index < 5) {
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
            assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
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
    private void checkGroups() {
        Cursor cursor = db.getGroupCursor();
        int index = db.getGroupCount();

        while (cursor.moveToNext()) {
            Group group = Group.toGroup(cursor);

            String expectedGroupName = String.format("group, \"%4d", index);

            assertEquals(expectedGroupName, group._id);

            index--;
        }
        cursor.close();
    }

    @Test
    public void multipleCardsExportImport() throws IOException {
        final int NUM_CARDS = 10;

        addLoyaltyCards(NUM_CARDS);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    public void multipleCardsExportImportPasswordProtected() throws IOException {
        final int NUM_CARDS = 10;
        List<char[]> passwords = Arrays.asList(null, "123456789".toCharArray());
        for (char[] password : passwords) {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, password);
            assertEquals(ImportExportResult.Success, result);
            outStream.close();

            TestHelpers.getEmptyDb(activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            // Import the CSV data
            result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima, password);
            assertEquals(ImportExportResult.Success, result);

            assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

            checkLoyaltyCards();

            // Clear the database for the next format under test
            TestHelpers.getEmptyDb(activity);
        }

    }

    @Test
    public void multipleCardsExportImportSomeStarred() throws IOException {
        final int NUM_CARDS = 9;

        addLoyaltyCardsFiveStarred();

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCardsFiveStarred();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    private List<String> groupsToGroupNames(List<Group> groups) {
        List<String> groupNames = new ArrayList<>();

        for (Group group : groups) {
            groupNames.add(group._id);
        }

        return groupNames;
    }

    @Test
    public void multipleCardsExportImportWithGroups() throws IOException {
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
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);

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
    public void importExistingCardsNotReplace() throws IOException {
        final int NUM_CARDS = 10;

        addLoyaltyCards(NUM_CARDS);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export into CSV data
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        outStream.close();

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data on top of the existing database
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void corruptedImportNothingSaved() throws IOException {
        final int NUM_CARDS = 10;

        for (DataFormat format : DataFormat.values()) {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), db, outData, DataFormat.Catima, null);
            assertEquals(ImportExportResult.Success, result);

            TestHelpers.getEmptyDb(activity);

            // commons-csv would throw a RuntimeException if an entry was quotes but had
            // content after. For example:
            //   abc,def,""abc,abc
            //             ^ after the quote there should only be a , \n or EOF
            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat,\"\"a";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData.toString() + corruptEntry).getBytes());

            // Attempt to import the data
            result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inData, format, null);
            assertEquals(ImportExportResult.GenericFailure, result);

            assertEquals(0, db.getLoyaltyCardCount());

            TestHelpers.getEmptyDb(activity);
        }
    }

    class TestTaskCompleteListener implements ImportExportTask.TaskCompleteListener {
        ImportExportResult result;

        public void onTaskComplete(ImportExportResult result, DataFormat dataFormat) {
            this.result = result;
        }
    }

    @Test
    @LooperMode(LooperMode.Mode.PAUSED)
    public void useImportExportTask() throws FileNotFoundException {
        final int NUM_CARDS = 10;

        final File sdcardDir = Environment.getExternalStorageDirectory();
        final File exportFile = new File(sdcardDir, "Catima.csv");

        addLoyaltyCards(NUM_CARDS);

        TestTaskCompleteListener listener = new TestTaskCompleteListener();

        // Export to the file
        final String password = "123456789";
        FileOutputStream fileOutputStream = new FileOutputStream(exportFile);
        ImportExportTask task = new ImportExportTask(activity, DataFormat.Catima, fileOutputStream, password.toCharArray(), listener);
        TaskHandler mTasks = new TaskHandler();
        mTasks.executeTask(TaskHandler.TYPE.EXPORT, task);

        // Actually run the task to completion
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, false, false, true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();


        // Check that the listener was executed
        assertNotNull(listener.result);
        assertEquals(ImportExportResult.Success, listener.result);

        TestHelpers.getEmptyDb(activity);

        // Import everything back from the default location

        listener = new TestTaskCompleteListener();

        FileInputStream fileStream = new FileInputStream(exportFile);

        task = new ImportExportTask(activity, DataFormat.Catima, fileStream, password.toCharArray(), listener);
        mTasks.executeTask(TaskHandler.TYPE.IMPORT, task);

        // Actually run the task to completion
        // I am CONVINCED there must be a better way than to wait on this Queue with a flush.
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, false, false, true);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5000));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Check that the listener was executed
        assertNotNull(listener.result);
        assertEquals(ImportExportResult.Success, listener.result);

        assertEquals(NUM_CARDS, db.getLoyaltyCardCount());

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutColorsV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType.format());
        assertNull(card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutNullColorsV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType.format());
        assertNull(card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutInvalidColorsV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.GenericFailure, result);
        assertEquals(0, db.getLoyaltyCardCount());

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithNoBarcodeTypeV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
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
    public void importWithStarredFieldV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType.format());
        assertEquals(1, (long) card.headerColor);
        assertEquals(1, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithNoStarredFieldV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType.format());
        assertEquals(1, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithInvalidStarFieldV1() throws IOException {
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
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
        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(1, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("12345", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card.barcodeType.format());
        assertEquals(1, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void exportImportV2Zip() throws FileNotFoundException {
        // Prepare images
        BitmapDrawable launcher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());
        BitmapDrawable roundLauncher = (BitmapDrawable) ResourcesCompat.getDrawableForDensity(activity.getResources(), R.mipmap.ic_launcher_round, DisplayMetrics.DENSITY_XXXHIGH, activity.getTheme());

        Bitmap launcherBitmap = launcher.getBitmap();
        Bitmap roundLauncherBitmap = roundLauncher.getBitmap();

        // Set up cards and groups
        HashMap<Integer, LoyaltyCard> loyaltyCardHashMap = new HashMap<>();
        HashMap<Integer, List<Group>> loyaltyCardGroups = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardFrontImages = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardBackImages = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardIconImages = new HashMap<>();

        // Create card 1
        int loyaltyCardId = (int) db.insertLoyaltyCard("Card 1", "Note 1", new Date(1618053234), new BigDecimal("100"), Currency.getInstance("USD"), "1234", "5432", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE), 1, 0, null);
        loyaltyCardHashMap.put(loyaltyCardId, db.getLoyaltyCard(loyaltyCardId));
        db.insertGroup("One");
        List<Group> groups = Arrays.asList(db.getGroup("One"));
        db.setLoyaltyCardGroups(loyaltyCardId, groups);
        loyaltyCardGroups.put(loyaltyCardId, groups);
        Utils.saveCardImage(activity.getApplicationContext(), launcherBitmap, loyaltyCardId, ImageLocationType.front);
        Utils.saveCardImage(activity.getApplicationContext(), roundLauncherBitmap, loyaltyCardId, ImageLocationType.back);
        Utils.saveCardImage(activity.getApplicationContext(), launcherBitmap, loyaltyCardId, ImageLocationType.icon);
        loyaltyCardFrontImages.put(loyaltyCardId, launcherBitmap);
        loyaltyCardBackImages.put(loyaltyCardId, roundLauncherBitmap);
        loyaltyCardIconImages.put(loyaltyCardId, launcherBitmap);

        // Create card 2
        loyaltyCardId = (int) db.insertLoyaltyCard("Card 2", "", null, new BigDecimal(0), null, "123456", null, null, 2, 1, null);
        loyaltyCardHashMap.put(loyaltyCardId, db.getLoyaltyCard(loyaltyCardId));

        // Export everything
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MultiFormatExporter.exportData(activity.getApplicationContext(), db, outputStream, DataFormat.Catima, null);

        // Wipe database
        TestHelpers.getEmptyDb(activity);

        // Import everything
        MultiFormatImporter.importData(activity.getApplicationContext(), db, new ByteArrayInputStream(outputStream.toByteArray()), DataFormat.Catima, null);

        // Ensure everything is there
        assertEquals(loyaltyCardHashMap.size(), db.getLoyaltyCardCount());

        for (Integer loyaltyCardID : loyaltyCardHashMap.keySet()) {
            LoyaltyCard loyaltyCard = loyaltyCardHashMap.get(loyaltyCardID);

            LoyaltyCard dbLoyaltyCard = db.getLoyaltyCard(loyaltyCardID);

            assertEquals(loyaltyCard.id, dbLoyaltyCard.id);
            assertEquals(loyaltyCard.store, dbLoyaltyCard.store);
            assertEquals(loyaltyCard.note, dbLoyaltyCard.note);
            assertEquals(loyaltyCard.expiry, dbLoyaltyCard.expiry);
            assertEquals(loyaltyCard.balance, dbLoyaltyCard.balance);
            assertEquals(loyaltyCard.cardId, dbLoyaltyCard.cardId);
            assertEquals(loyaltyCard.barcodeId, dbLoyaltyCard.barcodeId);
            assertEquals(loyaltyCard.starStatus, dbLoyaltyCard.starStatus);
            assertEquals(loyaltyCard.barcodeType != null ? loyaltyCard.barcodeType.format() : null, dbLoyaltyCard.barcodeType != null ? dbLoyaltyCard.barcodeType.format() : null);
            assertEquals(loyaltyCard.balanceType, dbLoyaltyCard.balanceType);
            assertEquals(loyaltyCard.headerColor, dbLoyaltyCard.headerColor);

            List<Group> emptyGroup = new ArrayList<>();

            assertEquals(
                    groupsToGroupNames(
                            (List<Group>) Utils.mapGetOrDefault(
                                    loyaltyCardGroups,
                                    loyaltyCardID,
                                    emptyGroup
                            )
                    ),
                    groupsToGroupNames(
                            db.getLoyaltyCardGroups(
                                    loyaltyCardID
                            )
                    )
            );

            Bitmap expectedFrontImage = loyaltyCardFrontImages.get(loyaltyCardID);
            Bitmap expectedBackImage = loyaltyCardBackImages.get(loyaltyCardID);
            Bitmap expectedIconImage = loyaltyCardIconImages.get(loyaltyCardID);
            Bitmap actualFrontImage = Utils.retrieveCardImage(activity.getApplicationContext(), Utils.getCardImageFileName(loyaltyCardID, ImageLocationType.front));
            Bitmap actualBackImage = Utils.retrieveCardImage(activity.getApplicationContext(), Utils.getCardImageFileName(loyaltyCardID, ImageLocationType.back));
            Bitmap actualIconImage = Utils.retrieveCardImage(activity.getApplicationContext(), Utils.getCardImageFileName(loyaltyCardID, ImageLocationType.icon));

            if (expectedFrontImage != null) {
                assertTrue(expectedFrontImage.sameAs(actualFrontImage));
            } else {
                assertNull(actualFrontImage);
            }

            if (expectedBackImage != null) {
                assertTrue(expectedBackImage.sameAs(actualBackImage));
            } else {
                assertNull(actualBackImage);
            }

            if (expectedIconImage != null) {
                assertTrue(expectedIconImage.sameAs(actualIconImage));
            } else {
                assertNull(actualIconImage);
            }
        }
    }

    @Test
    public void importV2CSV() {
        String csvText = "2\n" +
                "\n" +
                "_id\n" +
                "Health\n" +
                "Food\n" +
                "Fashion\n" +
                "\n" +
                "_id,store,note,expiry,balance,balancetype,cardid,barcodeid,headercolor,barcodetype,starstatus\n" +
                "1,Card 1,Note 1,1618053234,100,USD,1234,5432,1,QR_CODE,0,\r\n" +
                "8,Clothes Store,Note about store,,0,,a,,-5317,,0,\n" +
                "2,Department Store,,1618041729,0,,A,,-9977996,,0,\n" +
                "3,Grocery Store,\"Multiline note about grocery store\n" +
                "\n" +
                "with blank line\",,150,,dhd,,-9977996,,0,\n" +
                "4,Pharmacy,,,0,,dhshsvshs,,-10902850,,1,\n" +
                "5,Restaurant,Note about restaurant here,,0,,98765432,23456,-10902850,CODE_128,0,\n" +
                "6,Shoe Store,,,12.50,EUR,a,-5317,,AZTEC,0,\n" +
                "\n" +
                "cardId,groupId\n" +
                "8,Fashion\n" +
                "3,Food\n" +
                "4,Health\n" +
                "5,Food\n" +
                "6,Fashion\n";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvText.getBytes(StandardCharsets.UTF_8));

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResult.Success, result);
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
        LoyaltyCard card1 = db.getLoyaltyCard(1);

        assertEquals("Card 1", card1.store);
        assertEquals("Note 1", card1.note);
        assertEquals(new Date(1618053234), card1.expiry);
        assertEquals(new BigDecimal("100"), card1.balance);
        assertEquals(Currency.getInstance("USD"), card1.balanceType);
        assertEquals("1234", card1.cardId);
        assertEquals("5432", card1.barcodeId);
        assertEquals(BarcodeFormat.QR_CODE, card1.barcodeType.format());
        assertEquals(1, (long) card1.headerColor);
        assertEquals(0, card1.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card1.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card1.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card1.id, ImageLocationType.icon));

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
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card8.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card8.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card8.id, ImageLocationType.icon));

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
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card2.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card2.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card2.id, ImageLocationType.icon));

        LoyaltyCard card3 = db.getLoyaltyCard(3);

        assertEquals("Grocery Store", card3.store);
        assertEquals("Multiline note about grocery store\n\nwith blank line", card3.note);
        assertEquals(null, card3.expiry);
        assertEquals(new BigDecimal("150"), card3.balance);
        assertEquals(null, card3.balanceType);
        assertEquals("dhd", card3.cardId);
        assertEquals(null, card3.barcodeId);
        assertEquals(null, card3.barcodeType);
        assertEquals(-9977996, (long) card3.headerColor);
        assertEquals(0, card3.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card3.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card3.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card3.id, ImageLocationType.icon));

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
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card4.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card4.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card4.id, ImageLocationType.icon));

        LoyaltyCard card5 = db.getLoyaltyCard(5);

        assertEquals("Restaurant", card5.store);
        assertEquals("Note about restaurant here", card5.note);
        assertEquals(null, card5.expiry);
        assertEquals(new BigDecimal("0"), card5.balance);
        assertEquals(null, card5.balanceType);
        assertEquals("98765432", card5.cardId);
        assertEquals("23456", card5.barcodeId);
        assertEquals(BarcodeFormat.CODE_128, card5.barcodeType.format());
        assertEquals(-10902850, (long) card5.headerColor);
        assertEquals(0, card5.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card5.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card5.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card5.id, ImageLocationType.icon));

        LoyaltyCard card6 = db.getLoyaltyCard(6);

        assertEquals("Shoe Store", card6.store);
        assertEquals("", card6.note);
        assertEquals(null, card6.expiry);
        assertEquals(new BigDecimal("12.50"), card6.balance);
        assertEquals(Currency.getInstance("EUR"), card6.balanceType);
        assertEquals("a", card6.cardId);
        assertEquals("-5317", card6.barcodeId);
        assertEquals(BarcodeFormat.AZTEC, card6.barcodeType.format());
        assertEquals(null, card6.headerColor);
        assertEquals(0, card6.starStatus);
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card6.id, ImageLocationType.front));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card6.id, ImageLocationType.back));
        assertEquals(null, Utils.retrieveCardImage(activity.getApplicationContext(), card6.id, ImageLocationType.icon));

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importFidme() {
        InputStream inputStream = getClass().getResourceAsStream("fidme.zip");

        // Import the Fidme data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Fidme, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(3, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("Hema", card.store);
        assertEquals("2021-03-24 18:35:08 UTC", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("82825292629272726", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(0, card.starStatus);

        card = db.getLoyaltyCard(2);

        assertEquals("test", card.store);
        assertEquals("Test\n2021-03-24 18:34:19 UTC", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("123456", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(0, card.starStatus);

        card = db.getLoyaltyCard(3);

        assertEquals("Albert Heijn", card.store);
        assertEquals("Bonus Kaart\n2021-03-24 16:47:47 UTC\nFirst Last", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("123435363634", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importStocard() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("stocard.zip");

        // Import the Stocard data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Stocard, null);
        assertEquals(ImportExportResult.BadPassword, result);
        assertEquals(0, db.getLoyaltyCardCount());

        inputStream = getClass().getResourceAsStream("stocard.zip");

        result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.Stocard, "da811b40a4dac56f0cbb2d99b21bbb9a".toCharArray());
        assertEquals(ImportExportResult.Success, result);
        assertEquals(3, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("GAMMA", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("55555", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.EAN_13, card.barcodeType.format());
        assertEquals(0, card.starStatus);

        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 1, ImageLocationType.front));
        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 1, ImageLocationType.back));
        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 1, ImageLocationType.icon));

        card = db.getLoyaltyCard(2);

        assertEquals("Air Miles", card.store);
        assertEquals("szjsbs", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("7649484", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.EAN_13, card.barcodeType.format());
        assertEquals(0, card.starStatus);

        assertTrue(BitmapFactory.decodeStream(getClass().getResourceAsStream("stocard-front.jpg")).sameAs(Utils.retrieveCardImage(activity.getApplicationContext(), 2, ImageLocationType.front)));
        assertTrue(BitmapFactory.decodeStream(getClass().getResourceAsStream("stocard-back.jpg")).sameAs(Utils.retrieveCardImage(activity.getApplicationContext(), 2, ImageLocationType.back)));
        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 2, ImageLocationType.icon));

        card = db.getLoyaltyCard(3);

        // I don't think we can know this one, but falling back to an unique store name is at least something
        assertEquals("63536738-d64b-48ae-aeb8-82761523fa67", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("(01)09010374000019(21)02097564604859211217(10)01231287693", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.RSS_EXPANDED, card.barcodeType.format());
        assertEquals(0, card.starStatus);

        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 3, ImageLocationType.front));
        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 3, ImageLocationType.back));
        assertNull(Utils.retrieveCardImage(activity.getApplicationContext(), 3, ImageLocationType.icon));

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
                "    \"balanceMilliunits\": null,\n" +
                "    \"color\": \"GREY\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"uuid\": \"29a5d3b3-eace-4311-a15c-4c7e6a010531\",\n" +
                "    \"description\": \"Department Store\",\n" +
                "    \"code\": \"26846363\",\n" +
                "    \"codeType\": \"CODE39\",\n" +
                "    \"expires\": \"2021-03-26T00:00:00.000\",\n" +
                "    \"removeOnceExpired\": true,\n" +
                "    \"balance\": null,\n" +
                "    \"balanceMilliunits\": 3500,\n" +
                "    \"color\": \"PURPLE\"\n" +
                "  }\n" +
                "]";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8));

        // Import the Voucher Vault data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), db, inputStream, DataFormat.VoucherVault, null);
        assertEquals(ImportExportResult.Success, result);
        assertEquals(2, db.getLoyaltyCardCount());

        LoyaltyCard card = db.getLoyaltyCard(1);

        assertEquals("Clothes Store", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(Currency.getInstance("USD"), card.balanceType);
        assertEquals("123456", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.CODE_128, card.barcodeType.format());
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
        assertEquals(BarcodeFormat.CODE_39, card.barcodeType.format());
        assertEquals(Color.rgb(128, 0, 128), (long) card.headerColor);
        assertEquals(0, card.starStatus);

        TestHelpers.getEmptyDb(activity);
    }
}
