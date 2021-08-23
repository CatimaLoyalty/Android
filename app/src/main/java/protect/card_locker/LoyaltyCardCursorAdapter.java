package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import protect.card_locker.preferences.Settings;

public class LoyaltyCardCursorAdapter extends BaseCursorAdapter<LoyaltyCardCursorAdapter.LoyaltyCardListItemViewHolder>
{
    private static int mCurrentSelectedIndex = -1;
    private Cursor mCursor;
    Settings mSettings;
    boolean mDarkModeEnabled;
    private Context mContext;
    private CardAdapterListener mListener;
    private SparseBooleanArray mSelectedItems;
    private SparseBooleanArray mAnimationItemsIndex;
    private boolean mReverseAllAnimations = false;

    public LoyaltyCardCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener)
    {
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
    public LoyaltyCardListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType)
    {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.loyalty_card_layout, inputParent, false);
        return new LoyaltyCardListItemViewHolder(itemView);
    }

    public Cursor getCursor()
    {
        return mCursor;
    }

    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        if (mDarkModeEnabled) {
            inputHolder.mStarIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
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
            inputHolder.mBalanceField.setVisibility(View.VISIBLE);
            inputHolder.mBalanceField.setText(mContext.getString(R.string.balanceSentence, Utils.formatBalance(mContext, loyaltyCard.balance, loyaltyCard.balanceType)));
            inputHolder.mBalanceField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
        } else {
            inputHolder.mBalanceField.setVisibility(View.GONE);
        }

        if (loyaltyCard.expiry != null)
        {
            inputHolder.mExpiryField.setVisibility(View.VISIBLE);
            int expiryString = R.string.expiryStateSentence;
            if(Utils.hasExpired(loyaltyCard.expiry)) {
                expiryString = R.string.expiryStateSentenceExpired;
                inputHolder.mExpiryField.setTextColor(mContext.getResources().getColor(R.color.alert));
            }
            inputHolder.mExpiryField.setText(mContext.getString(expiryString, DateFormat.getDateInstance(DateFormat.LONG).format(loyaltyCard.expiry)));
            inputHolder.mExpiryField.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));
        } else {
            inputHolder.mExpiryField.setVisibility(View.GONE);
        }

        inputHolder.mStarIcon.setVisibility((loyaltyCard.starStatus != 0) ? View.VISIBLE : View.GONE);

        Bitmap cardIcon = Utils.retrieveCardImage(mContext, loyaltyCard.id, ImageType.icon);
        if (cardIcon != null) {
            inputHolder.mCardIcon.setImageBitmap(cardIcon);
        } else {
            inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
        }
        inputHolder.mCardIcon.setBackgroundColor(loyaltyCard.headerColor);

        inputHolder.itemView.setActivated(mSelectedItems.get(inputCursor.getPosition(), false));
        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());

    }

    private void applyClickEvents(LoyaltyCardListItemViewHolder inputHolder, final int inputPosition)
    {
        inputHolder.mThumbnailContainer.setOnClickListener(inputView -> mListener.onIconClicked(inputPosition));
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

    private void applyIconAnimation(LoyaltyCardListItemViewHolder inputHolder, int inputPosition)
    {
        if (mSelectedItems.get(inputPosition, false))
        {
            inputHolder.mThumbnailFrontContainer.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mThumbnailBackContainer);
            inputHolder.mThumbnailBackContainer.setVisibility(View.VISIBLE);
            inputHolder.mThumbnailBackContainer.setAlpha(1);
            if (mCurrentSelectedIndex == inputPosition)
            {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mThumbnailBackContainer, inputHolder.mThumbnailFrontContainer, true);
                resetCurrentIndex();
            }
        }
        else
        {
            inputHolder.mThumbnailBackContainer.setVisibility(View.GONE);
            resetIconYAxis(inputHolder.mThumbnailFrontContainer);
            inputHolder.mThumbnailFrontContainer.setVisibility(View.VISIBLE);
            inputHolder.mThumbnailFrontContainer.setAlpha(1);
            if ((mReverseAllAnimations && mAnimationItemsIndex.get(inputPosition, false)) || mCurrentSelectedIndex == inputPosition)
            {
                LoyaltyCardAnimator.flipView(mContext, inputHolder.mThumbnailBackContainer, inputHolder.mThumbnailFrontContainer, false);
                resetCurrentIndex();
            }
        }
    }

    private void resetIconYAxis(View inputView)
    {
        if (inputView.getRotationY() != 0)
        {
            inputView.setRotationY(0);
        }
    }

    public void resetAnimationIndex()
    {
        mReverseAllAnimations = false;
        mAnimationItemsIndex.clear();
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void toggleSelection(int inputPosition)
    {
        mCurrentSelectedIndex = inputPosition;
        if (mSelectedItems.get(inputPosition, false))
        {
            mSelectedItems.delete(inputPosition);
            mAnimationItemsIndex.delete(inputPosition);
        }
        else
        {
            mSelectedItems.put(inputPosition, true);
            mAnimationItemsIndex.put(inputPosition, true);
        }
        notifyItemChanged(inputPosition);
    }

    public void clearSelections()
    {
        mReverseAllAnimations = true;
        mSelectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount()
    {
        return mSelectedItems.size();
    }

    public ArrayList<LoyaltyCard> getSelectedItems()
    {

        ArrayList<LoyaltyCard> result = new ArrayList<>();

        int i;
        for(i = 0; i < mSelectedItems.size(); i++)
        {
            mCursor.moveToPosition(mSelectedItems.keyAt(i));
            result.add(LoyaltyCard.toLoyaltyCard(mCursor));
        }

        return result;
    }

    private void resetCurrentIndex()
    {
        mCurrentSelectedIndex = -1;
    }

    public interface CardAdapterListener
    {
        void onIconClicked(int inputPosition);
        void onRowClicked(int inputPosition);
        void onRowLongClicked(int inputPosition);
    }

    public class LoyaltyCardListItemViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener
    {

        public TextView mStoreField, mNoteField, mBalanceField, mExpiryField;
        public LinearLayout mInformationContainer;
        public ImageView mCardIcon, mStarIcon;
        public CardView mThumbnailContainer;
        public ConstraintLayout mRow;
        public RelativeLayout mThumbnailFrontContainer, mThumbnailBackContainer;

        public LoyaltyCardListItemViewHolder(View inputView)
        {
            super(inputView);
            mThumbnailContainer = inputView.findViewById(R.id.thumbnail_container);
            mRow = inputView.findViewById(R.id.row);
            mThumbnailFrontContainer = inputView.findViewById(R.id.thumbnail_front);
            mThumbnailBackContainer = inputView.findViewById(R.id.thumbnail_back);
            mInformationContainer = inputView.findViewById(R.id.information_container);
            mStoreField = inputView.findViewById(R.id.store);
            mNoteField = inputView.findViewById(R.id.note);
            mBalanceField = inputView.findViewById(R.id.balance);
            mExpiryField = inputView.findViewById(R.id.expiry);
            mCardIcon = inputView.findViewById(R.id.thumbnail);
            mStarIcon = inputView.findViewById(R.id.star);
            inputView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View inputView)
        {
            mListener.onRowLongClicked(getAdapterPosition());
            inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }
}