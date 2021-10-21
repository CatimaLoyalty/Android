package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.preferences.Settings;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    private Cursor mCursor;
    Settings mSettings;
    boolean mDarkModeEnabled;
    private Context mContext;
    private CardAdapterListener mListener;
    private SparseBooleanArray mSelectedItems;
    private SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener) {
        super(inputCursor);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext;
        mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = MainActivity.isDarkModeEnabled(inputContext);

        swapCursor(mCursor);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        super.swapCursor(inputCursor);
        mCursor = inputCursor;
    }

    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.loyalty_card_layout, inputParent, false);
        return new LoyaltyCardListItemViewHolder(itemView, mListener);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        // Invisible until we want to show something more
        inputHolder.mDivider.setVisibility(View.GONE);

        if (mDarkModeEnabled) {
            inputHolder.mStarIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
        }

        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);

        inputHolder.mStoreField.setText(loyaltyCard.store);
        inputHolder.mStoreField.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        if (!loyaltyCard.note.isEmpty()) {
            inputHolder.mNoteField.setVisibility(View.VISIBLE);
            inputHolder.mNoteField.setText(loyaltyCard.note);
            inputHolder.mNoteField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
        } else {
            inputHolder.mNoteField.setVisibility(View.GONE);
        }

        if (!loyaltyCard.balance.equals(new BigDecimal("0"))) {
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mBalanceField.setVisibility(View.VISIBLE);
            if (mDarkModeEnabled) {
                inputHolder.mBalanceField.getCompoundDrawables()[0].setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mBalanceField.setText(Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType));
            inputHolder.mBalanceField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
        } else {
            inputHolder.mBalanceField.setVisibility(View.GONE);
        }

        if (loyaltyCard.expiry != null) {
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mExpiryField.setVisibility(View.VISIBLE);
            Drawable expiryIcon = inputHolder.mExpiryField.getCompoundDrawables()[0];
            if (Utils.hasExpired(loyaltyCard.expiry)) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.SRC_ATOP));
                inputHolder.mExpiryField.setTextColor(Color.RED);
            } else if (mDarkModeEnabled) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mExpiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry));
            inputHolder.mExpiryField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
        } else {
            inputHolder.mExpiryField.setVisibility(View.GONE);
        }

        inputHolder.mStarIcon.setVisibility(loyaltyCard.starStatus != 0 ? View.VISIBLE : View.GONE);
        inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
        int imageSize = mSettings.getFontSizeMax(mSettings.getSmallFont());
        inputHolder.mCardIcon.getLayoutParams().height = imageSize*6;
        inputHolder.mCardIcon.getLayoutParams().width = imageSize*6;
        inputHolder.mStarIcon.getLayoutParams().height = imageSize*6;
        inputHolder.mStarIcon.getLayoutParams().width = imageSize*6;
        inputHolder.mTickIcon.getLayoutParams().height = imageSize*6;
        inputHolder.mTickIcon.getLayoutParams().width = imageSize*6;

        inputHolder.itemView.setActivated(mSelectedItems.get(inputCursor.getPosition(), false));
        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());
    }

    private void applyClickEvents(LoyaltyCardListItemViewHolder inputHolder, final int inputPosition) {
        inputHolder.mRow.setOnClickListener(inputView -> mListener.onRowClicked(inputPosition));
        inputHolder.mInformationContainer.setOnClickListener(inputView -> mListener.onRowClicked(inputPosition));

        inputHolder.mRow.setOnLongClickListener(inputView -> {
            mListener.onRowLongClicked(inputPosition);
            inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        });

        inputHolder.mInformationContainer.setOnLongClickListener(inputView -> {
            mListener.onRowLongClicked(inputPosition);
            inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        });
    }

    private void applyIconAnimation(LoyaltyCardListItemViewHolder inputHolder, int inputPosition) {
        if (mSelectedItems.get(inputPosition, false)) {
            inputHolder.mThumbnailFrontContainer.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mThumbnailBackContainer);
            inputHolder.mThumbnailBackContainer.setVisibility(View.VISIBLE);
            inputHolder.mThumbnailBackContainer.setAlpha(1);
            if (mCurrentSelectedIndex == inputPosition) {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mThumbnailBackContainer, inputHolder.mThumbnailFrontContainer, true);
                resetCurrentIndex();
            }
        } else {
            inputHolder.mThumbnailBackContainer.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mThumbnailFrontContainer);
            inputHolder.mThumbnailFrontContainer.setVisibility(View.VISIBLE);
            inputHolder.mThumbnailFrontContainer.setAlpha(1);
            if ((mReverseAllAnimations && mAnimationItemsIndex.get(inputPosition, false)) || mCurrentSelectedIndex == inputPosition) {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mThumbnailBackContainer, inputHolder.mThumbnailFrontContainer, false);
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
        public LinearLayout mInformationContainer;
        public ImageView mCardIcon, mStarIcon, mTickIcon;
        public MaterialCardView mRow;
        public View mDivider;
        public RelativeLayout mThumbnailFrontContainer, mThumbnailBackContainer;

        public LoyaltyCardListItemViewHolder(View inputView, CardAdapterListener inputListener) {
            super(inputView);
            mRow = inputView.findViewById(R.id.row);
            mDivider = inputView.findViewById(R.id.info_divider);
            mThumbnailFrontContainer = inputView.findViewById(R.id.thumbnail_front);
            mThumbnailBackContainer = inputView.findViewById(R.id.thumbnail_back);
            mInformationContainer = inputView.findViewById(R.id.information_container);
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
}