package protect.card_locker;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LoyaltyCardListDisplayOptionsManager {
    public final Context mContext;

    private final Runnable mRefreshCardsCallback;
    private final Runnable mSwapCursorCallback;

    protected SharedPreferences mCardDetailsPref;

    private boolean mShowNameBelowThumbnail;
    private boolean mShowNote;
    private boolean mShowBalance;
    private boolean mShowValidity;
    private boolean mShowArchivedCards;

    public static class LoyaltyCardDisplayOption {
        public String name;
        public boolean value;
        public Consumer<Boolean> callback;

        LoyaltyCardDisplayOption(String name, boolean value, Consumer<Boolean> callback) {
            this.name = name;
            this.value = value;
            this.callback = callback;
        }
    }

    public LoyaltyCardListDisplayOptionsManager(Context context, @NonNull Runnable refreshCardsCallback, @Nullable Runnable swapCursorCallback) {
        mContext = context;
        mRefreshCardsCallback = refreshCardsCallback;
        mSwapCursorCallback = swapCursorCallback;

        // Retrieve user details preference
        mCardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        mShowNameBelowThumbnail = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_name_below_thumbnail), false);
        mShowNote = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_note), true);
        mShowBalance = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_balance), true);
        mShowValidity = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_validity), true);
        mShowArchivedCards = mCardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_archived_cards), true);
    }

    void saveDetailState(int stateId, boolean value) {
        SharedPreferences.Editor cardDetailsPrefEditor = mCardDetailsPref.edit();
        cardDetailsPrefEditor.putBoolean(mContext.getString(stateId), value);
        cardDetailsPrefEditor.apply();
    }

    public void showNameBelowThumbnail(boolean show) {
        mShowNameBelowThumbnail = show;
        mRefreshCardsCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_name_below_thumbnail, show);
    }

    public boolean showingNameBelowThumbnail() {
        return mShowNameBelowThumbnail;
    }

    public void showNote(boolean show) {
        mShowNote = show;
        mRefreshCardsCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_note, show);
    }

    public boolean showingNote() {
        return mShowNote;
    }

    public void showBalance(boolean show) {
        mShowBalance = show;
        mRefreshCardsCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_balance, show);
    }

    public boolean showingBalance() {
        return mShowBalance;
    }

    public void showValidity(boolean show) {
        mShowValidity = show;
        mRefreshCardsCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_validity, show);
    }

    public boolean showingValidity() {
        return mShowValidity;
    }

    public void showArchivedCards(boolean show) {
        if (mSwapCursorCallback == null) {
            throw new IllegalStateException("No swap cursor callback is available, can not manage archive state");
        }

        mShowArchivedCards = show;
        mSwapCursorCallback.run();

        saveDetailState(R.string.sharedpreference_card_details_show_archived_cards, show);
    }

    public boolean showingArchivedCards() {
        if (mSwapCursorCallback == null) {
            throw new IllegalStateException("No swap cursor callback is available, can not manage archive state");
        }

        return mShowArchivedCards;
    }

    public void showDisplayOptionsDialog() {
        List<LoyaltyCardDisplayOption> displayOptions = new ArrayList<>();

        displayOptions.add(new LoyaltyCardDisplayOption(
                mContext.getString(R.string.show_name_below_image_thumbnail),
                showingNameBelowThumbnail(),
                this::showNameBelowThumbnail
        ));
        displayOptions.add(new LoyaltyCardDisplayOption(
                mContext.getString(R.string.show_note),
                showingNote(),
                this::showNote
        ));
        displayOptions.add(new LoyaltyCardDisplayOption(
                mContext.getString(R.string.show_balance),
                showingBalance(),
                this::showBalance
        ));
        displayOptions.add(new LoyaltyCardDisplayOption(
                mContext.getString(R.string.show_validity),
                showingValidity(),
                this::showValidity
        ));

        // Hide "Show archived cards" option unless the callback exists
        if (mSwapCursorCallback != null) {
            displayOptions.add(new LoyaltyCardDisplayOption(
                    mContext.getString(R.string.show_archived_cards),
                    showingArchivedCards(),
                    this::showArchivedCards
            ));
        }

        // We need to convert Boolean[] to boolean[]
        boolean[] values = new boolean[displayOptions.size()];
        for (int i = 0; i < values.length; i++) values[i] = displayOptions.get(i).value;

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(mContext);
        builder.setTitle(R.string.action_display_options);
        builder.setMultiChoiceItems(
                displayOptions.stream().map(x -> x.name).toArray(String[]::new),
                values,
                (DialogInterface.OnMultiChoiceClickListener) (dialogInterface, i, b) -> displayOptions.get(i).callback.accept(b)
        );
        builder.setPositiveButton(R.string.ok, (dialog, i) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
