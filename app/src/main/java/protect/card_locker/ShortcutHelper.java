package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.IconCompat;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class ShortcutHelper {
    // Android documentation says that no more than 5 shortcuts
    // are supported. However, that may be too many, as not all
    // launcher will show all 5. Instead, the number is limited
    // to 3 here, so that the most recent shortcut has a good
    // chance of being shown.
    private static final int MAX_SHORTCUTS = 3;

    // https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable.html
    private static final int ADAPTIVE_BITMAP_SCALE = 1;
    private static final int ADAPTIVE_BITMAP_SIZE = 108 * ADAPTIVE_BITMAP_SCALE;
    private static final int ADAPTIVE_BITMAP_VISIBLE_SIZE = 72 * ADAPTIVE_BITMAP_SCALE;
    private static final int ADAPTIVE_BITMAP_IMAGE_SIZE = ADAPTIVE_BITMAP_VISIBLE_SIZE + 5 * ADAPTIVE_BITMAP_SCALE;
    private static final int PADDING_COLOR_OVERLAY = Color.argb(127, 0, 0, 0);

    /**
     * Add a card to the app shortcuts, and maintain a list of the most
     * recently used cards. If there is already a shortcut for the card,
     * the card is marked as the most recently used card. If adding this
     * card exceeds the max number of shortcuts, then the least recently
     * used card shortcut is discarded.
     */
    static void updateShortcuts(Context context, LoyaltyCard card) {
        if (card.archiveStatus == 1) {
            // Don't add archived card to menu
            return;
        }

        LinkedList<ShortcutInfoCompat> list = new LinkedList<>(ShortcutManagerCompat.getDynamicShortcuts(context));

        SQLiteDatabase database = new DBHelper(context).getReadableDatabase();

        String shortcutId = Integer.toString(card.id);

        // Sort the shortcuts by rank, so working with the relative order will be easier.
        // This sorts so that the lowest rank is first.
        Collections.sort(list, Comparator.comparingInt(ShortcutInfoCompat::getRank));

        Integer foundIndex = null;

        for (int index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(shortcutId)) {
                // Found the item already
                foundIndex = index;
                break;
            }
        }

        if (foundIndex != null) {
            // If the item is already found, then the list needs to be
            // reordered, so that the selected item now has the lowest
            // rank, thus letting it survive longer.
            ShortcutInfoCompat found = list.remove(foundIndex.intValue());
            list.addFirst(found);
        } else {
            // The item is new to the list. We add it and trim the list later.
            ShortcutInfoCompat shortcut = createShortcutBuilder(context, card).build();
            list.addFirst(shortcut);
        }

        LinkedList<ShortcutInfoCompat> finalList = new LinkedList<>();
        int rank = 0;

        // The ranks are now updated; the order in the list is the rank.
        for (int index = 0; index < list.size(); index++) {
            ShortcutInfoCompat prevShortcut = list.get(index);

            LoyaltyCard loyaltyCard = DBHelper.getLoyaltyCard(database, Integer.parseInt(prevShortcut.getId()));

            // skip outdated cards that no longer exist
            if (loyaltyCard != null) {
                ShortcutInfoCompat updatedShortcut = createShortcutBuilder(context, loyaltyCard)
                        .setRank(rank)
                        .build();

                finalList.addLast(updatedShortcut);
                rank++;

                // trim the list
                if (rank >= MAX_SHORTCUTS) {
                    break;
                }
            }
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, finalList);
    }

    /**
     * Remove the given card id from the app shortcuts, if such a
     * shortcut exists.
     */
    static void removeShortcut(Context context, int cardId) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, Collections.singletonList(Integer.toString(cardId)));
    }

    static @NotNull
    Bitmap createAdaptiveBitmap(@NotNull Bitmap in, int paddingColor) {
        Bitmap ret = Bitmap.createBitmap(ADAPTIVE_BITMAP_SIZE, ADAPTIVE_BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas output = new Canvas(ret);
        output.drawColor(ColorUtils.compositeColors(PADDING_COLOR_OVERLAY, paddingColor));
        Bitmap resized = Utils.resizeBitmap(in, ADAPTIVE_BITMAP_IMAGE_SIZE);
        output.drawBitmap(resized, (ADAPTIVE_BITMAP_SIZE - resized.getWidth()) / 2f, (ADAPTIVE_BITMAP_SIZE - resized.getHeight()) / 2f, null);
        return ret;
    }

    static ShortcutInfoCompat.Builder createShortcutBuilder(Context context, LoyaltyCard loyaltyCard) {
        Intent intent = new Intent(context, LoyaltyCardViewActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        // Prevent instances of the view activity from piling up; if one exists let this
        // one replace it.
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final Bundle bundle = new Bundle();
        bundle.putInt(LoyaltyCardViewActivity.BUNDLE_ID, loyaltyCard.id);
        intent.putExtras(bundle);

        Bitmap iconBitmap = Utils.retrieveCardImage(context, loyaltyCard.id, ImageLocationType.icon);
        if (iconBitmap == null) {
            iconBitmap = Utils.generateIcon(context, loyaltyCard, true).getLetterTile();
        } else {
            iconBitmap = createAdaptiveBitmap(iconBitmap, Utils.getHeaderColor(context, loyaltyCard));
        }

        IconCompat icon = IconCompat.createWithAdaptiveBitmap(iconBitmap);

        return new ShortcutInfoCompat.Builder(context, Integer.toString(loyaltyCard.id))
                .setShortLabel(loyaltyCard.store)
                .setLongLabel(loyaltyCard.store)
                .setIntent(intent)
                .setIcon(icon);
    }
}
