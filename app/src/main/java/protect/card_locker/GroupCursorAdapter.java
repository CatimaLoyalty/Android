package protect.card_locker;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.databinding.GroupLayoutBinding;
import protect.card_locker.preferences.Settings;

class GroupCursorAdapter extends BaseCursorAdapter<GroupCursorAdapter.GroupListItemViewHolder>
{
    Settings mSettings;
    private Cursor mCursor;
    private final Context mContext;
    private final GroupCursorAdapter.GroupAdapterListener mListener;
    DBHelper mDb;

    public GroupCursorAdapter(Context inputContext, Cursor inputCursor, GroupCursorAdapter.GroupAdapterListener inputListener) {
        super(inputCursor);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext;
        mListener = inputListener;
        mDb = new DBHelper(inputContext);

        swapCursor(mCursor);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        super.swapCursor(inputCursor);
        mCursor = inputCursor;
    }

    @NonNull
    @Override
    public GroupCursorAdapter.GroupListItemViewHolder onCreateViewHolder(@NonNull ViewGroup inputParent, int inputViewType)
    {
        GroupLayoutBinding binding = GroupLayoutBinding.inflate(LayoutInflater.from(inputParent.getContext()), inputParent, false);
        return new GroupListItemViewHolder(binding);
    }

    public Cursor getCursor()
    {
        return mCursor;
    }

    public void onBindViewHolder(GroupCursorAdapter.GroupListItemViewHolder inputHolder, Cursor inputCursor) {
        Group group = Group.toGroup(inputCursor);

        inputHolder.mName.setText(group._id);

        int groupCardCount = mDb.getGroupCardCount(group._id);
        inputHolder.mCardCount.setText(mContext.getResources().getQuantityString(R.plurals.groupCardCount, groupCardCount, groupCardCount));

        inputHolder.mName.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        inputHolder.mCardCount.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));

        applyClickEvents(inputHolder);
    }

    private void applyClickEvents(GroupListItemViewHolder inputHolder)
    {
        inputHolder.mMoveDown.setOnClickListener(view -> mListener.onMoveDownButtonClicked(inputHolder.itemView, inputHolder.mName));
        inputHolder.mMoveUp.setOnClickListener(view -> mListener.onMoveUpButtonClicked(inputHolder.itemView, inputHolder.mName));
        inputHolder.mEdit.setOnClickListener(view -> mListener.onEditButtonClicked(inputHolder.itemView, inputHolder.mName));
        inputHolder.mDelete.setOnClickListener(view -> mListener.onDeleteButtonClicked(inputHolder.itemView, inputHolder.mName));
    }

    public interface GroupAdapterListener
    {
        void onMoveDownButtonClicked(View view, TextView name);
        void onMoveUpButtonClicked(View view, TextView name);
        void onEditButtonClicked(View view, TextView name);
        void onDeleteButtonClicked(View view, TextView name);
    }

    public static class GroupListItemViewHolder extends RecyclerView.ViewHolder
    {
        public TextView mName, mCardCount;
        public AppCompatImageButton mMoveUp, mMoveDown, mEdit, mDelete;

        public GroupListItemViewHolder(GroupLayoutBinding binding) {
            super(binding.getRoot());
            mName = binding.name;
            mCardCount = binding.cardCount;
            mMoveUp = binding.moveUp;
            mMoveDown = binding.moveDown;
            mEdit = binding.edit;
            mDelete = binding.delete;
        }
    }
}
