package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LoyaltyCardListDisplayOptions {
    Context mContext;

    LoyaltyCardCursorAdapter mAdapter;

    SharedPreferences mCardDetailsPref;

    private boolean mShowNameBelowThumbnail;
    private boolean mShowNote;
    private boolean mShowBalance;
    private boolean mShowValidity;

    public LoyaltyCardListDisplayOptions(Context context) {
        mContext = context;

        // Retrieve user details preference
        mCardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        mShowNameBelowThumbnail = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_name_below_thumbnail), false);
        mShowNote = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_note), true);
        mShowBalance = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_balance), true);
        mShowValidity = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_validity), true);
    }

    public void setAdapterToRefresh(LoyaltyCardCursorAdapter adapter) {
        mAdapter = adapter;
    }

    void saveDetailState(int stateId, boolean value) {
        SharedPreferences.Editor cardDetailsPrefEditor = mCardDetailsPref.edit();
        cardDetailsPrefEditor.putBoolean(mContext.getString(stateId), value);
        cardDetailsPrefEditor.apply();
    }

    public void showNameBelowThumbnail(boolean show) {
        mShowNameBelowThumbnail = show;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        saveDetailState(R.string.sharedpreference_card_details_show_name_below_thumbnail, show);
    }

    public boolean showingNameBelowThumbnail() {
        return mShowNameBelowThumbnail;
    }

    public void showNote(boolean show) {
        mShowNote = show;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        saveDetailState(R.string.sharedpreference_card_details_show_note, show);
    }

    public boolean showingNote() {
        return mShowNote;
    }

    public void showBalance(boolean show) {
        mShowBalance = show;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        saveDetailState(R.string.sharedpreference_card_details_show_balance, show);
    }

    public boolean showingBalance() {
        return mShowBalance;
    }

    public void showValidity(boolean show) {
        mShowValidity = show;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }

        saveDetailState(R.string.sharedpreference_card_details_show_validity, show);
    }

    public boolean showingValidity() {
        return mShowValidity;
    }

    public void showDisplayOptionsDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(mContext);
        builder.setTitle(R.string.action_display_options);
        builder.setMultiChoiceItems(
                new String[]{
                        mContext.getString(R.string.show_name_below_image_thumbnail),
                        mContext.getString(R.string.show_note),
                        mContext.getString(R.string.show_balance),
                        mContext.getString(R.string.show_validity)
                },
                new boolean[]{
                        showingNameBelowThumbnail(),
                        showingNote(),
                        showingBalance(),
                        showingValidity()
                },
                (dialogInterface, i, b) -> {
                    switch (i) {
                        case 0: showNameBelowThumbnail(b); break;
                        case 1: showNote(b); break;
                        case 2: showBalance(b); break;
                        case 3: showValidity(b); break;
                        default: throw new IndexOutOfBoundsException("No such index exists in LoyaltyCardCursorAdapter show details view");
                    }
                }
        );
        builder.setPositiveButton(R.string.ok, (dialog, i) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
