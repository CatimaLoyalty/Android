package protect.card_locker.contentprovider;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import protect.card_locker.CatimaBarcode;
import protect.card_locker.DBHelper;
import protect.card_locker.Group;
import protect.card_locker.TestHelpers;

@RunWith(RobolectricTestRunner.class)
public class CardsContentProviderTest {
    private ContentResolver mResolver;
    private DBHelper dbHelper;
    private SQLiteDatabase mDatabase;


    @Before
    public void setUp() {
        final ContentProvider contentProvider = new CardsContentProvider();
        final ProviderInfo providerInfo = new ProviderInfo();
        providerInfo.authority = CardsContentProvider.AUTHORITY;
        contentProvider.attachInfo(RuntimeEnvironment.getApplication(), providerInfo);
        contentProvider.onCreate();
        Robolectric.buildContentProvider(CardsContentProvider.class).create(providerInfo);

        mResolver = RuntimeEnvironment.getApplication().getContentResolver();
        dbHelper = TestHelpers.getEmptyDb(RuntimeEnvironment.getApplication());
        mDatabase = dbHelper.getWritableDatabase();
    }

    @After
    public void cleanup() {
        mDatabase.close();
        dbHelper.close();
    }

    @Test
    public void testVersion() {
        final Uri versionUri = getUri("version");

        try (Cursor cursor = mResolver.query(versionUri, null, null, null)) {
            assertEquals("number of entries", 1, cursor.getCount());
            assertEquals("number of columns", 2, cursor.getColumnCount());
            assertArrayEquals("column names", new String[]{"major", "minor"}, cursor.getColumnNames());
            cursor.moveToNext();
            assertEquals("major version", 1, cursor.getInt(cursor.getColumnIndexOrThrow("major")));
            assertEquals("minor version", 0, cursor.getInt(cursor.getColumnIndexOrThrow("minor")));
        }
    }

    @Test
    public void testCards() {
        final Uri cardsUri = getUri("cards");

        try (Cursor cursor = mResolver.query(cardsUri, null, null, null)) {
            assertEquals(cursor.getCount(), 0);
        }

        final String store = "the best store";
        final String note = "this is a note";
        final Instant validFrom = Instant.ofEpochMilli(1687112209000L);
        final Instant expiry = Instant.ofEpochMilli(1687112277000L);
        final BigDecimal balance = new BigDecimal("123.20");
        final Currency balanceType = Currency.getInstance("EUR");
        final String cardId = "a-card-id";
        final String barcodeId = "barcode-id";
        final CatimaBarcode barcodeType = CatimaBarcode.fromName("QR_CODE");
        final int headerColor = 0xFFFF00FF;
        final int starStatus = 1;
        final long lastUsed = 1687112282000L;
        final int archiveStatus = 1;
        long id = DBHelper.insertLoyaltyCard(
                mDatabase, store, note, validFrom, expiry, balance, balanceType,
                cardId, barcodeId, barcodeType, headerColor, starStatus, lastUsed,
                archiveStatus
        );
        assertEquals("expect first card", 1, id);

        try (Cursor cursor = mResolver.query(cardsUri, null, null, null)) {
            assertEquals("number of cards", 1, cursor.getCount());

            final String[] expectedColumns = new String[]{
                    "_id", "store", "validfrom", "expiry", "balance", "balancetype",
                    "note", "headercolor", "cardid", "barcodeid",
                    "barcodetype", "starstatus", "lastused", "archive"
            };

            assertEquals("number of columns", expectedColumns.length, cursor.getColumnCount());
            assertEquals(
                    "column names",
                    new HashSet<>(Arrays.asList(expectedColumns)),
                    new HashSet<>(Arrays.asList(cursor.getColumnNames()))
            );

            cursor.moveToNext();

            final int actualId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            final String actualName = cursor.getString(cursor.getColumnIndexOrThrow("store"));
            final String actualNote = cursor.getString(cursor.getColumnIndexOrThrow("note"));
            final String actualValidFrom = cursor.getString(cursor.getColumnIndexOrThrow("validfrom"));
            final String actualExpiry = cursor.getString(cursor.getColumnIndexOrThrow("expiry"));
            final BigDecimal actualBalance = new BigDecimal(cursor.getString(cursor.getColumnIndexOrThrow("balance")));
            final String actualBalanceType = cursor.getString(cursor.getColumnIndexOrThrow("balancetype"));
            final String actualCardId = cursor.getString(cursor.getColumnIndexOrThrow("cardid"));
            final String actualBarcodeId = cursor.getString(cursor.getColumnIndexOrThrow("barcodeid"));
            final String actualBarcodeType = cursor.getString(cursor.getColumnIndexOrThrow("barcodetype"));
            final int actualHeaderColor = cursor.getInt(cursor.getColumnIndexOrThrow("headercolor"));
            final int actualStarred = cursor.getInt(cursor.getColumnIndexOrThrow("starstatus"));
            final long actualLastUsed = cursor.getLong(cursor.getColumnIndexOrThrow("lastused"));
            final int actualArchiveStatus = cursor.getInt(cursor.getColumnIndexOrThrow("archive"));

            assertEquals("Id", 1, actualId);
            assertEquals("Name", store, actualName);
            assertEquals("Note", note, actualNote);
            assertEquals("ValidFrom", validFrom.toString(), actualValidFrom);
            assertEquals("Expiry", expiry.toString(), actualExpiry);
            assertEquals("Balance", balance, actualBalance);
            assertEquals("BalanceTypeColumn", balanceType.toString(), actualBalanceType);
            assertEquals("CardId", cardId, actualCardId);
            assertEquals("BarcodeId", barcodeId, actualBarcodeId);
            assertEquals("BarcodeType", barcodeType.format().name(), actualBarcodeType);
            assertEquals("HeaderColorColumn", headerColor, actualHeaderColor);
            assertEquals("Starred", starStatus, actualStarred);
            assertEquals("LastUsed", lastUsed, actualLastUsed);
            assertEquals("ArchiveStatus", archiveStatus, actualArchiveStatus);
        }
    }

    @Test
    public void testCardsProjection() {
        final Uri cardsUri = getUri("cards");

        try (Cursor cursor = mResolver.query(cardsUri, null, null, null)) {
            assertEquals(cursor.getCount(), 0);
        }

        TestHelpers.addLoyaltyCards(mDatabase, 1);

        // Query with projection of columns, including internal column names, which should be filtered out
        try (Cursor cursor = mResolver.query(cardsUri, new String[] {"_id", "store", "zoomlevel"}, null, null)) {
            assertEquals("number of cards", 1, cursor.getCount());

            assertEquals("number of columns", 2, cursor.getColumnCount());
            assertArrayEquals("column names", new String[]{"_id", "store"}, cursor.getColumnNames());

            cursor.moveToNext();

            final int actualId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            final String actualName = cursor.getString(cursor.getColumnIndexOrThrow("store"));

            assertEquals("id", 1, actualId);
            assertEquals("store", "store, \"   1", actualName);
        }
    }

    @Test
    public void testGroups() {
        final Uri groupsUri = getUri("groups");

        try (Cursor cursor = mResolver.query(groupsUri, null, null, null)) {
            assertEquals("start without groups", 0, cursor.getCount());
        }

        TestHelpers.addGroups(mDatabase, 4);

        try (Cursor cursor = mResolver.query(groupsUri, null, null, null)) {
            assertEquals("number of groups", 4, cursor.getCount());
            assertEquals("number of columns", 2, cursor.getColumnCount());
            assertArrayEquals("column names", new String[]{"_id", "orderId"}, cursor.getColumnNames());
            for (int i = 0; i < 4; i++) {
                cursor.moveToNext();
                assertEquals(
                        String.format("groups[%d]._id", i),
                        String.format("group, \"%4d", 4 - i),
                        cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                );
                assertEquals(
                        String.format("groups[%d].orderId", i),
                        String.valueOf(i),
                        cursor.getString(cursor.getColumnIndexOrThrow("orderId"))
                );
            }
        }
    }

    @Test
    public void testCardGroups() {
        final Uri cardGroupsUri = getUri("card_groups");

        try (Cursor cursor = mResolver.query(cardGroupsUri, null, null, null)) {
            assertEquals(cursor.getCount(), 0);
        }

        TestHelpers.addLoyaltyCards(mDatabase, 5);
        TestHelpers.addGroups(mDatabase, 4);

        final List<Group> groupsForOne = new ArrayList<>();
        groupsForOne.add(DBHelper.getGroup(mDatabase, "group, \"   1"));

        final List<Group> groupsForTwo = new ArrayList<>();
        groupsForTwo.add(DBHelper.getGroup(mDatabase, "group, \"   1"));
        groupsForTwo.add(DBHelper.getGroup(mDatabase, "group, \"   2"));

        DBHelper.setLoyaltyCardGroups(mDatabase, 1, groupsForOne);
        DBHelper.setLoyaltyCardGroups(mDatabase, 2, groupsForTwo);

        final Map<String, List<String>> expectedGroups = new HashMap<>() {{
            put("group, \"   1", Arrays.asList("1", "2"));
            put("group, \"   2", Collections.singletonList("2"));
        }};

        try (Cursor cursor = mResolver.query(cardGroupsUri, null, null, null)) {
            assertEquals("number of card groups", 3, cursor.getCount());
            assertEquals("number of columns", 2, cursor.getColumnCount());
            assertArrayEquals("column names", new String[]{"cardId", "groupId"}, cursor.getColumnNames());

            final Map<String, List<String>> groups = new HashMap<>();
            while (cursor.moveToNext()) {
                final String cardId = cursor.getString(cursor.getColumnIndexOrThrow("cardId"));
                final String groupId = cursor.getString(cursor.getColumnIndexOrThrow("groupId"));
                groups.computeIfAbsent(groupId, k -> new ArrayList<>()).add(cardId);
            }
            assertEquals("expected groups with cards", expectedGroups, groups);
        }
    }

    private Uri getUri(final String endpoint) {
        return Uri.parse(String.format(Locale.ROOT, "content://%s/%s", CardsContentProvider.AUTHORITY, endpoint));
    }
}
