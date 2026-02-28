package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedList;

class ShortcutHelper {
    /**
     * This variable controls the maximum number of shortcuts available.
     * It is made public only to make testing easier and should not be
     * manually modified. We use -1 here as a default value to check if
     * the value has been set either manually by the test scenario or
     * automatically in the `updateShortcuts` function.
     * Its actual value will be set based on the maximum amount of shortcuts
     * declared by the launcher via `getMaxShortcutCountPerActivity`.
     */
    @VisibleForTesting
    public static int maxShortcuts = -1;

    // https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable.html
    private static final int ADAPTIVE_BITMAP_SCALE = 1;
    private static final int ADAPTIVE_BITMAP_SIZE = 108 * ADAPTIVE_BITMAP_SCALE;
    private static final int ADAPTIVE_BITMAP_VISIBLE_SIZE = 72 * ADAPTIVE_BITMAP_SCALE;
    private static final int ADAPTIVE_BITMAP_IMAGE_SIZE = ADAPTIVE_BITMAP_VISIBLE_SIZE + 5 * ADAPTIVE_BITMAP_SCALE;

    /**
     * Update the dynamic shortcut list with the most recently viewed cards
     * based on the lastUsed field. Archived cards are excluded from the shortcuts
     * list. The list keeps at most maxShortcuts number of elements.
     */
    static void updateShortcuts(Context context) {
        if (maxShortcuts == -1) {
            maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context);
        }
        LinkedList<ShortcutInfoCompat> finalList = new LinkedList<>();
        SQLiteDatabase database = new DBHelper(context).getReadableDatabase();
        Cursor loyaltyCardCursor = DBHelper.getLoyaltyCardCursor(
                database,
                "",
                null,
                DBHelper.LoyaltyCardOrder.LastUsed,
                DBHelper.LoyaltyCardOrderDirection.Ascending,
                DBHelper.LoyaltyCardArchiveFilter.Unarchived
        );

        int rank = 0;

        while (rank < maxShortcuts && loyaltyCardCursor.moveToNext()) {
            int id = loyaltyCardCursor.getInt(loyaltyCardCursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID));
            LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(context, database, id);

            ShortcutInfoCompat updatedShortcut = createShortcutBuilder(context, loyaltyCard)
                    .setRank(rank)
                    .build();

            finalList.addLast(updatedShortcut);
            rank++;
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, finalList);
    }

    static @NotNull
    Bitmap createAdaptiveBitmap(@NotNull Bitmap in, int paddingColor) {
        Bitmap ret = Bitmap.createBitmap(ADAPTIVE_BITMAP_SIZE, ADAPTIVE_BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas output = new Canvas(ret);
        output.drawColor(paddingColor);
        Bitmap resized = Utils.resizeBitmap(in, ADAPTIVE_BITMAP_IMAGE_SIZE);
        output.drawBitmap(resized, (ADAPTIVE_BITMAP_SIZE - resized.getWidth()) / 2f, (ADAPTIVE_BITMAP_SIZE - resized.getHeight()) / 2f, null);
        return ret;
    }

    static ShortcutInfoCompat.Builder createShortcutBuilder(Context context, LoyaltyCard loyaltyCard) {
        Intent intent = new Intent(context, LoyaltyCardViewActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        // Prevent instances of the view activity from piling up; if one exists let this
        // one replace it.
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        final Bundle bundle = new Bundle();
        bundle.putInt(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id);
        intent.putExtras(bundle);

        Bitmap iconBitmap = loyaltyCard.getImageThumbnail(context);
        if (iconBitmap == null) {
            iconBitmap = Utils.generateIcon(context, loyaltyCard, true).getLetterTile();
        } else {
            iconBitmap = createAdaptiveBitmap(iconBitmap, Utils.needsDarkForeground(Utils.getHeaderColor(context, loyaltyCard)) ? Color.BLACK : Color.WHITE);
        }

        IconCompat icon = IconCompat.createWithAdaptiveBitmap(iconBitmap);

        return new ShortcutInfoCompat.Builder(context, Integer.toString(loyaltyCard.id))
                .setShortLabel(loyaltyCard.store)
                .setLongLabel(loyaltyCard.store)
                .setIntent(intent)
                .setIcon(icon);
    }
}
