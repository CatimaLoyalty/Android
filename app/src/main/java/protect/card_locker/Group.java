package protect.card_locker;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

public class Group
{
    public final String _id;
    public final int order;

    public Group(final String _id, final int order) {
        this._id = _id;
        this.order = order;
    }

    protected Group(Parcel in){
        this._id = in.readString();
        this.order = in.readInt();
    }

    public static Group toGroup(Cursor cursor)
    {
        String _id = cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ID));
        int order = cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbGroups.ORDER));

        return new Group(_id, order);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null){
            return false;
        }
        if (!(obj instanceof Group)){
            return false;
        }
        Group anotherGroup = (Group)obj;
        return _id.equals(anotherGroup._id) && order == anotherGroup.order;
    }

    @Override
    public int hashCode(){
        String combined = _id + "_" + order;
        return combined.hashCode();
    }
}
