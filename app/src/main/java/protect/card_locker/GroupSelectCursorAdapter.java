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

public class GroupSelectCursorAdapter extends BaseCursorAdapter<GroupSelectCursorAdapter.GroupListItemViewHolder> {
    Settings mSettings;
    private final Context mContext;
    private final GroupAdapterListener mListener;
    DBHelper mDb;

    public GroupSelectCursorAdapter(Context inputContext, Cursor inputCursor, GroupAdapterListener inputListener) {
        super(inputCursor, DBHelper.LoyaltyCardDbGroups.ORDER);
        setHasStableIds(true);
        mSettings = new Settings(inputContext);
        mContext = inputContext.getApplicationContext();
        mListener = inputListener;
        mDb = new DBHelper(inputContext);

        swapCursor(inputCursor);
    }

    @NonNull
    @Override
    public GroupSelectCursorAdapter.GroupListItemViewHolder onCreateViewHolder(ViewGroup inputParent, int inputViewType) {
        View itemView = LayoutInflater.from(inputParent.getContext()).inflate(R.layout.group_select_layout, inputParent, false);
        return new GroupListItemViewHolder(itemView);
    }

    public void onBindViewHolder(GroupListItemViewHolder inputHolder, Cursor inputCursor) {
        Group group = Group.toGroup(inputCursor);

        inputHolder.mName.setText(group._id);

        int groupCardCount = mDb.getGroupCardCount(group._id);
        inputHolder.mCardCount.setText(mContext.getResources().getQuantityString(R.plurals.groupCardCount, groupCardCount, groupCardCount));

        inputHolder.mName.setTextSize(mSettings.getFontSizeMax(mSettings.getMediumFont()));
        inputHolder.mCardCount.setTextSize(mSettings.getFontSizeMax(mSettings.getSmallFont()));

        applyClickEvents(inputHolder);
    }

    private void applyClickEvents(GroupListItemViewHolder inputHolder) {
        inputHolder.mSelect.setOnClickListener(view -> mListener.onSelectButtonClicked(inputHolder.itemView));
    }

    public interface GroupAdapterListener {
        void onSelectButtonClicked(View view);
    }

    public static class GroupListItemViewHolder extends RecyclerView.ViewHolder {
        public TextView mName, mCardCount;
        public AppCompatImageButton mSelect;

        public GroupListItemViewHolder(View inputView) {
            super(inputView);
            mName = inputView.findViewById(R.id.name);
            mCardCount = inputView.findViewById(R.id.cardCount);
            mSelect = inputView.findViewById(R.id.select);
        }
    }
}
