package protect.card_locker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class ShortcutHelper
{
    // Android documentation says that no more than 5 shortcuts
    // are supported. However, that may be too many, as not all
    // launcher will show all 5. Instead, the number is limited
    // to 3 here, so that the most recent shortcut has a good
    // chance of being shown.
    private static final int MAX_SHORTCUTS = 3;

    /**
     * Add a card to the app shortcuts, and maintain a list of the most
     * recently used cards. If there is already a shortcut for the card,
     * the card is marked as the most recently used card. If adding this
     * card exceeds the max number of shortcuts, then the least recently
     * used card shortcut is discarded.
     */
    @TargetApi(25)
    static void updateShortcuts(Context context, LoyaltyCard card, Intent intent)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1)
        {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            LinkedList<ShortcutInfo> list = new LinkedList<>(shortcutManager.getDynamicShortcuts());

            String shortcutId = Integer.toString(card.id);

            // Sort the shortcuts by rank, so working with the relative order will be easier.
            // This sorts so that the lowest rank is first.
            Collections.sort(list, new Comparator<ShortcutInfo>()
            {
                @Override
                public int compare(ShortcutInfo o1, ShortcutInfo o2)
                {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1)
                    {
                        return o1.getRank() - o2.getRank();
                    }
                    else
                    {
                        return 0;
                    }
                }
            });

            Integer foundIndex = null;

            for(int index = 0; index < list.size(); index++)
            {
                if(list.get(index).getId().equals(shortcutId))
                {
                    // Found the item already
                    foundIndex = index;
                    break;
                }
            }

            if(foundIndex != null)
            {
                // If the item is already found, then the list needs to be
                // reordered, so that the selected item now has the lowest
                // rank, thus letting it survive longer.
                ShortcutInfo found = list.remove(foundIndex.intValue());
                list.addFirst(found);
            }
            else
            {
                // The item is new to the list. First, we need to trim the list
                // until it is able to accept a new item, then the item is
                // inserted.
                while(list.size() >= MAX_SHORTCUTS)
                {
                    list.pollLast();
                }

                ShortcutInfo shortcut = new ShortcutInfo.Builder(context, Integer.toString(card.id))
                    .setShortLabel(card.store)
                    .setLongLabel(card.store)
                    .setIntent(intent)
                    .build();

                list.addFirst(shortcut);
            }

            LinkedList<ShortcutInfo> finalList = new LinkedList<>();

            // The ranks are now updated; the order in the list is the rank.
            for(int index = 0; index < list.size(); index++)
            {
                ShortcutInfo prevShortcut = list.get(index);

                Intent shortcutIntent = prevShortcut.getIntent();

                // Prevent instances of the view activity from piling up; if one exists let this
                // one replace it.
                shortcutIntent.setFlags(shortcutIntent.getFlags() | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                ShortcutInfo updatedShortcut = new ShortcutInfo.Builder(context, prevShortcut.getId())
                        .setShortLabel(prevShortcut.getShortLabel())
                        .setLongLabel(prevShortcut.getLongLabel())
                        .setIntent(shortcutIntent)
                        .setIcon(Icon.createWithResource(context, R.drawable.circle))
                        .setRank(index)
                        .build();

                finalList.addLast(updatedShortcut);
            }

            shortcutManager.setDynamicShortcuts(finalList);
        }
    }

    /**
     * Remove the given card id from the app shortcuts, if such a
     * shortcut exists.
     */
    @TargetApi(25)
    static void removeShortcut(Context context, int cardId)
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1)
        {
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            List<ShortcutInfo> list = shortcutManager.getDynamicShortcuts();

            String shortcutId = Integer.toString(cardId);

            for(int index = 0; index < list.size(); index++)
            {
                if(list.get(index).getId().equals(shortcutId))
                {
                    list.remove(index);
                    break;
                }
            }

            shortcutManager.setDynamicShortcuts(list);
        }
    }
}
