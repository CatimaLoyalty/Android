package protect.card_locker;

import android.database.Cursor;

public class Group
{
    public final String _id;
    public final int order;

    public Group(final String _id, final int order) {
        this._id = _id;
        this.order = order;
    }

    public static Group toGroup(Cursor cursor)
    {
        String _id = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ID));
        int order = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ORDER));

        return new Group(_id, order);
    }
}
