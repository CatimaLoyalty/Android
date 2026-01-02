package protect.card_locker;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;

import com.google.zxing.BarcodeFormat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.math.BigDecimal;
import java.util.Comparator;

@RunWith(RobolectricTestRunner.class)
public class ShortcutHelperTest {
    private Activity mActivity;
    private SQLiteDatabase mDatabase;
    private int id1;
    private int id2;
    private int id3;
    private int id4;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(MainActivity.class);
        mDatabase = TestHelpers.getEmptyDb(mActivity).getWritableDatabase();

        long now = System.currentTimeMillis();
        id1 = (int) DBHelper.insertLoyaltyCard(mDatabase, "store1", "note1", null, null, new BigDecimal("0"), null, "cardId1", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), null, Color.BLACK, 0, now,0);
        id2 = (int) DBHelper.insertLoyaltyCard(mDatabase, "store2", "note2", null, null, new BigDecimal("0"), null, "cardId2", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), null, Color.BLACK, 0, now + 10,0);
        id3 = (int) DBHelper.insertLoyaltyCard(mDatabase, "store3", "note3", null, null, new BigDecimal("0"), null, "cardId3", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), null, Color.BLACK, 0, now + 20,0);
        id4 = (int) DBHelper.insertLoyaltyCard(mDatabase, "store4", "note4", null, null, new BigDecimal("0"), null, "cardId4", null, CatimaBarcode.fromBarcode(BarcodeFormat.UPC_A), null, Color.BLACK, 0, now + 30,0);

        ShortcutHelper.maxShortcuts = 3;
    }

    private Integer[] getShortcutIds(Context context) {
        return ShortcutManagerCompat.getDynamicShortcuts(context)
                .stream()
                .sorted(Comparator.comparingInt(ShortcutInfoCompat::getRank))
                .map(shortcut -> Integer.parseInt(shortcut.getId()))
                .toArray(Integer[]::new);
    }

    @Test
    public void onArchiveUnarchive() {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity) activityController.get();

        activityController.pause();
        activityController.resume();

        assertEquals(3, ShortcutManagerCompat.getDynamicShortcuts(mainActivity).stream().count());

        Integer[] ids = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id4, id3, id2}, ids);

        DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, id4, 1);

        activityController.pause();
        activityController.resume();

        Integer[] idsAfterArchive = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id3, id2, id1}, idsAfterArchive);

        DBHelper.updateLoyaltyCardArchiveStatus(mDatabase, id4, 0);

        activityController.pause();
        activityController.resume();

        Integer[] idsAfterUnarchive = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id4, id3, id2}, idsAfterUnarchive);
    }

    @Test
    public void onAddRemoveFavorite() {
        ActivityController activityController = Robolectric.buildActivity(MainActivity.class).create();

        Activity mainActivity = (Activity) activityController.get();

        activityController.pause();
        activityController.resume();

        assertEquals(3, ShortcutManagerCompat.getDynamicShortcuts(mainActivity).stream().count());

        Integer[] ids = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id4, id3, id2}, ids);

        DBHelper.updateLoyaltyCardStarStatus(mDatabase, id1, 1);

        activityController.pause();
        activityController.resume();

        Integer[] idsAfterFav = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id1, id4, id3}, idsAfterFav);

        DBHelper.updateLoyaltyCardStarStatus(mDatabase, id1, 0);

        activityController.pause();
        activityController.resume();

        Integer[] idsAfterUnfav = getShortcutIds(mainActivity);

        assertArrayEquals(new Integer[] {id4, id3, id2}, idsAfterUnfav);
    }
}
