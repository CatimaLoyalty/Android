package protect.card_locker;

import android.database.Cursor;

import java.io.Serializable;

import androidx.annotation.Nullable;

public class Group
{
    public final String _id;

    public Group(final String _id)
    {
        this._id = _id;
    }

    public static Group toGroup(Cursor cursor)
    {
        String _id = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ID));

        return new Group(_id);
    }
}
