package protect.card_locker;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import protect.card_locker.databinding.LoyaltyCardLayoutBinding;
import protect.card_locker.preferences.Settings;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    boolean mDarkModeEnabled;
    public final Context mContext;
    private final CardAdapterListener mListener;
    private final LoyaltyCardListDisplayOptionsManager mLoyaltyCardListDisplayOptions;
    protected SparseBooleanArray mSelectedItems;
    protected SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener, Runnable inputSwapCursorCallback) {
        super(inputCursor, DBHelper.LoyaltyCardDbIds.ID);
        setHasStableIds(true);
        mContext = inputContext;
        mListener = inputListener;

        Runnable refreshCardsCallback = () -> notifyDataSetChanged();

        mLoyaltyCardListDisplayOptions = new LoyaltyCardListDisplayOptionsManager(mContext, refreshCardsCallback, inputSwapCursorCallback);
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);

        swapCursor(inputCursor);
    }

    public void showDisplayOptionsDialog() {
        mLoyaltyCardListDisplayOptions.showDisplayOptionsDialog();
    }

    public boolean showingArchivedCards() {
        return mLoyaltyCardListDisplayOptions.showingArchivedCards();
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
        return LoyaltyCard.fromCursor(mContext, mCursor);
    }

    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        // Invisible until we want to show something more
        boolean showDivider = false;
        inputHolder.mDivider.setVisibility(View.GONE);

        LoyaltyCard loyaltyCard = LoyaltyCard.fromCursor(mContext, inputCursor);
        Bitmap icon = loyaltyCard.getImageThumbnail(mContext);

        if (mLoyaltyCardListDisplayOptions.showingNameBelowThumbnail() && icon != null) {
            showDivider = true;
            inputHolder.setStoreField(loyaltyCard.store);
        } else {
            inputHolder.setStoreField(null);
        }

        if (mLoyaltyCardListDisplayOptions.showingNote() && !loyaltyCard.note.isEmpty()) {
            showDivider = true;
            inputHolder.setNoteField(loyaltyCard.note);
        } else {
            inputHolder.setNoteField(null);
        }

        if (mLoyaltyCardListDisplayOptions.showingBalance() && !loyaltyCard.balance.equals(new BigDecimal("0"))) {
            inputHolder.setExtraField(inputHolder.mBalanceField, Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType), null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mBalanceField, null, null, false);
        }

        if (mLoyaltyCardListDisplayOptions.showingValidity() && loyaltyCard.validFrom != null) {
            inputHolder.setExtraField(inputHolder.mValidFromField, loyaltyCard.validFrom.format(Utils.mediumFormatter), Utils.isNotYetValid(loyaltyCard.validFrom) ? Color.RED : null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mValidFromField, null, null, false);
        }

        if (mLoyaltyCardListDisplayOptions.showingValidity() && loyaltyCard.expiry != null) {
            inputHolder.setExtraField(inputHolder.mExpiryField, loyaltyCard.expiry.format(Utils.mediumFormatter), Utils.hasExpired(loyaltyCard.expiry) ? Color.RED : null, showDivider);
        } else {
            inputHolder.setExtraField(inputHolder.mExpiryField, null, null, false);
        }

        inputHolder.mCardIcon.setContentDescription(loyaltyCard.store);
        Utils.setIconOrTextWithBackground(mContext, loyaltyCard, icon, inputHolder.mCardIcon, inputHolder.mCardText, new Settings(mContext).getPreferredColumnCount());

        inputHolder.toggleCardStateIcon(loyaltyCard.starStatus != 0, loyaltyCard.archiveStatus != 0);

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
            result.add(LoyaltyCard.fromCursor(mContext, mCursor));
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
        public ImageView mCardIcon, mTickIcon;
        public MaterialCardView mRow;
        public ConstraintLayout mStar, mArchived;
        public View mDivider;

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
            mArchived = loyaltyCardLayoutBinding.archivedIcon;
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

        public void toggleCardStateIcon(boolean enableStar, boolean enableArchive) {
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
        }
    }
}
