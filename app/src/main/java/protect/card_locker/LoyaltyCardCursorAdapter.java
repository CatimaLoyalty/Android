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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.LoyaltyCardLayoutBinding;
import protect.card_locker.preferences.Settings;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    Settings mSettings;
    boolean mDarkModeEnabled;
    public final Context mContext;
    private final CardAdapterListener mListener;
    protected SparseBooleanArray mSelectedItems;
    protected SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;
    private boolean mShowDetails;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener) {
        super(inputCursor, DBHelper.LoyaltyCardDbIds.ID);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext;
        mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);

        refreshState();

        swapCursor(inputCursor);
    }

    public void refreshState() {
        // Retrieve user details preference
        SharedPreferences cardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        mShowDetails = cardDetailsPref.getBoolean(mContext.getString(R.string.sharedpreference_card_details_show), true);
    }

    public void showDetails(boolean show) {
        mShowDetails = show;
        notifyDataSetChanged();

        // Store in Shared Preference to restore next adapter launch
        SharedPreferences cardDetailsPref = mContext.getSharedPreferences(
                mContext.getString(R.string.sharedpreference_card_details),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor cardDetailsPrefEditor = cardDetailsPref.edit();
        cardDetailsPrefEditor.putBoolean(mContext.getString(R.string.sharedpreference_card_details_show), show);
        cardDetailsPrefEditor.apply();
    }

    public boolean showingDetails() {
        return mShowDetails;
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
        inputHolder.mDivider.setVisibility(View.GONE);

        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);

        inputHolder.setStoreField(loyaltyCard.store);
        if (mShowDetails && !loyaltyCard.note.isEmpty()) {
            inputHolder.setNoteField(loyaltyCard.note);
        } else {
            inputHolder.setNoteField(null);
        }

        if (mShowDetails && !loyaltyCard.balance.equals(new BigDecimal("0"))) {
            inputHolder.setExtraField(inputHolder.mBalanceField, Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType), null);
        } else {
            inputHolder.setExtraField(inputHolder.mBalanceField, null, null);
        }

        if (mShowDetails && loyaltyCard.validFrom != null) {
            inputHolder.setExtraField(inputHolder.mValidFromField, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.validFrom), Utils.isNotYetValid(loyaltyCard.validFrom) ? Color.RED : null);
        } else {
            inputHolder.setExtraField(inputHolder.mValidFromField, null, null);
        }

        if (mShowDetails && loyaltyCard.expiry != null) {
            inputHolder.setExtraField(inputHolder.mExpiryField, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry), Utils.hasExpired(loyaltyCard.expiry) ? Color.RED : null);
        } else {
            inputHolder.setExtraField(inputHolder.mExpiryField, null, null);
        }

        setHeaderHeight(inputHolder, mShowDetails);
        Bitmap cardIcon = Utils.retrieveCardImage(mContext, loyaltyCard.id, ImageLocationType.icon);
        if (cardIcon != null) {
            inputHolder.mCardIcon.setImageBitmap(cardIcon);
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        inputHolder.setIconBackgroundColor(loyaltyCard.headerColor != null ? loyaltyCard.headerColor : androidx.appcompat.R.attr.colorPrimary);

        inputHolder.toggleCardStateIcon(loyaltyCard.starStatus != 0, loyaltyCard.archiveStatus != 0, itemSelected(inputCursor.getPosition()));

        inputHolder.itemView.setActivated(mSelectedItems.get(inputCursor.getPosition(), false));
        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());

        // Force redraw to fix size not shrinking after data change
        inputHolder.mRow.requestLayout();
    }

    private void setHeaderHeight(LoyaltyCardListItemViewHolder inputHolder, boolean expanded) {
        int iconHeight;
        if (expanded) {
            iconHeight = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            iconHeight = (int) mContext.getResources().getDimension(R.dimen.cardThumbnailSize);
        }

        inputHolder.mIconLayout.getLayoutParams().height = expanded ? 0 : iconHeight;
        inputHolder.mCardIcon.getLayoutParams().height = iconHeight;
        inputHolder.mTickIcon.getLayoutParams().height = iconHeight;
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

        public TextView mStoreField, mNoteField, mBalanceField, mValidFromField, mExpiryField;
        public ImageView mCardIcon, mStarBackground, mStarBorder, mTickIcon, mArchivedBackground;
        public MaterialCardView mRow, mIconLayout;
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
            mIconLayout = loyaltyCardLayoutBinding.iconLayout;
            mCardIcon = loyaltyCardLayoutBinding.thumbnail;
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

        private void setExtraField(TextView field, String text, Integer color) {
            // If text is null, hide the field
            // If iconColor is null, use the default text and icon color based on theme
            if (text == null) {
                field.setVisibility(View.GONE);
                field.requestLayout();
                return;
            }

            int size = mSettings.getFontSizeMax(mSettings.getSmallFont());

            field.setVisibility(View.VISIBLE);
            field.setText(text);
            field.setTextSize(size);
            field.setTextColor(color != null ? color : MaterialColors.getColor(mContext, com.google.android.material.R.attr.colorSecondary, ContextCompat.getColor(mContext, mDarkModeEnabled ? R.color.md_theme_dark_secondary : R.color.md_theme_light_secondary)));

            int drawableSize = dpToPx((size * 24) / 14, mContext);
            mDivider.setVisibility(View.VISIBLE);
            field.setVisibility(View.VISIBLE);
            Drawable icon = field.getCompoundDrawables()[0];
            if (icon != null) {
                icon.mutate();
                icon.setBounds(0, 0, drawableSize, drawableSize);
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
            mStoreField.setText(text);
            mStoreField.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
            mStoreField.requestLayout();
        }

        public void setNoteField(String text) {
            if (text == null) {
                mNoteField.setVisibility(View.GONE);
            } else {
                mNoteField.setVisibility(View.VISIBLE);
                mNoteField.setText(text);
                mNoteField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
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
