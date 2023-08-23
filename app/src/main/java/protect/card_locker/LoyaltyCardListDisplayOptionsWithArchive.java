package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoyaltyCardListDisplayOptionsWithArchive extends LoyaltyCardListDisplayOptions {
    Runnable mSwapCursorCallback;

    private boolean mShowArchivedCards;

    public LoyaltyCardListDisplayOptionsWithArchive(Context context, Runnable swapCursorCallback) {
        super(context);
        mShowArchivedCards = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_archived_cards), true);
        mSwapCursorCallback = swapCursorCallback;
    }

    public void showArchivedCards(boolean show) {
        mShowArchivedCards = show;

        mSwapCursorCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_archived_cards, show);
    }

    public boolean showingArchivedCards() {
        return mShowArchivedCards;
    }

    public void showDisplayOptionsDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(mContext);
        builder.setTitle(R.string.action_display_options);
        builder.setMultiChoiceItems(
                new String[]{
                        mContext.getString(R.string.show_name_below_image_thumbnail),
                        mContext.getString(R.string.show_note),
                        mContext.getString(R.string.show_balance),
                        mContext.getString(R.string.show_validity),
                        mContext.getString(R.string.show_archived_cards)
                },
                new boolean[]{
                        showingNameBelowThumbnail(),
                        showingNote(),
                        showingBalance(),
                        showingValidity(),
                        showingArchivedCards()
                },
                (dialogInterface, i, b) -> {
                    switch (i) {
                        case 0: showNameBelowThumbnail(b); break;
                        case 1: showNote(b); break;
                        case 2: showBalance(b); break;
                        case 3: showValidity(b); break;
                        case 4: showArchivedCards(b); break;
                        default: throw new IndexOutOfBoundsException("No such index exists in LoyaltyCardCursorAdapter show details view");
                    }
                }
        );
        builder.setPositiveButton(R.string.ok, (dialog, i) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
