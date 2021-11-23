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
import protect.card_locker.preferences.Settings;

class GroupCursorAdapter extends BaseCursorAdapter<GroupCursorAdapter.GroupListItemViewHolder> {
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
    public GroupCursorAdapter.GroupListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.group_layout, inputParent, false);
        return new GroupListItemViewHolder(itemView);
    }

    public Cursor getCursor() {
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
        public AppCompatImageButton mMoveUp, mMoveDown, mEdit, mDelete;

        public GroupListItemViewHolder(View inputView) {
            super(inputView);
            mName = inputView.findViewById(R.id.name);
            mCardCount = inputView.findViewById(R.id.cardCount);
            mMoveUp = inputView.findViewById(R.id.moveUp);
            mMoveDown = inputView.findViewById(R.id.moveDown);
            mEdit = inputView.findViewById(R.id.edit);
            mDelete = inputView.findViewById(R.id.delete);
        }
    }
}
