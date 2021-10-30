package protect.card_locker;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.BlendModeColorFilterCompat;
import androidx.core.graphics.BlendModeCompat;
import androidx.recyclerview.widget.RecyclerView;

import protect.card_locker.preferences.Settings;

public class ManageGroupCursorAdapter extends LoyaltyCardCursorAdapter {
    private HashMap<Integer, Integer> mIndexCardMap;
    private HashMap<Integer, Boolean> mInGroupOverlay;
    private Group mGroup;
    private DBHelper mDb;
    public ManageGroupCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener, Group group){
        super(inputContext, inputCursor, inputListener);
        mGroup = new Group(group._id, group.order);
        mInGroupOverlay = new HashMap<Integer, Boolean>();
        mDb = new DBHelper(inputContext);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        super.swapCursor(inputCursor);
        mIndexCardMap = new HashMap<Integer, Integer>();
    }

    @Override
    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor){
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);
        Boolean overlayValue = mInGroupOverlay.get(loyaltyCard.id);
        if((overlayValue != null? overlayValue: isLoyaltyCardInGroup(loyaltyCard.id))) {
            mAnimationItemsIndex.put(inputCursor.getPosition(), true);
            mSelectedItems.put(inputCursor.getPosition(), true);
        }
        mIndexCardMap.put(inputCursor.getPosition(), loyaltyCard.id);
        super.onBindViewHolder(inputHolder, inputCursor);
    }

    private boolean isLoyaltyCardInGroup(int cardId){
        List<Group> groups = mDb.getLoyaltyCardGroups(cardId);
        Iterator<Group> groupItr = groups.listIterator();
        while(groupItr.hasNext()){
            if (groupItr.next().equals(mGroup)){
                return true;
            }
        }
        return false;
    }

    @Override
    public void toggleSelection(int inputPosition){
        super.toggleSelection(inputPosition);
        int cardId = mIndexCardMap.get(inputPosition);
        Boolean overlayValue = mInGroupOverlay.get(cardId);
        if (overlayValue == null){
            mInGroupOverlay.put(cardId, !isLoyaltyCardInGroup(cardId));
        }else{
            mInGroupOverlay.remove(cardId);
        }
    }

    public boolean hasChanged() {
        return mInGroupOverlay.size() > 0;
    }

    public void commitToDatabase(Context context){
        // this is very inefficient but done to keep the size of DBHelper low
        for(Map.Entry<Integer, Boolean> entry: mInGroupOverlay.entrySet()){
            int cardId = entry.getKey();
            List<Group> groups = mDb.getLoyaltyCardGroups(cardId);
            if(entry.getValue()){
                groups.add(mGroup);
            }else{
                groups.remove(mGroup);
            }
            mDb.setLoyaltyCardGroups(cardId, groups);
        }
    }

    public void importInGroupState(HashMap<Integer, Boolean> cardIdInGroupMap) {
        mInGroupOverlay = (HashMap<Integer, Boolean>) cardIdInGroupMap.clone();
    }

    public HashMap<Integer, Boolean> exportInGroupState(){
         return (HashMap<Integer, Boolean>)mInGroupOverlay.clone();
    }

    public int getCountFromCursor() {
        return super.getCursor().getCount();
    }
}
