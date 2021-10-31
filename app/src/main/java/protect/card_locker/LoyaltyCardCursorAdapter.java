package protect.card_locker;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
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

import androidx.annotation.NonNull;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.LoyaltyCardLayoutBinding;
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

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);

        swapCursor(mCursor);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        super.swapCursor(inputCursor);
        mCursor = inputCursor;
    }

    @NonNull
    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(@NonNull ViewGroup inputParent, int inputViewType) {
        LoyaltyCardLayoutBinding binding = LoyaltyCardLayoutBinding.inflate(LayoutInflater.from(inputParent.getContext()), inputParent, false);
        return new LoyaltyCardListItemViewHolder(binding, mListener);
    }

    public Cursor getCursor() {
        return mCursor;
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
        if (!loyaltyCard.note.isEmpty()) {
            inputHolder.mNoteField.setVisibility(View.VISIBLE);
            inputHolder.mNoteField.setText(loyaltyCard.note);
            inputHolder.mNoteField.setTextSize(size);
        } else {
            inputHolder.mNoteField.setVisibility(View.GONE);
        }

        if (!loyaltyCard.balance.equals(new BigDecimal("0"))) {
            int drawableSize = dpToPx((size*24)/14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mBalanceField.setVisibility(View.VISIBLE);
            Drawable balanceIcon = inputHolder.mBalanceField.getCompoundDrawables()[0];
            balanceIcon.setBounds(0,0,drawableSize,drawableSize);
            inputHolder.mBalanceField.setCompoundDrawablesRelative(balanceIcon, null, null, null);
            if (mDarkModeEnabled) {
                balanceIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mBalanceField.setText(Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType));
            inputHolder.mBalanceField.setTextSize(size);
        } else {
            inputHolder.mBalanceField.setVisibility(View.GONE);
        }

        if (loyaltyCard.expiry != null) {
            int drawableSize = dpToPx((size*24)/14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mExpiryField.setVisibility(View.VISIBLE);
            Drawable expiryIcon = inputHolder.mExpiryField.getCompoundDrawables()[0];
            expiryIcon.setBounds(0,0, drawableSize, drawableSize);
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

        inputHolder.mStarIcon.setVisibility(loyaltyCard.starStatus != 0 ? View.VISIBLE : View.GONE);
        inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
        int imageSize = dpToPx( (size*46)/14, mContext);
        inputHolder.mCardIcon.getLayoutParams().height = imageSize;
        inputHolder.mCardIcon.getLayoutParams().width = imageSize;
        inputHolder.mStarIcon.getLayoutParams().height = imageSize;
        inputHolder.mStarIcon.getLayoutParams().width = imageSize;
        inputHolder.mTickIcon.getLayoutParams().height = imageSize;
        inputHolder.mTickIcon.getLayoutParams().width = imageSize;

        /* Changing Padding and Mragin of different views according to font size
        * Views Included:
        * a) InformationContainer padding
        * b) Store left padding
        * c) Divider Margin
        * d) note top margin
        * e) row margin
        * */
        int marginPaddingSize = dpToPx((size*16)/14, mContext );
        inputHolder.mInformationContainer.setPadding(marginPaddingSize, marginPaddingSize, marginPaddingSize, marginPaddingSize);
        inputHolder.mStoreField.setPadding(marginPaddingSize, 0, 0, 0);
        LinearLayout.LayoutParams lpDivider = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        lpDivider.setMargins(0, marginPaddingSize, 0, marginPaddingSize);
        inputHolder.mDivider.setLayoutParams(lpDivider);
        LinearLayout.LayoutParams lpNoteField = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        lpNoteField.setMargins(0, marginPaddingSize/2, 0, 0);
        inputHolder.mNoteField.setLayoutParams(lpNoteField);
        LinearLayout.LayoutParams lpRow = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT );
        lpRow.setMargins(marginPaddingSize/2, marginPaddingSize/2, marginPaddingSize/2, marginPaddingSize/2);
        inputHolder.mRow.setLayoutParams(lpRow);

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

        public LoyaltyCardListItemViewHolder(LoyaltyCardLayoutBinding binding, CardAdapterListener inputListener) {
            super(binding.getRoot());
            mRow = binding.row;
            mDivider = binding.infoDivider;
            mThumbnailFrontContainer = binding.thumbnailFront;
            mThumbnailBackContainer = binding.thumbnailBack;
            mInformationContainer = binding.informationContainer;
            mStoreField = binding.store;
            mNoteField = binding.note;
            mBalanceField = binding.balance;
            mExpiryField = binding.expiry;
            mCardIcon = binding.thumbnail;
            mStarIcon = binding.star;
            mTickIcon = binding.selectedThumbnail;
            binding.row.setOnLongClickListener(view -> {
                inputListener.onRowClicked(getAdapterPosition());
                binding.row.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
        }
    }

    public int dpToPx(int dp, Context mContext){
        Resources r = mContext.getResources();
        int px = (int)TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }
}
