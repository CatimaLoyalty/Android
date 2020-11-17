package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
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
        mCursor= inputCursor;
        this.mContext = inputContext;
        this.mListener = inputListener;
        mSelectedItems = new SparseBooleanArray();
        mAnimationItemsIndex = new SparseBooleanArray();

        mSettings = new Settings(inputContext);
        mDarkModeEnabled = MainActivity.isDarkModeEnabled(inputContext);
    }

    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType)
    {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.loyalty_card_layout, inputParent, false);
        return new LoyaltyCardListItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {

        if(mDarkModeEnabled)
        {
            inputHolder.mStarIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);

        inputHolder.mStoreField.setText(loyaltyCard.store);
        inputHolder.mStoreField.setTextSize(mSettings.getCardTitleListFontSize());
        if(!loyaltyCard.note.isEmpty())
        {
            inputHolder.mNoteField.setVisibility(View.VISIBLE);
            inputHolder.mNoteField.setText(loyaltyCard.note);
            inputHolder.mNoteField.setTextSize(mSettings.getCardNoteListFontSize());
        }
        else
        {
            inputHolder.mNoteField.setVisibility(View.GONE);
        }


        inputHolder.mStarIcon.setVisibility(((loyaltyCard.starStatus!=0)) ? View.VISIBLE : View.GONE);

        int tileLetterFontSize = mContext.getResources().getDimensionPixelSize(R.dimen.tileLetterFontSize);
        int pixelSize = mContext.getResources().getDimensionPixelSize(R.dimen.cardThumbnailSize);

        Integer letterBackgroundColor = loyaltyCard.headerColor;
        Integer letterTextColor = loyaltyCard.headerTextColor;
        LetterBitmap letterBitmap = new LetterBitmap(mContext, loyaltyCard.store, loyaltyCard.store,
                tileLetterFontSize, pixelSize, pixelSize, letterBackgroundColor, letterTextColor);
        inputHolder.mCardIcon.setImageBitmap(letterBitmap.getLetterTile());

        inputHolder.itemView.setActivated(mSelectedItems.get(inputCursor.getPosition(), false));
        applyIconAnimation(inputHolder, inputCursor.getPosition());
        applyClickEvents(inputHolder, inputCursor.getPosition());

    }

    private void applyClickEvents(LoyaltyCardListItemViewHolder inputHolder, final int inputPosition)
    {
        inputHolder.mThumbnailContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View inputView)
            {
                mListener.onIconClicked(inputPosition);
            }
        });

        inputHolder.mRow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View inputView)
            {
                mListener.onRowClicked(inputPosition);
            }
        });

        inputHolder.mInformationContainer.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View inputView)
            {
                mListener.onRowClicked(inputPosition);
            }
        });

        inputHolder.mRow.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View inputView)
            {
                mListener.onRowLongClicked(inputPosition);
                inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
        });

        inputHolder.mInformationContainer.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View inputView)
            {
                mListener.onRowLongClicked(inputPosition);
                inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
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

    public String getSelectedItemsID()
    {

        StringBuilder result= new StringBuilder();

        int i;
        for(i= 0; i < (mSelectedItems.size() - 1); i++)
        {
            mCursor.moveToPosition(mSelectedItems.keyAt(i));
            result.append(LoyaltyCard.toLoyaltyCard(mCursor).id).append(", ");
        }
        mCursor.moveToPosition(mSelectedItems.keyAt(i));
        result.append(LoyaltyCard.toLoyaltyCard(mCursor).id);

        return result.toString();
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

        public TextView mStoreField, mNoteField;
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
            mInformationContainer= inputView.findViewById(R.id.information_container);
            mStoreField = inputView.findViewById(R.id.store);
            mNoteField = inputView.findViewById(R.id.note);
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