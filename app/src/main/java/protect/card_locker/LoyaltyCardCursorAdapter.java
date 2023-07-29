package protect.card_locker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.LoyaltyCardLayoutBinding;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    boolean mDarkModeEnabled;
    public final Context mContext;
    private final CardAdapterListener mListener;
    protected SparseBooleanArray mSelectedItems;
    protected SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;
    private boolean mShowNameBelowThumbnail;
    private boolean mShowNote;
    private boolean mShowBalance;
    private boolean mShowValidity;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener) {
        super(inputCursor, DBHelper.LoyaltyCardDbIds.ID);
        setHasStableIds(true);
        mContext = inputContext;
        mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);

        refreshState();

        swapCursor(inputCursor);
    }

    private void saveDetailState(int stateId, boolean value) {
        SharedPreferences cardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor cardDetailsPrefEditor = cardDetailsPref.edit();
        cardDetailsPrefEditor.putBoolean(mContext.getString(stateId), value);
        cardDetailsPrefEditor.apply();
    }

    public void refreshState() {
        // Retrieve user details preference
        SharedPreferences cardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        mShowNameBelowThumbnail = cardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_name_below_thumbnail), false);
        mShowNote = cardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_note), true);
        mShowBalance = cardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_balance), true);
        mShowValidity = cardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show_validity), true);
    }

    public void showNameBelowThumbnail(boolean show) {
        mShowNameBelowThumbnail = show;
        notifyDataSetChanged();

        saveDetailState(R.string.sharedpreference_card_details_show_name_below_thumbnail, show);
    }

    public boolean showingNameBelowThumbnail() {
        return mShowNameBelowThumbnail;
    }

    public void showNote(boolean show) {
        mShowNote = show;
        notifyDataSetChanged();

        saveDetailState(R.string.sharedpreference_card_details_show_note, show);
    }

    public boolean showingNote() {
        return mShowNote;
    }

    public void showBalance(boolean show) {
        mShowBalance = show;
        notifyDataSetChanged();

        saveDetailState(R.string.sharedpreference_card_details_show_balance, show);
    }

    public boolean showingBalance() {
        return mShowBalance;
    }

    public void showValidity(boolean show) {
        mShowValidity = show;
        notifyDataSetChanged();

        saveDetailState(R.string.sharedpreference_card_details_show_validity, show);
    }

    public boolean showingValidity() {
        return mShowValidity;
    }

    public void showSelectDetailDisplayDialog() {
        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(mContext);
        builder.setTitle(R.string.action_show_details);
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

    @NonNull
    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(@NonNull ViewGroup inputParent, int inputViewType) {
        LoyaltyCardLayoutBinding loyaltyCardLayoutBinding = LoyaltyCardLayoutBinding.inflate(
                LayoutInflater.from(inputParent.getContext()),
                inputParent,
                false
        );
        return new LoyaltyCardListItemViewHolder(loyaltyCardLayoutBinding, mListener);
    }

    public LoyaltyCard getCard(int position) {
        mCursor.moveToPosition(position);
        return LoyaltyCard.toLoyaltyCard(mCursor);
    }

    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        // Invisible until we want to show something more
        boolean showDivider = false;
        inputHolder.mDivider.setVisibility(View.GONE);

        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);
        Bitmap icon = Utils.retrieveCardImage(mContext, loyaltyCard.id, ImageLocationType.icon);

        if (mShowNameBelowThumbnail && icon != null) {
            showDivider = true;
            inputHolder.setStoreField(loyaltyCard.store);
        } else {
            inputHolder.setStoreField(null);
        }

        if (mShowNote && !loyaltyCard.note.isEmpty()) {
            showDivider = true;
            inputHolder.setNoteField(loyaltyCard.note);
        } else {
            inputHolder.setNoteField(null);
        }

        if (mShowBalance && !loyaltyCard.balance.equals(new BigDecimal("0"))) {
            inputHolder.setExtraField(inputHolder.mBalanceField, Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType), null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mBalanceField, null, null, false);
        }

        if (mShowValidity && loyaltyCard.validFrom != null) {
            inputHolder.setExtraField(inputHolder.mValidFromField, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.validFrom), Utils.isNotYetValid(loyaltyCard.validFrom) ? Color.RED : null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mValidFromField, null, null, false);
        }

        if (mShowValidity && loyaltyCard.expiry != null) {
            inputHolder.setExtraField(inputHolder.mExpiryField, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry), Utils.hasExpired(loyaltyCard.expiry) ? Color.RED : null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mExpiryField, null, null, false);
        }

        inputHolder.mCardIcon.setContentDescription(loyaltyCard.store);
        Utils.setIconOrTextWithBackground(mContext, loyaltyCard, icon, inputHolder.mCardIcon, inputHolder.mCardText);
        inputHolder.setIconBackgroundColor(Utils.getHeaderColor(mContext, loyaltyCard));

        inputHolder.toggleCardStateIcon(loyaltyCard.starStatus != 0, loyaltyCard.archiveStatus != 0, itemSelected(inputCursor.getPosition()));

        inputHolder.itemView.setActivated(mSelectedItems.get(inputCursor.getPosition(), false));
        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());

        // Force redraw to fix size not shrinking after data change
        inputHolder.mRow.requestLayout();
    }

    private void applyClickEvents(LoyaltyCardListItemViewHolder inputHolder, final int inputPosition) {
        inputHolder.mRow.setOnClickListener(inputView -> mListener.onRowClicked(inputPosition));

        inputHolder.mRow.setOnLongClickListener(inputView -> {
            mListener.onRowLongClicked(inputPosition);
            inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        });
    }

    private boolean itemSelected(int inputPosition) {
        return mSelectedItems.get(inputPosition, false);
    }

    private void applyIconAnimation(LoyaltyCardListItemViewHolder inputHolder, int inputPosition) {
        if (itemSelected(inputPosition)) {
            inputHolder.mTickIcon.setVisibility(View.VISIBLE);
            if (mCurrentSelectedIndex == inputPosition) {
                resetCurrentIndex();
            }
        } else {
            inputHolder.mTickIcon.setVisibility(View.GONE);
            if ((mReverseAllAnimations && mAnimationItemsIndex.get(inputPosition, false)) || mCurrentSelectedIndex == inputPosition) {
                resetCurrentIndex();
            }
        }
    }

    public void toggleSelection(int inputPosition) {
        mCurrentSelectedIndex = inputPosition;
        if (mSelectedItems.get(inputPosition, false)) {
            mSelectedItems.delete(inputPosition);
            mAnimationItemsIndex.delete(inputPosition);
        } else {
            mSelectedItems.put(inputPosition, true);
            mAnimationItemsIndex.put(inputPosition, true);
        }
        notifyDataSetChanged();
    }

    public void clearSelections() {
        mReverseAllAnimations = true;
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return mSelectedItems.size();
    }

    public ArrayList<LoyaltyCard> getSelectedItems() {

        ArrayList<LoyaltyCard> result = new ArrayList<>();

        int i;
        for (i = 0; i < mSelectedItems.size(); i++) {
            mCursor.moveToPosition(mSelectedItems.keyAt(i));
            result.add(LoyaltyCard.toLoyaltyCard(mCursor));
        }

        return result;
    }

    private void resetCurrentIndex() {
        mCurrentSelectedIndex = -1;
    }

    public interface CardAdapterListener {
        void onRowClicked(int inputPosition);

        void onRowLongClicked(int inputPosition);
    }

    public class LoyaltyCardListItemViewHolder extends RecyclerView.ViewHolder {

        public TextView mCardText, mStoreField, mNoteField, mBalanceField, mValidFromField, mExpiryField;
        public ImageView mCardIcon, mStarBackground, mStarBorder, mTickIcon, mArchivedBackground;
        public MaterialCardView mRow;
        public ConstraintLayout mStar, mArchived;
        public View mDivider;

        private int mIconBackgroundColor;

        protected LoyaltyCardListItemViewHolder(LoyaltyCardLayoutBinding loyaltyCardLayoutBinding, CardAdapterListener inputListener) {
            super(loyaltyCardLayoutBinding.getRoot());
            View inputView = loyaltyCardLayoutBinding.getRoot();
            mRow = loyaltyCardLayoutBinding.row;
            mDivider = loyaltyCardLayoutBinding.infoDivider;
            mStoreField = loyaltyCardLayoutBinding.store;
            mNoteField = loyaltyCardLayoutBinding.note;
            mBalanceField = loyaltyCardLayoutBinding.balance;
            mValidFromField = loyaltyCardLayoutBinding.validFrom;
            mExpiryField = loyaltyCardLayoutBinding.expiry;
            mCardIcon = loyaltyCardLayoutBinding.thumbnail;
            mCardText = loyaltyCardLayoutBinding.thumbnailText;
            mStar = loyaltyCardLayoutBinding.star;
            mStarBackground = loyaltyCardLayoutBinding.starBackground;
            mStarBorder = loyaltyCardLayoutBinding.starBorder;
            mArchived = loyaltyCardLayoutBinding.archivedIcon;
            mArchivedBackground = loyaltyCardLayoutBinding.archiveBackground;
            mTickIcon = loyaltyCardLayoutBinding.selectedThumbnail;
            inputView.setOnLongClickListener(view -> {
                inputListener.onRowClicked(getAdapterPosition());
                inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
        }

        private void setExtraField(TextView field, String text, Integer color, boolean showDivider) {
            // If text is null, hide the field
            // If iconColor is null, use the default text and icon color based on theme
            if (text == null) {
                field.setVisibility(View.GONE);
                field.requestLayout();
                return;
            }

            // Shown when there is a name and/or note and at least 1 extra field
            if (showDivider) {
                mDivider.setVisibility(View.VISIBLE);
            }

            field.setText(text);
            field.setTextColor(color != null ? color : MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSecondary, ContextCompat.getColor(mContext, mDarkModeEnabled ? R.color.md_theme_dark_secondary : R.color.md_theme_light_secondary)));
            field.setVisibility(View.VISIBLE);

            Drawable icon = field.getCompoundDrawables()[0];
            if (icon != null) {
                icon.mutate();
                field.setCompoundDrawablesRelative(icon, null, null, null);

                if (color != null) {
                    icon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP));
                } else {
                    icon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(mDarkModeEnabled ? Color.WHITE : Color.BLACK, BlendModeCompat.SRC_ATOP));
                }
            }

            field.requestLayout();
        }

        public void setStoreField(String text) {
            if (text == null) {
                mStoreField.setVisibility(View.GONE);
            } else {
                mStoreField.setVisibility(View.VISIBLE);
                mStoreField.setText(text);
            }
            mStoreField.requestLayout();
        }

        public void setNoteField(String text) {
            if (text == null) {
                mNoteField.setVisibility(View.GONE);
            } else {
                mNoteField.setVisibility(View.VISIBLE);
                mNoteField.setText(text);
            }
            mNoteField.requestLayout();
        }

        public void toggleCardStateIcon(boolean enableStar, boolean enableArchive, boolean colorByTheme) {
            /* the below code does not work in android 5! hence the change of drawable instead
            boolean needDarkForeground = Utils.needsDarkForeground(mIconBackgroundColor);
            Drawable borderDrawable = mStarBorder.getDrawable().mutate();
            Drawable backgroundDrawable = mStarBackground.getDrawable().mutate();
            DrawableCompat.setTint(borderDrawable, needsDarkForeground ? Color.BLACK : Color.WHITE);
            DrawableCompat.setTint(backgroundDrawable, needsDarkForeground ? Color.BLACK : Color.WHITE);
            mStarBorder.setImageDrawable(borderDrawable);
            mStarBackground.setImageDrawable(backgroundDrawable);
            */
            boolean dark = Utils.needsDarkForeground(mIconBackgroundColor);
            if (colorByTheme) {
                dark = !mDarkModeEnabled;
            }

            if (dark) {
                mStarBorder.setImageResource(R.drawable.ic_unstarred_white);
                mStarBackground.setImageResource(R.drawable.ic_starred_black);
                mArchivedBackground.setImageResource(R.drawable.ic_baseline_archive_24_black);
            } else {
                mStarBorder.setImageResource(R.drawable.ic_unstarred_black);
                mStarBackground.setImageResource(R.drawable.ic_starred_white);
                mArchivedBackground.setImageResource(R.drawable.ic_baseline_archive_24);
            }

            if (enableStar) {
                mStar.setVisibility(View.VISIBLE);
            } else{
                mStar.setVisibility(View.GONE);
            }

            if (enableArchive) {
                mArchived.setVisibility(View.VISIBLE);
            } else{
                mArchived.setVisibility(View.GONE);
            }

            mStarBorder.invalidate();
            mStarBackground.invalidate();
            mArchivedBackground.invalidate();

        }

        public void setIconBackgroundColor(int color) {
            mIconBackgroundColor = color;
            mCardIcon.setBackgroundColor(color);
        }
    }

    public int dpToPx(int dp, Context mContext) {
        Resources r = mContext.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }
}
