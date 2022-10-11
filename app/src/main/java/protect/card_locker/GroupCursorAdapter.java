package protect.card_locker;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.GroupLayoutBinding;
import protect.card_locker.preferences.Settings;

public class GroupCursorAdapter extends BaseCursorAdapter<GroupCursorAdapter.GroupListItemViewHolder> {
    Settings mSettings;
    public final Context mContext;
    private final GroupAdapterListener mListener;
    SQLiteDatabase mDatabase;

    public GroupCursorAdapter(Context inputContext, Cursor inputCursor, GroupAdapterListener inputListener) {
        super(inputCursor, DBHelper.LoyaltyCardDbGroups.ORDER);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext;
        mListener = inputListener;
        mDatabase = new DBHelper(inputContext).getReadableDatabase();

        swapCursor(inputCursor);
    }

    @NonNull
    @Override
    public GroupCursorAdapter.GroupListItemViewHolder onCreateViewHolder(@NonNull ViewGroup inputParent, int inputViewType) {
        return new GroupListItemViewHolder(
                GroupLayoutBinding.inflate(
                        LayoutInflater.from(inputParent.getContext()),
                        inputParent,
                        false
                )
        );
    }

    public void onBindViewHolder(GroupListItemViewHolder inputHolder, Cursor inputCursor) {
        Group group = Group.toGroup(inputCursor);

        inputHolder.mName.setText(group._id);

        int groupCardCount = DBHelper.getGroupCardCount(mDatabase, group._id);
        int archivedCardCount = DBHelper.getArchivedCardsCount(mDatabase, group._id);

        Resources resources = mContext.getResources();

        String cardCountText;
        if (archivedCardCount > 0) {
            cardCountText = resources.getQuantityString(R.plurals.groupCardCountWithArchived, groupCardCount, groupCardCount, archivedCardCount);
        }  else {
            cardCountText = resources.getQuantityString(R.plurals.groupCardCount, groupCardCount, groupCardCount);
        }

        inputHolder.mCardCount.setText(cardCountText);
        inputHolder.mName.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        inputHolder.mCardCount.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));

        applyClickEvents(inputHolder);
    }

    private void applyClickEvents(GroupListItemViewHolder inputHolder) {
        inputHolder.mMoveDown.setOnClickListener(view -> mListener.onMoveDownButtonClicked(inputHolder.itemView));
        inputHolder.mMoveUp.setOnClickListener(view -> mListener.onMoveUpButtonClicked(inputHolder.itemView));
        inputHolder.mEdit.setOnClickListener(view -> mListener.onEditButtonClicked(inputHolder.itemView));
        inputHolder.mDelete.setOnClickListener(view -> mListener.onDeleteButtonClicked(inputHolder.itemView));
    }

    public interface GroupAdapterListener {
        void onMoveDownButtonClicked(View view);

        void onMoveUpButtonClicked(View view);

        void onEditButtonClicked(View view);

        void onDeleteButtonClicked(View view);
    }

    public static class GroupListItemViewHolder extends RecyclerView.ViewHolder {
        public TextView mName, mCardCount;
        public ImageButton mMoveUp, mMoveDown, mEdit, mDelete;

        public GroupListItemViewHolder(GroupLayoutBinding groupLayoutBinding) {
            super(groupLayoutBinding.getRoot());
            mName = groupLayoutBinding.name;
            mCardCount = groupLayoutBinding.cardCount;
            mMoveUp = groupLayoutBinding.moveUp;
            mMoveDown = groupLayoutBinding.moveDown;
            mEdit = groupLayoutBinding.edit;
            mDelete = groupLayoutBinding.delete;
        }
    }
}
