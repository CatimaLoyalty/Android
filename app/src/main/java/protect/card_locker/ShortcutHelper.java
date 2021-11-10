package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.IconCompat;

import org.jetbrains.annotations.NotNull;

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
    private static final int PADDING_COLOR = Color.argb(255, 255, 255, 255);
    private static final int PADDING_COLOR_OVERLAY = Color.argb(127, 0, 0, 0);

    /**
     * Add a card to the app shortcuts, and maintain a list of the most
     * recently used cards. If there is already a shortcut for the card,
     * the card is marked as the most recently used card. If adding this
     * card exceeds the max number of shortcuts, then the least recently
     * used card shortcut is discarded.
     */
    static void updateShortcuts(Context context, LoyaltyCard card) {
        LinkedList<ShortcutInfoCompat> list = new LinkedList<>(ShortcutManagerCompat.getDynamicShortcuts(context));

        DBHelper dbHelper = new DBHelper(context);

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
            // The item is new to the list. First, we need to trim the list
            // until it is able to accept a new item, then the item is
            // inserted.
            while (list.size() >= MAX_SHORTCUTS) {
                list.pollLast();
            }

            ShortcutInfoCompat shortcut = createShortcutBuilder(context, card).build();

            list.addFirst(shortcut);
        }

        LinkedList<ShortcutInfoCompat> finalList = new LinkedList<>();

        // The ranks are now updated; the order in the list is the rank.
        for (int index = 0; index < list.size(); index++) {
            ShortcutInfoCompat prevShortcut = list.get(index);

            LoyaltyCard loyaltyCard = dbHelper.getLoyaltyCard(Integer.parseInt(prevShortcut.getId()));

            ShortcutInfoCompat updatedShortcut = createShortcutBuilder(context, loyaltyCard)
                    .setRank(index)
                    .build();

            finalList.addLast(updatedShortcut);
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, finalList);
    }

    /**
     * Remove the given card id from the app shortcuts, if such a
     * shortcut exists.
     */
    static void removeShortcut(Context context, int cardId) {
        List<ShortcutInfoCompat> list = ShortcutManagerCompat.getDynamicShortcuts(context);

        String shortcutId = Integer.toString(cardId);

        for (int index = 0; index < list.size(); index++) {
            if (list.get(index).getId().equals(shortcutId)) {
                list.remove(index);
                break;
            }
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, list);
    }

    static @NotNull Bitmap createAdaptiveBitmap(@NotNull Bitmap in, int paddingColor){
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
        bundle.putInt("id", loyaltyCard.id);
        bundle.putBoolean("view", true);
        intent.putExtras(bundle);

        Bitmap iconBitmap = Utils.retrieveCardImage(context, loyaltyCard.id, ImageLocationType.icon);
        if (iconBitmap == null) {
            iconBitmap = Utils.generateIcon(context, loyaltyCard, true).getLetterTile();
        }else{
            iconBitmap = createAdaptiveBitmap(iconBitmap, loyaltyCard.headerColor == null ? PADDING_COLOR : loyaltyCard.headerColor);
        }

        IconCompat icon = IconCompat.createWithAdaptiveBitmap(iconBitmap);

        return new ShortcutInfoCompat.Builder(context, Integer.toString(loyaltyCard.id))
                .setShortLabel(loyaltyCard.store)
                .setLongLabel(loyaltyCard.store)
                .setIntent(intent)
                .setIcon(icon);
    }
}
