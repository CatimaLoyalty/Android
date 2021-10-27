package protect.card_locker;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.preferences.Settings;

public class ManageGroupCursorAdapter extends BaseCursorAdapter<ManageGroupCursorAdapter.ManageGroupListItemViewHolder> {
    private int mCurrentSelectedIndex = -1;
    private Cursor mCursor;
    Settings mSettings;
    boolean mDarkModeEnabled;
    private Context mContext;
    private CardAdapterListener mListener;
    private SparseBooleanArray mSelectedItems;
    private SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;
    private HashMap<Integer, ManageGroupLoyaltyCard> mIndexCardMap;
    private HashMap<Integer, Boolean> mInGroupOverlay;


    public ManageGroupCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener) {
        super(inputCursor);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext;
        mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mDarkModeEnabled = Utils.isDarkModeEnabled(inputContext);


        mInGroupOverlay = new HashMap<Integer, Boolean>();
        swapCursor(mCursor);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        mIndexCardMap = new HashMap<Integer, ManageGroupLoyaltyCard>();

        super.swapCursor(inputCursor);
        mCursor = inputCursor;

    }

    @Override
    public ManageGroupListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.manage_group_loyalty_card_layout, inputParent, false);
        return new ManageGroupListItemViewHolder(itemView, mListener);
    }

    public Cursor getCursor() {
        return mCursor;
    }

    public void onBindViewHolder(ManageGroupListItemViewHolder inputHolder, Cursor inputCursor) {
        // Invisible until we want to show something more
        inputHolder.mDivider.setVisibility(View.GONE);

        int size = mSettings.getFontSizeMax(mSettings.getSmallFont());

        if (mDarkModeEnabled) {
            inputHolder.mStarIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
        }

        ManageGroupLoyaltyCard cardEntry = ManageGroupLoyaltyCard.toCard(inputCursor);

        inputHolder.mStoreField.setText(cardEntry.store);
        inputHolder.mStoreField.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        if (!cardEntry.note.isEmpty()) {
            inputHolder.mNoteField.setVisibility(View.VISIBLE);
            inputHolder.mNoteField.setText(cardEntry.note);
            inputHolder.mNoteField.setTextSize(size);
        } else {
            inputHolder.mNoteField.setVisibility(View.GONE);
        }

        if (!cardEntry.balance.equals(new BigDecimal("0"))) {
            int drawableSize = dpToPx((size*24)/14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mBalanceField.setVisibility(View.VISIBLE);
            Drawable balanceIcon = inputHolder.mBalanceField.getCompoundDrawables()[0];
            balanceIcon.setBounds(0,0,drawableSize,drawableSize);
            inputHolder.mBalanceField.setCompoundDrawablesRelative(balanceIcon, null, null, null);
            if (mDarkModeEnabled) {
                balanceIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mBalanceField.setText(Utils.formatBalance(mContext, cardEntry.balance, cardEntry.balanceType));
            inputHolder.mBalanceField.setTextSize(size);
        } else {
            inputHolder.mBalanceField.setVisibility(View.GONE);
        }

        if (cardEntry.expiry != null) {
            int drawableSize = dpToPx((size*24)/14, mContext);
            inputHolder.mDivider.setVisibility(View.VISIBLE);
            inputHolder.mExpiryField.setVisibility(View.VISIBLE);
            Drawable expiryIcon = inputHolder.mExpiryField.getCompoundDrawables()[0];
            expiryIcon.setBounds(0,0, drawableSize, drawableSize);
            inputHolder.mExpiryField.setCompoundDrawablesRelative(expiryIcon, null, null, null);
            if (Utils.hasExpired(cardEntry.expiry)) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.SRC_ATOP));
                inputHolder.mExpiryField.setTextColor(Color.RED);
            } else if (mDarkModeEnabled) {
                expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
            }
            inputHolder.mExpiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(cardEntry.expiry));
            inputHolder.mExpiryField.setTextSize(size);
        } else {
            inputHolder.mExpiryField.setVisibility(View.GONE);
        }

        // inputHolder.mStarIcon.setVisibility(cardEntry.starStatus != 0 ? View.VISIBLE : View.GONE);
        inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, cardEntry.store, cardEntry.headerColor).getLetterTile());
        int imageSize = dpToPx( (size*46)/14, mContext);
        inputHolder.mCardIcon.getLayoutParams().height = imageSize;
        inputHolder.mCardIcon.getLayoutParams().width = imageSize;
        inputHolder.mStarIcon.getLayoutParams().height = imageSize;
        inputHolder.mStarIcon.getLayoutParams().width = imageSize;
        inputHolder.mTickIcon.getLayoutParams().height = imageSize;
        inputHolder.mTickIcon.getLayoutParams().width = imageSize;

        /* Changing Padding and Margin of different views according to font size
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

        Boolean overlayValue = mInGroupOverlay.get(cardEntry.id);
        if((overlayValue != null? overlayValue: cardEntry.is_in_group)) {
            mAnimationItemsIndex.put(inputCursor.getPosition(), true);
            mSelectedItems.put(inputCursor.getPosition(), true);
        }

        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());

        mIndexCardMap.put(inputCursor.getPosition(), cardEntry);
    }

    private void applyClickEvents(ManageGroupListItemViewHolder inputHolder, final int inputPosition) {
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

    private void applyIconAnimation(ManageGroupListItemViewHolder inputHolder, int inputPosition) {
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
        ManageGroupLoyaltyCard cardEntry = mIndexCardMap.get(inputPosition);
        Boolean overlayValue = mInGroupOverlay.get(cardEntry.id);
        if (overlayValue == null){
            mInGroupOverlay.put(cardEntry.id, !cardEntry.is_in_group);
        }else{
            mInGroupOverlay.put(cardEntry.id, !overlayValue);
        }

        notifyDataSetChanged();
    }

    private void resetCurrentIndex() {
        mCurrentSelectedIndex = -1;
    }

    public interface CardAdapterListener {
        void onRowClicked(int inputPosition);

        void onRowLongClicked(int inputPosition);
    }

    private HashMap<Integer, ManageGroupLoyaltyCard> fetchWholeQuery (){
        HashMap<Integer, ManageGroupLoyaltyCard> res = new HashMap<Integer, ManageGroupLoyaltyCard>();
        int oldPosition = mCursor.getPosition();
        mCursor.moveToFirst();
        while(!mCursor.isAfterLast()){
            ManageGroupLoyaltyCard cardEntry = ManageGroupLoyaltyCard.toCard(mCursor);
            res.put(cardEntry.id, cardEntry);
            mCursor.moveToNext();
        }
        mCursor.moveToPosition(oldPosition);
        return res;
    }

    public void commitToDatabase(Context context, String groupId){
        DBHelper dbHelper = new DBHelper(context);
        HashMap<Integer, ManageGroupLoyaltyCard> cache = fetchWholeQuery();
        for(Map.Entry<Integer, Boolean> entry : mInGroupOverlay.entrySet()){
            ManageGroupLoyaltyCard cardEntry = cache.get(entry.getKey());
            if (cardEntry == null){
                Log.d("commitToDatabase", "card with id " + entry.getKey() + " was removed from database unexpectedly");
                continue;
            }
            if (entry.getValue() != cardEntry.is_in_group) {
                if (entry.getValue()) {
                    dbHelper.addLoyaltyCardToGroup(entry.getKey(), groupId);
                } else {
                    dbHelper.removeLoyaltyCardFromGroup(entry.getKey(), groupId);
                }
            }
        }
    }

    public boolean hasChanged(){
        HashMap<Integer, ManageGroupLoyaltyCard> cache = fetchWholeQuery();
        for(Map.Entry<Integer, Boolean> entry : mInGroupOverlay.entrySet()){
            ManageGroupLoyaltyCard cardEntry = cache.get(entry.getKey());
            if(cardEntry.is_in_group != entry.getValue()){
                return true;
            }
        }
        return false;
    }

    public int getCountFromCursor(){
        return mCursor.getCount();
    }

    public HashMap<Integer, Boolean> exportInGroupState(){
        return (HashMap<Integer, Boolean>)mInGroupOverlay.clone();
    }

    public void importInGroupState(HashMap<Integer, Boolean> cardIdInGroupMap){
        mInGroupOverlay = (HashMap<Integer, Boolean>)cardIdInGroupMap.clone();
    }


    public static class ManageGroupListItemViewHolder extends RecyclerView.ViewHolder {

        public TextView mStoreField, mNoteField, mBalanceField, mExpiryField;
        public LinearLayout mInformationContainer;
        public ImageView mCardIcon, mStarIcon, mTickIcon;
        public MaterialCardView mRow;
        public View mDivider;
        public RelativeLayout mThumbnailFrontContainer, mThumbnailBackContainer;

        public ManageGroupListItemViewHolder(View inputView, CardAdapterListener inputListener) {
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

    public int dpToPx(int dp, Context mContext){
        Resources r = mContext.getResources();
        int px = (int)TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        return px;
    }
}
