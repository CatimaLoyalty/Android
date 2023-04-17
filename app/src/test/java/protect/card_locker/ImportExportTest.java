package protect.card_locker;

import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.os.Looper;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import protect.card_locker.async.TaskHandler;
import protect.card_locker.importexport.DataFormat;
import protect.card_locker.importexport.ImportExportResult;
import protect.card_locker.importexport.ImportExportResultType;
import protect.card_locker.importexport.MultiFormatExporter;
import protect.card_locker.importexport.MultiFormatImporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
public class ImportExportTest {
    private Activity activity;
    private SQLiteDatabase mDatabase;

    private final String BARCODE_DATA = "428311627547";
    private final CatimaBarcode BARCODE_TYPE = CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A);

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;

        activity = Robolectric.setupActivity(MainActivity.class);
        mDatabase = TestHelpers.getEmptyDb(activity).getWritableDatabase();
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
            long id = DBHelper.insertLoyaltyCard(mDatabase, storeName, note, null, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0, null,0);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(cardsToAdd, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    private void addLoyaltyCardsFiveStarred() {
        int cardsToAdd = 9;
        // Add in reverse order to test sorting
        for (int index = cardsToAdd; index > 4; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            long id = DBHelper.insertLoyaltyCard(mDatabase, storeName, note, null, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 1, null,0);
            boolean result = (id != -1);
            assertTrue(result);
        }
        for (int index = cardsToAdd - 5; index > 0; index--) {
            String storeName = String.format("store, \"%4d", index);
            String note = String.format("note, \"%4d", index);
            //if index is even
            long id = DBHelper.insertLoyaltyCard(mDatabase, storeName, note, null, null, new BigDecimal(String.valueOf(index)), null, BARCODE_DATA, null, BARCODE_TYPE, index, 0, null,0);
            boolean result = (id != -1);
            assertTrue(result);
        }
        assertEquals(cardsToAdd, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    @Test
    public void addLoyaltyCardsWithExpiryNeverPastTodayFuture() {
        long id = DBHelper.insertLoyaltyCard(mDatabase, "No Expiry", "", null, null, new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null,0);
        boolean result = (id != -1);
        assertTrue(result);

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, (int) id);
        assertEquals("No Expiry", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = DBHelper.insertLoyaltyCard(mDatabase, "Past", "", null, new Date((long) 1), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null,0);
        result = (id != -1);
        assertTrue(result);

        card = DBHelper.getLoyaltyCard(mDatabase, (int) id);
        assertEquals("Past", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
        assertTrue(card.expiry.before(new Date()));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        id = DBHelper.insertLoyaltyCard(mDatabase, "Today", "", null, new Date(), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null,0);
        result = (id != -1);
        assertTrue(result);

        card = DBHelper.getLoyaltyCard(mDatabase, (int) id);
        assertEquals("Today", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
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
        id = DBHelper.insertLoyaltyCard(mDatabase, "Future", "", null, new Date(2147483648000L), new BigDecimal("0"), null, BARCODE_DATA, null, BARCODE_TYPE, 0, 0, null,0);
        result = (id != -1);
        assertTrue(result);

        card = DBHelper.getLoyaltyCard(mDatabase, (int) id);
        assertEquals("Future", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
        assertTrue(card.expiry.after(new Date(new Date().getTime() + 86400)));
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals(BARCODE_DATA, card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BARCODE_TYPE.format(), card.barcodeType.format());
        assertEquals(Integer.valueOf(0), card.headerColor);
        assertEquals(0, card.starStatus);

        assertEquals(4, DBHelper.getLoyaltyCardCount(mDatabase));
    }

    private void addGroups(int groupsToAdd) {
        // Add in reverse order to test sorting
        for (int index = groupsToAdd; index > 0; index--) {
            String groupName = String.format("group, \"%4d", index);
            long id = DBHelper.insertGroup(mDatabase, groupName);
            boolean result = (id != -1);
            assertTrue(result);
        }

        assertEquals(groupsToAdd, DBHelper.getGroupCount(mDatabase));
    }

    /**
     * Check that all of the cards follow the pattern
     * specified in addLoyaltyCards(), and are in sequential order
     * where the smallest card's index is 1
     */
    private void checkLoyaltyCards() {
        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        int index = 1;

        while (cursor.moveToNext()) {
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

            String expectedStore = String.format("store, \"%4d", index);
            String expectedNote = String.format("note, \"%4d", index);

            assertEquals(expectedStore, card.store);
            assertEquals(expectedNote, card.note);
            assertEquals(null, card.validFrom);
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
        Cursor cursor = DBHelper.getLoyaltyCardCursor(mDatabase);
        int index = 5;

        while (index < 10) {
            cursor.moveToNext();
            LoyaltyCard card = LoyaltyCard.toLoyaltyCard(cursor);

            String expectedStore = String.format("store, \"%4d", index);
            String expectedNote = String.format("note, \"%4d", index);

            assertEquals(expectedStore, card.store);
            assertEquals(expectedNote, card.note);
            assertEquals(null, card.validFrom);
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
            assertEquals(null, card.validFrom);
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
        Cursor cursor = DBHelper.getGroupCursor(mDatabase);
        int index = DBHelper.getGroupCount(mDatabase);

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
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());

        assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));

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
            ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, password);
            assertEquals(ImportExportResultType.Success, result.resultType());
            outStream.close();

            TestHelpers.getEmptyDb(activity);

            ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

            // Import the CSV data
            result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, DataFormat.Catima, password);
            assertEquals(ImportExportResultType.Success, result.resultType());

            assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));

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
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());

        assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));

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
        groupsForOne.add(DBHelper.getGroup(mDatabase, "group, \"   1"));

        List<Group> groupsForTwo = new ArrayList<>();
        groupsForTwo.add(DBHelper.getGroup(mDatabase, "group, \"   1"));
        groupsForTwo.add(DBHelper.getGroup(mDatabase, "group, \"   2"));

        List<Group> groupsForThree = new ArrayList<>();
        groupsForThree.add(DBHelper.getGroup(mDatabase, "group, \"   1"));
        groupsForThree.add(DBHelper.getGroup(mDatabase, "group, \"   2"));
        groupsForThree.add(DBHelper.getGroup(mDatabase, "group, \"   3"));

        List<Group> groupsForFour = new ArrayList<>();
        groupsForFour.add(DBHelper.getGroup(mDatabase, "group, \"   1"));
        groupsForFour.add(DBHelper.getGroup(mDatabase, "group, \"   2"));
        groupsForFour.add(DBHelper.getGroup(mDatabase, "group, \"   3"));

        List<Group> groupsForFive = new ArrayList<>();
        groupsForFive.add(DBHelper.getGroup(mDatabase,"group, \"   1"));
        groupsForFive.add(DBHelper.getGroup(mDatabase, "group, \"   3"));

        DBHelper.setLoyaltyCardGroups(mDatabase, 1, groupsForOne);
        DBHelper.setLoyaltyCardGroups(mDatabase, 2, groupsForTwo);
        DBHelper.setLoyaltyCardGroups(mDatabase, 3, groupsForThree);
        DBHelper.setLoyaltyCardGroups(mDatabase, 4, groupsForFour);
        DBHelper.setLoyaltyCardGroups(mDatabase, 5, groupsForFive);

        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        OutputStreamWriter outStream = new OutputStreamWriter(outData);

        // Export data to CSV format
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        outStream.close();

        TestHelpers.getEmptyDb(activity);

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());

        assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));
        assertEquals(NUM_GROUPS, DBHelper.getGroupCount(mDatabase));

        checkLoyaltyCards();
        checkGroups();

        assertEquals(groupsToGroupNames(groupsForOne), groupsToGroupNames(DBHelper.getLoyaltyCardGroups(mDatabase, 1)));
        assertEquals(groupsToGroupNames(groupsForTwo), groupsToGroupNames(DBHelper.getLoyaltyCardGroups(mDatabase, 2)));
        assertEquals(groupsToGroupNames(groupsForThree), groupsToGroupNames(DBHelper.getLoyaltyCardGroups(mDatabase, 3)));
        assertEquals(groupsToGroupNames(groupsForFour), groupsToGroupNames(DBHelper.getLoyaltyCardGroups(mDatabase, 4)));
        assertEquals(groupsToGroupNames(groupsForFive), groupsToGroupNames(DBHelper.getLoyaltyCardGroups(mDatabase, 5)));
        assertEquals(emptyGroup, DBHelper.getLoyaltyCardGroups(mDatabase, 6));
        assertEquals(emptyGroup, DBHelper.getLoyaltyCardGroups(mDatabase, 7));
        assertEquals(emptyGroup, DBHelper.getLoyaltyCardGroups(mDatabase, 8));
        assertEquals(emptyGroup, DBHelper.getLoyaltyCardGroups(mDatabase, 9));
        assertEquals(emptyGroup, DBHelper.getLoyaltyCardGroups(mDatabase, 10));

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
        ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        outStream.close();

        ByteArrayInputStream inData = new ByteArrayInputStream(outData.toByteArray());

        // Import the CSV data on top of the existing database
        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());

        assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void corruptedImportNothingSaved() {
        final int NUM_CARDS = 10;

        for (DataFormat format : DataFormat.values()) {
            addLoyaltyCards(NUM_CARDS);

            ByteArrayOutputStream outData = new ByteArrayOutputStream();
            OutputStreamWriter outStream = new OutputStreamWriter(outData);

            // Export data to CSV format
            ImportExportResult result = MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outData, DataFormat.Catima, null);
            assertEquals(ImportExportResultType.Success, result.resultType());

            TestHelpers.getEmptyDb(activity);

            // commons-csv would throw a RuntimeException if an entry was quotes but had
            // content after. For example:
            //   abc,def,""abc,abc
            //             ^ after the quote there should only be a , \n or EOF
            String corruptEntry = "ThisStringIsLikelyNotPartOfAnyFormat,\"\"a";

            ByteArrayInputStream inData = new ByteArrayInputStream((outData + corruptEntry).getBytes());

            // Attempt to import the data
            result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inData, format, null);
            assertEquals(ImportExportResultType.GenericFailure, result.resultType());

            assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));

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
        assertEquals(ImportExportResultType.Success, listener.result.resultType());

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
        assertEquals(ImportExportResultType.Success, listener.result.resultType());

        assertEquals(NUM_CARDS, DBHelper.getLoyaltyCardCount(mDatabase));

        checkLoyaltyCards();

        // Clear the database for the next format under test
        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithoutColorsV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_no_colors.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
    public void importWithoutNullColorsV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_empty_colors.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
    public void importWithoutInvalidColorsV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_invalid_colors.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.GenericFailure, result.resultType());
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));

        TestHelpers.getEmptyDb(activity);
    }

    @Test
    public void importWithNoBarcodeTypeV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_no_barcode_type.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
    public void importWithStarredFieldV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_starred_field.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
    public void importWithNoStarredFieldV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_no_starred_field.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
    public void importWithInvalidStarFieldV1() {
        InputStream inputStream = getClass().getResourceAsStream("catima_v1_invalid_starred_field.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        inputStream = getClass().getResourceAsStream("catima_v1_invalid_starred_field_2.csv");

        // Import the CSV data
        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(1, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("store", card.store);
        assertEquals("note", card.note);
        assertEquals(null, card.validFrom);
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
        Bitmap bitmap1 = new LetterBitmap(activity.getApplicationContext(), "1", "1", 12, 64, 64, Color.BLACK, Color.YELLOW).getLetterTile();
        Bitmap bitmap2 = new LetterBitmap(activity.getApplicationContext(), "2", "2", 12, 64, 64, Color.GREEN, Color.WHITE).getLetterTile();

        // Set up cards and groups
        HashMap<Integer, LoyaltyCard> loyaltyCardHashMap = new HashMap<>();
        HashMap<Integer, List<Group>> loyaltyCardGroups = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardFrontImages = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardBackImages = new HashMap<>();
        HashMap<Integer, Bitmap> loyaltyCardIconImages = new HashMap<>();

        // Create card 1
        int loyaltyCardId = (int) DBHelper.insertLoyaltyCard(mDatabase, "Card 1", "Note 1", new Date(1601510400), new Date(1618053234), new BigDecimal("100"), Currency.getInstance("USD"), "1234", "5432", CatimaBarcode.fromBarcode(BarcodeFormat.QR_CODE), 1, 0, null,0);
        loyaltyCardHashMap.put(loyaltyCardId, DBHelper.getLoyaltyCard(mDatabase, loyaltyCardId));
        DBHelper.insertGroup(mDatabase, "One");
        List<Group> groups = Arrays.asList(DBHelper.getGroup(mDatabase, "One"));
        DBHelper.setLoyaltyCardGroups(mDatabase, loyaltyCardId, groups);
        loyaltyCardGroups.put(loyaltyCardId, groups);
        Utils.saveCardImage(activity.getApplicationContext(), bitmap1, loyaltyCardId, ImageLocationType.front);
        Utils.saveCardImage(activity.getApplicationContext(), bitmap2, loyaltyCardId, ImageLocationType.back);
        Utils.saveCardImage(activity.getApplicationContext(), bitmap1, loyaltyCardId, ImageLocationType.icon);
        loyaltyCardFrontImages.put(loyaltyCardId, bitmap1);
        loyaltyCardBackImages.put(loyaltyCardId, bitmap2);
        loyaltyCardIconImages.put(loyaltyCardId, bitmap1);

        // Create card 2
        loyaltyCardId = (int) DBHelper.insertLoyaltyCard(mDatabase, "Card 2", "", null, null, new BigDecimal(0), null, "123456", null, null, 2, 1, null,0);
        loyaltyCardHashMap.put(loyaltyCardId, DBHelper.getLoyaltyCard(mDatabase, loyaltyCardId));

        // Export everything
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MultiFormatExporter.exportData(activity.getApplicationContext(), mDatabase, outputStream, DataFormat.Catima, null);

        // Wipe database
        TestHelpers.getEmptyDb(activity);

        // Import everything
        MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, new ByteArrayInputStream(outputStream.toByteArray()), DataFormat.Catima, null);

        // Ensure everything is there
        assertEquals(loyaltyCardHashMap.size(), DBHelper.getLoyaltyCardCount(mDatabase));

        for (Integer loyaltyCardID : loyaltyCardHashMap.keySet()) {
            LoyaltyCard loyaltyCard = loyaltyCardHashMap.get(loyaltyCardID);

            LoyaltyCard dbLoyaltyCard = DBHelper.getLoyaltyCard(mDatabase, loyaltyCardID);

            assertEquals(loyaltyCard.id, dbLoyaltyCard.id);
            assertEquals(loyaltyCard.store, dbLoyaltyCard.store);
            assertEquals(loyaltyCard.note, dbLoyaltyCard.note);
            assertEquals(loyaltyCard.validFrom, dbLoyaltyCard.validFrom);
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
                            DBHelper.getLoyaltyCardGroups(
                                    mDatabase,
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
        InputStream inputStream = getClass().getResourceAsStream("catima_v2.csv");

        // Import the CSV data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Catima, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(7, DBHelper.getLoyaltyCardCount(mDatabase));
        assertEquals(3, DBHelper.getGroupCount(mDatabase));

        // Check all groups
        Group healthGroup = DBHelper.getGroup(mDatabase, "Health");
        assertNotNull(healthGroup);
        assertEquals(1, DBHelper.getGroupCardCount(mDatabase, "Health"));
        assertEquals(Arrays.asList(4), DBHelper.getGroupCardIds(mDatabase, "Health"));

        Group foodGroup = DBHelper.getGroup(mDatabase, "Food");
        assertNotNull(foodGroup);
        assertEquals(2, DBHelper.getGroupCardCount(mDatabase, "Food"));
        assertEquals(Arrays.asList(3, 5), DBHelper.getGroupCardIds(mDatabase, "Food"));

        Group fashionGroup = DBHelper.getGroup(mDatabase, "Fashion");
        assertNotNull(fashionGroup);
        assertEquals(2, DBHelper.getGroupCardCount(mDatabase, "Fashion"));
        assertEquals(Arrays.asList(8, 6), DBHelper.getGroupCardIds(mDatabase, "Fashion"));

        // Check all cards
        LoyaltyCard card1 = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("Card 1", card1.store);
        assertEquals("Note 1", card1.note);
        assertEquals(new Date(1601510400), card1.validFrom);
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

        LoyaltyCard card8 = DBHelper.getLoyaltyCard(mDatabase, 8);

        assertEquals("Clothes Store", card8.store);
        assertEquals("Note about store", card8.note);
        assertEquals(null, card8.validFrom);
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

        LoyaltyCard card2 = DBHelper.getLoyaltyCard(mDatabase, 2);

        assertEquals("Department Store", card2.store);
        assertEquals("", card2.note);
        assertEquals(null, card2.validFrom);
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

        LoyaltyCard card3 = DBHelper.getLoyaltyCard(mDatabase, 3);

        assertEquals("Grocery Store", card3.store);
        assertEquals("Multiline note about grocery store\n\nwith blank line", card3.note);
        assertEquals(null, card3.validFrom);
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

        LoyaltyCard card4 = DBHelper.getLoyaltyCard(mDatabase, 4);

        assertEquals("Pharmacy", card4.store);
        assertEquals("", card4.note);
        assertEquals(null, card4.validFrom);
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

        LoyaltyCard card5 = DBHelper.getLoyaltyCard(mDatabase, 5);

        assertEquals("Restaurant", card5.store);
        assertEquals("Note about restaurant here", card5.note);
        assertEquals(null, card5.validFrom);
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

        LoyaltyCard card6 = DBHelper.getLoyaltyCard(mDatabase, 6);

        assertEquals("Shoe Store", card6.store);
        assertEquals("", card6.note);
        assertEquals(null, card6.validFrom);
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
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Fidme, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(3, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("Hema", card.store);
        assertEquals("2021-03-24 18:35:08 UTC", card.note);
        assertEquals(null, card.validFrom);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("82825292629272726", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(0, card.starStatus);

        card = DBHelper.getLoyaltyCard(mDatabase, 2);

        assertEquals("test", card.store);
        assertEquals("Test\n2021-03-24 18:34:19 UTC", card.note);
        assertEquals(null, card.validFrom);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(null, card.balanceType);
        assertEquals("123456", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(null, card.barcodeType);
        assertEquals(0, card.starStatus);

        card = DBHelper.getLoyaltyCard(mDatabase, 3);

        assertEquals("Albert Heijn", card.store);
        assertEquals("Bonus Kaart\n2021-03-24 16:47:47 UTC\nFirst Last", card.note);
        assertEquals(null, card.validFrom);
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
    public void importStocard() {
        // FIXME: The provided stocard.zip is a very old export (8 July 2021) manually edited to
        // look more like the Stocard files provided by users for #1242. It is not an up-to-date
        // export and the test is possibly unreliable. This should be replaced by an up-to-date
        // export.
        InputStream inputStream = getClass().getResourceAsStream("stocard.zip");

        // Import the Stocard data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Stocard, null);
        assertEquals(ImportExportResultType.BadPassword, result.resultType());
        assertEquals(0, DBHelper.getLoyaltyCardCount(mDatabase));

        inputStream = getClass().getResourceAsStream("stocard.zip");

        result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.Stocard, "da811b40a4dac56f0cbb2d99b21bbb9a".toCharArray());
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(3, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("GAMMA", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
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

        card = DBHelper.getLoyaltyCard(mDatabase, 2);

        assertEquals("Air Miles", card.store);
        assertEquals("szjsbs", card.note);
        assertEquals(null, card.validFrom);
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

        card = DBHelper.getLoyaltyCard(mDatabase, 3);

        assertEquals("jö", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
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
    public void importVoucherVault() {
        InputStream inputStream = getClass().getResourceAsStream("vouchervault.json");

        // Import the Voucher Vault data
        ImportExportResult result = MultiFormatImporter.importData(activity.getApplicationContext(), mDatabase, inputStream, DataFormat.VoucherVault, null);
        assertEquals(ImportExportResultType.Success, result.resultType());
        assertEquals(2, DBHelper.getLoyaltyCardCount(mDatabase));

        LoyaltyCard card = DBHelper.getLoyaltyCard(mDatabase, 1);

        assertEquals("Clothes Store", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
        assertEquals(null, card.expiry);
        assertEquals(new BigDecimal("0"), card.balance);
        assertEquals(Currency.getInstance("USD"), card.balanceType);
        assertEquals("123456", card.cardId);
        assertEquals(null, card.barcodeId);
        assertEquals(BarcodeFormat.CODE_128, card.barcodeType.format());
        assertEquals(Color.GRAY, (long) card.headerColor);
        assertEquals(0, card.starStatus);

        card = DBHelper.getLoyaltyCard(mDatabase, 2);

        assertEquals("Department Store", card.store);
        assertEquals("", card.note);
        assertEquals(null, card.validFrom);
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
