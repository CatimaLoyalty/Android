package protect.card_locker;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class ManageGroupActivityInGroupState implements Parcelable {
    HashMap<Integer, Boolean> map;



    public ManageGroupActivityInGroupState(HashMap<Integer, Boolean> in){
        map = (HashMap<Integer, Boolean>)in.clone();
    }

    @Override
    public int describeContents(){
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(map.size());

        for(Map.Entry<Integer, Boolean> entry : map.entrySet()){
            dest.writeInt(entry.getKey());
            dest.writeInt(entry.getValue() ? 1 : 0);
        }
    }

    protected ManageGroupActivityInGroupState(Parcel in){
        map = new HashMap<Integer, Boolean>();
        int length = in.readInt();
        for (int i = 0;i < length; i++){
            int key = in.readInt();
            boolean value = in.readInt() == 1 ? true : false;
            map.put(key, value);
        }
    }

    public static final Creator<ManageGroupActivityInGroupState> CREATOR = new Creator<ManageGroupActivityInGroupState>() {
        @Override
        public ManageGroupActivityInGroupState createFromParcel(Parcel in) {
            return new ManageGroupActivityInGroupState(in);
        }

        @Override
        public ManageGroupActivityInGroupState[] newArray(int size) {
            return new ManageGroupActivityInGroupState[size];
        }
    };

    public HashMap<Integer, Boolean> getMap(){
        return (HashMap<Integer,Boolean>)map.clone();
    }
}
