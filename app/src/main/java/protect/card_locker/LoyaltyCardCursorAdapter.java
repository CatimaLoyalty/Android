package protect.card_locker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;
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

    private final int MAGIC_NUMBER_ARCHIVE_REFERENCE = -100;

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

    public void setCards(Cursor loyaltyCardCursor, int archiveCount) {
        if (archiveCount == 0) {
            swapCursor(loyaltyCardCursor);
        } else {
            // Build fake DB entry
            MatrixCursor archiveReference = new MatrixCursor(new String[] {
                    DBHelper.LoyaltyCardDbIds.ID,
                    DBHelper.LoyaltyCardDbIds.STORE,
                    DBHelper.LoyaltyCardDbIds.NOTE,
                    DBHelper.LoyaltyCardDbIds.EXPIRY,
                    DBHelper.LoyaltyCardDbIds.BALANCE,
                    DBHelper.LoyaltyCardDbIds.BALANCE_TYPE,
                    DBHelper.LoyaltyCardDbIds.CARD_ID,
                    DBHelper.LoyaltyCardDbIds.BARCODE_ID,
                    DBHelper.LoyaltyCardDbIds.BARCODE_TYPE,
                    DBHelper.LoyaltyCardDbIds.STAR_STATUS,
                    DBHelper.LoyaltyCardDbIds.LAST_USED,
                    DBHelper.LoyaltyCardDbIds.ZOOM_LEVEL,
                    DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS,
                    DBHelper.LoyaltyCardDbIds.HEADER_COLOR
            });
            archiveReference.addRow(new Object[] {
                    MAGIC_NUMBER_ARCHIVE_REFERENCE,
                    mContext.getString(R.string.archiveList),
                    "",
                    null,
                    new BigDecimal(0),
                    null,
                    "",
                    "",
                    null,
                    0,
                    0,
                    0,
                    0,
                    Color.WHITE
            });
            Cursor[] cursors = { loyaltyCardCursor, archiveReference };
            Cursor extendedCursor = new MergeCursor(cursors);

            swapCursor(extendedCursor);
        }
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

    @Override
    public LoyaltyCardListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.loyalty_card_layout, inputParent, false);
        return new LoyaltyCardListItemViewHolder(itemView, mListener);
    }

    public LoyaltyCard getCard(int position) {
        mCursor.moveToPosition(position);
        return LoyaltyCard.toLoyaltyCard(mCursor);
    }

    public void open(int position) {
        LoyaltyCard loyaltyCard = getCard(position);

        if (loyaltyCard == null) {
            throw new CursorIndexOutOfBoundsException("Requested position doesn't exist");
        }

        Intent i;
        if (isArchiveReference(loyaltyCard)) {
            i = new Intent(mContext, MainActivity.class);
            Bundle bundle = new Bundle();
            bundle.putBoolean("archiveMode", true);
            i.putExtras(bundle);
        } else {
            i = new Intent(mContext, LoyaltyCardViewActivity.class);
            i.setAction("");
            final Bundle b = new Bundle();
            b.putInt("id", loyaltyCard.id);
            i.putExtras(b);

            ShortcutHelper.updateShortcuts(mContext, loyaltyCard);
        }

        mContext.startActivity(i);
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
            inputHolder.setBalanceField(loyaltyCard.balance, loyaltyCard.balanceType);
        } else {
            inputHolder.setBalanceField(null, null);
        }

        if (mShowDetails && loyaltyCard.expiry != null) {
            inputHolder.setExpiryField(loyaltyCard.expiry);
        } else {
            inputHolder.setExpiryField(null);
        }

        setHeaderHeight(inputHolder, mShowDetails);

        Bitmap cardIcon;
        if (isArchiveReference(loyaltyCard)) {
            cardIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_baseline_archive_24);
        } else {
            cardIcon = Utils.retrieveCardImage(mContext, loyaltyCard.id, ImageLocationType.icon);
        }

        if (cardIcon != null) {
            inputHolder.mCardIcon.setImageBitmap(cardIcon);
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            inputHolder.mCardIcon.setImageBitmap(Utils.generateIcon(mContext, loyaltyCard.store, loyaltyCard.headerColor).getLetterTile());
            inputHolder.mCardIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        inputHolder.setIconBackgroundColor(loyaltyCard.headerColor != null ? loyaltyCard.headerColor : R.attr.colorPrimary);

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

        public TextView mStoreField, mNoteField, mBalanceField, mExpiryField;
        public ImageView mCardIcon, mStarBackground, mStarBorder, mTickIcon, mArchivedBackground;
        public MaterialCardView mRow, mIconLayout;
        public ConstraintLayout mStar, mArchived;
        public View mDivider;

        private int mIconBackgroundColor;

        protected LoyaltyCardListItemViewHolder(View inputView, CardAdapterListener inputListener) {
            super(inputView);
            mRow = inputView.findViewById(R.id.row);
            mDivider = inputView.findViewById(R.id.info_divider);
            mStoreField = inputView.findViewById(R.id.store);
            mNoteField = inputView.findViewById(R.id.note);
            mBalanceField = inputView.findViewById(R.id.balance);
            mExpiryField = inputView.findViewById(R.id.expiry);
            mIconLayout = inputView.findViewById(R.id.icon_layout);
            mCardIcon = inputView.findViewById(R.id.thumbnail);
            mStar = inputView.findViewById(R.id.star);
            mStarBackground = inputView.findViewById(R.id.star_background);
            mStarBorder = inputView.findViewById(R.id.star_border);
            mArchived = inputView.findViewById(R.id.archivedIcon);
            mArchivedBackground = inputView.findViewById(R.id.archive_background);
            mTickIcon = inputView.findViewById(R.id.selected_thumbnail);
            inputView.setOnLongClickListener(view -> {
                inputListener.onRowClicked(getAdapterPosition());
                inputView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            });
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

        public void setBalanceField(BigDecimal balance, Currency balanceType) {
            if (balance == null) {
                mBalanceField.setVisibility(View.GONE);
            } else {
                int size = mSettings.getFontSizeMax(mSettings.getSmallFont());
                int drawableSize = dpToPx((size * 24) / 14, mContext);
                mDivider.setVisibility(View.VISIBLE);
                mBalanceField.setVisibility(View.VISIBLE);
                Drawable balanceIcon = mBalanceField.getCompoundDrawables()[0];
                if (balanceIcon != null) {
                    balanceIcon.setBounds(0, 0, drawableSize, drawableSize);
                    mBalanceField.setCompoundDrawablesRelative(balanceIcon, null, null, null);
                    if (mDarkModeEnabled) {
                        balanceIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
                    }
                }
                mBalanceField.setText(Utils.formatBalance(mContext, balance, balanceType));
                mBalanceField.setTextSize(size);
            }
            mBalanceField.requestLayout();
        }

        public void setExpiryField(Date expiry) {
            if (expiry == null) {
                mExpiryField.setVisibility(View.GONE);
            } else {
                int size = mSettings.getFontSizeMax(mSettings.getSmallFont());
                int drawableSize = dpToPx((size * 24) / 14, mContext);
                mDivider.setVisibility(View.VISIBLE);
                mExpiryField.setVisibility(View.VISIBLE);
                Drawable expiryIcon = mExpiryField.getCompoundDrawables()[0];
                if (expiryIcon != null) {
                    expiryIcon.setBounds(0, 0, drawableSize, drawableSize);
                    mExpiryField.setCompoundDrawablesRelative(expiryIcon, null, null, null);
                    if (Utils.hasExpired(expiry)) {
                        expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.RED, BlendModeCompat.SRC_ATOP));
                    } else if (mDarkModeEnabled) {
                        expiryIcon.setColorFilter(BlendModeColorFilterCompat.createBlendModeColorFilterCompat(Color.WHITE, BlendModeCompat.SRC_ATOP));
                    }
                }
                mExpiryField.setText(DateFormat.getDateInstance(DateFormat.LONG).format(expiry));
                if (Utils.hasExpired(expiry)) {
                    mExpiryField.setTextColor(Color.RED);
                }
                mExpiryField.setTextSize(size);
            }
            mExpiryField.requestLayout();
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

    private boolean isArchiveReference(LoyaltyCard loyaltyCard) {
        return loyaltyCard.id == MAGIC_NUMBER_ARCHIVE_REFERENCE;
    }
}
