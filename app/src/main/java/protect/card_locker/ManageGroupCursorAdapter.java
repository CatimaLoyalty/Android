package protect.card_locker;

import android.content.Context;
import android.database.Cursor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageGroupCursorAdapter extends LoyaltyCardCursorAdapter {
    private HashMap<Integer, Integer> mIndexCardMap;
    private HashMap<Integer, Boolean> mInGroupOverlay;
    private HashMap<Integer, Boolean> mIsLoyaltyCardInGroupCache;
    private HashMap<Integer, List<Group>> mGetGroupCache;
    final private Group mGroup;
    final private DBHelper mDb;

    public ManageGroupCursorAdapter(Context inputContext, Cursor inputCursor, CardAdapterListener inputListener, Group group) {
        super(inputContext, inputCursor, inputListener);
        mGroup = new Group(group._id, group.order);
        mInGroupOverlay = new HashMap<>();
        mDb = new DBHelper(inputContext);
    }

    @Override
    public void swapCursor(Cursor inputCursor) {
        super.swapCursor(inputCursor);
        mIndexCardMap = new HashMap<>();
        mIsLoyaltyCardInGroupCache = new HashMap<>();
        mGetGroupCache = new HashMap<>();
    }

    @Override
    public void onBindViewHolder(LoyaltyCardListItemViewHolder inputHolder, Cursor inputCursor) {
        LoyaltyCard loyaltyCard = LoyaltyCard.toLoyaltyCard(inputCursor);
        Boolean overlayValue = mInGroupOverlay.get(loyaltyCard.id);
        if ((overlayValue != null ? overlayValue : isLoyaltyCardInGroup(loyaltyCard.id))) {
            mAnimationItemsIndex.put(inputCursor.getPosition(), true);
            mSelectedItems.put(inputCursor.getPosition(), true);
        }
        mIndexCardMap.put(inputCursor.getPosition(), loyaltyCard.id);
        super.onBindViewHolder(inputHolder, inputCursor);
    }

    private List<Group> getGroups(int cardId) {
        List<Group> cache = mGetGroupCache.get(cardId);
        if (cache != null) {
            return cache;
        }
        List<Group> groups = mDb.getLoyaltyCardGroups(cardId);
        mGetGroupCache.put(cardId, groups);
        return groups;
    }

    private boolean isLoyaltyCardInGroup(int cardId) {
        Boolean cache = mIsLoyaltyCardInGroupCache.get(cardId);
        if (cache != null) {
            return cache;
        }
        List<Group> groups = getGroups(cardId);
        if (groups.contains(mGroup)) {
            mIsLoyaltyCardInGroupCache.put(cardId, true);
            return true;
        }
        mIsLoyaltyCardInGroupCache.put(cardId, false);
        return false;
    }

    @Override
    public void toggleSelection(int inputPosition) {
        super.toggleSelection(inputPosition);
        Integer cardId = mIndexCardMap.get(inputPosition);
        if (cardId == null) {
            throw (new RuntimeException("cardId should not be null here"));
        }
        Boolean overlayValue = mInGroupOverlay.get(cardId);
        if (overlayValue == null) {
            mInGroupOverlay.put(cardId, !isLoyaltyCardInGroup(cardId));
        } else {
            mInGroupOverlay.remove(cardId);
        }
    }

    public boolean hasChanged() {
        return mInGroupOverlay.size() > 0;
    }

    public void commitToDatabase() {
        for (Map.Entry<Integer, Boolean> entry : mInGroupOverlay.entrySet()) {
            int cardId = entry.getKey();
            List<Group> groups = getGroups(cardId);
            if (entry.getValue()) {
                groups.add(mGroup);
            } else {
                groups.remove(mGroup);
            }
            mDb.setLoyaltyCardGroups(cardId, groups);
        }
    }

    public void importInGroupState(HashMap<Integer, Boolean> cardIdInGroupMap) {
        mInGroupOverlay = new HashMap<>(cardIdInGroupMap);
    }

    public HashMap<Integer, Boolean> exportInGroupState() {
        return new HashMap<>(mInGroupOverlay);
    }

    public int getCountFromCursor() {
        return super.getCursor().getCount();
    }
}
