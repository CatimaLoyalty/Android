package protect.card_locker;

import android.database.Cursor;

import androidx.recyclerview.widget.RecyclerView;

public abstract class BaseCursorAdapter<V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {
    private Cursor mCursor;
    private boolean mDataValid;
    private int mRowIDColumn;

    public BaseCursorAdapter(Cursor inputCursor) {
        setHasStableIds(true);
        swapCursor(inputCursor);
    }

    public abstract void onBindViewHolder(V inputHolder, Cursor inputCursor);

    @Override
    public void onBindViewHolder(V inputHolder, int inputPosition) {
        if (!mDataValid) {
            throw new IllegalStateException("Cannot bind view holder when cursor is in invalid state.");
        }

        if (!mCursor.moveToPosition(inputPosition)) {
            throw new IllegalStateException("Could not move cursor to position " + inputPosition + " when trying to bind view holder");
        }

        onBindViewHolder(inputHolder, mCursor);
    }

    @Override
    public int getItemCount() {
        if (mDataValid) {
            return mCursor.getCount();
        } else {
            return 0;
        }
    }

    @Override
    public long getItemId(int inputPosition) {
        if (!mDataValid) {
            throw new IllegalStateException("Cannot lookup item id when cursor is in invalid state.");
        }

        if (!mCursor.moveToPosition(inputPosition)) {
            throw new IllegalStateException("Could not move cursor to position " + inputPosition + " when trying to get an item id");
        }

        return mCursor.getLong(mRowIDColumn);
    }

    public void swapCursor(Cursor inputCursor) {
        if (inputCursor == mCursor) {
            return;
        }

        if (inputCursor != null) {
            mCursor = inputCursor;
            mDataValid = true;
            notifyDataSetChanged();
        } else {
            notifyItemRangeRemoved(0, getItemCount());
            mCursor = null;
            mRowIDColumn = -1;
            mDataValid = false;
        }
    }
}