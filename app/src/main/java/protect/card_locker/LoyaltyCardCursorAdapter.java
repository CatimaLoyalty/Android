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

import androidx.core.content.ContextCompat;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    Settings mSettings;
    boolean mDarkModeEnabled;
    private final Context mContext;
    private final CardAdapterListener mListener;
    protected SparseBooleanArray mSelectedItems;
    protected SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;
    private boolean mShowDetails = true;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener) {
        super(inputCursor, DBHelper.LoyaltyCardDbIds.ID);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext.getApplicationContext();
        mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);

        swapCursor(inputCursor);
    }

    public void showDetails(boolean show) {
        mShowDetails = show;
        notifyDataSetChanged();
    }

    public boolean showingDetails() {
        return mShowDetails;
    }

    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.loyalty_card_layout, inputParent, false);
        return new LoyaltyCardListItemViewHolder(itemView, mListener);
    }

    public LoyaltyCard getCard(int position) {
        mCursor.moveToPosition(position);
        return LoyaltyCard.toLoyaltyCard(mCursor);
    }

    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        // Invisible until we want to show something more
        inputHolder.mDivider.setVisibility(View.GONE);

        int size = mSettings.getFontSizeMax(mSettings.getSmallFont());

        if (mDarkModeEnabled) {
            inputHolder.mStarIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
        }

        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);

        inputHolder.mStoreField.setText(loyaltyCard.store);
        inputHolder.mStoreField.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        if (mShowDetails && !loyaltyCard.note.isEmpty()) {
            inputHolder.mNoteField.setVisibility(View.VISIBLE);
            inputHolder.mNoteField.setText(loyaltyCard.note);
            inputHolder.mNoteField.setTextSize(size);
        } else {
            inputHolder.mNoteField.setVisibility(View.GONE);
        }

        if (mShowDetails && !loyaltyCard.balance.equals(new BigDecimal("0"))) {
            int drawableSize = dpToPx((size * 24) / 14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mBalanceField.setVisibility(View.VISIBLE);
            Drawable balanceIcon = inputHolder.mBalanceField.getCompoundDrawables()[0];
            balanceIcon.setBounds(0, 0, drawableSize, drawableSize);
            inputHolder.mBalanceField.setCompoundDrawablesRelative(balanceIcon, null, null, null);
            if (mDarkModeEnabled) {
                balanceIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mBalanceField.setText(Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType));
            inputHolder.mBalanceField.setTextSize(size);
        } else {
            inputHolder.mBalanceField.setVisibility(View.GONE);
        }

        if (mShowDetails && loyaltyCard.expiry != null) {
            int drawableSize = dpToPx((size * 24) / 14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mExpiryField.setVisibility(View.VISIBLE);
            Drawable expiryIcon = inputHolder.mExpiryField.getCompoundDrawables()[0];
            expiryIcon.setBounds(0, 0, drawableSize, drawableSize);
            inputHolder.mExpiryField.setCompoundDrawablesRelative(expiryIcon, null, null, null);
            if (Utils.hasExpired(loyaltyCard.expiry)) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.SRC_ATOP));
                inputHolder.mExpiryField.setTextColor(Color.RED);
            } else if (mDarkModeEnabled) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mExpiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry));
            inputHolder.mExpiryField.setTextSize(size);
        } else {
            inputHolder.mExpiryField.setVisibility(View.GONE);
        }

        Bitmap cardIcon = Utils.retrieveCardImage(mContext, loyaltyCard.id, ImageLocationType.icon);
        if (cardIcon != null) {
            inputHolder.mCardIcon.setImageBitmap(cardIcon);
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        inputHolder.mIconLayout.setBackgroundColor(loyaltyCard.headerColor != null ? loyaltyCard.headerColor : ContextCompat.getColor(mContext, R.color.colorPrimary));

        inputHolder.mStarIcon.setVisibility(loyaltyCard.starStatus != 0 ? View.VISIBLE : View.GONE);

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

    private void applyIconAnimation(LoyaltyCardListItemViewHolder inputHolder, int inputPosition) {
        if (mSelectedItems.get(inputPosition, false)) {
            inputHolder.mCardIcon.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mTickIcon);
            inputHolder.mTickIcon.setVisibility(View.VISIBLE);
            if (mCurrentSelectedIndex == inputPosition) {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mTickIcon, inputHolder.mCardIcon, true);
                resetCurrentIndex();
            }
        } else {
            inputHolder.mTickIcon.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mCardIcon);
            inputHolder.mCardIcon.setVisibility(View.VISIBLE);
            if ((mReverseAllAnimations && mAnimationItemsIndex.get(inputPosition, false)) || mCurrentSelectedIndex == inputPosition) {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mTickIcon, inputHolder.mCardIcon, false);
                resetCurrentIndex();
            }
        }
    }

    private void resetIconYAxis(View inputView) {
        if (inputView.getRotationY() != 0) {
            inputView.setRotationY(0);
        }
    }

    public void resetAnimationIndex() {
        mReverseAllAnimations = false;
        mAnimationItemsIndex.clear();
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

    public static class LoyaltyCardListItemViewHolder extends RecyclerView.ViewHolder {

        public TextView mStoreField, mNoteField, mBalanceField, mExpiryField;
        public ImageView mCardIcon, mStarIcon, mTickIcon;
        public MaterialCardView mIconLayout, mRow;
        public View mDivider;

        public LoyaltyCardListItemViewHolder(View inputView, CardAdapterListener inputListener) {
            super(inputView);
            mRow = inputView.findViewById(R.id.row);
            mIconLayout = inputView.findViewById(R.id.icon_layout);
            mDivider = inputView.findViewById(R.id.info_divider);
            mStoreField = inputView.findViewById(R.id.store);
            mNoteField = inputView.findViewById(R.id.note);
            mBalanceField = inputView.findViewById(R.id.balance);
            mExpiryField = inputView.findViewById(R.id.expiry);

            mCardIcon = inputView.findViewById(R.id.thumbnail);
            mStarIcon = inputView.findViewById(R.id.star);
            mTickIcon = inputView.findViewById(R.id.selected_thumbnail);
            inputView.setOnLongClickListener(view -> {
                inputListener.onRowClicked(getAdapterPosition());
                inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
        }
    }

    public int dpToPx(int dp, Context mContext) {
        Resources r = mContext.getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }
}
