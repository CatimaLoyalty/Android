package protect.card_locker;

import android.database.Cursor;

import androidx.annotation.Nullable;

public class Group {
    public final int _id;
    public final String name;
    public final int order;

    public Group(final int _id, final String name, final int order) {
        this._id = _id;
        this.name = name;
        this.order = order;
    }

    public static Group toGroup(Cursor cursor) {
        int _id = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ID));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.NAME));
        int order = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ORDER));

        return new Group(_id, name, order);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Group)) {
            return false;
        }
        Group anotherGroup = (Group) obj;
        return _id == anotherGroup._id && order == anotherGroup.order;
    }

    @Override
    public int hashCode() {
        String combined = _id + "_" + order;
        return combined.hashCode();
    }
}
