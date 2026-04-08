package protect.card_locker.cardview;

import java.util.ArrayList;
import java.util.List;

final class LoyaltyCardImageNavigator {
    private final List<LoyaltyCardImageType> imageTypes;
    private int currentIndex;

    LoyaltyCardImageNavigator(List<LoyaltyCardImageType> imageTypes, int currentIndex) {
        this.imageTypes = new ArrayList<>(imageTypes);
        this.currentIndex = clampIndex(currentIndex);
    }

    LoyaltyCardImageType getCurrent() {
        if (isEmpty()) {
            return LoyaltyCardImageType.NONE;
        }
        return imageTypes.get(currentIndex);
    }

    boolean isEmpty() {
        return imageTypes.isEmpty();
    }

    int size() {
        return imageTypes.size();
    }

    boolean remove(LoyaltyCardImageType type) {
        int removedIndex = imageTypes.indexOf(type);
        if (removedIndex == -1) {
            return false;
        }

        imageTypes.remove(removedIndex);
        currentIndex = clampIndex(currentIndex);
        return true;
    }

    boolean canGoPrevious() {
        return currentIndex > 0;
    }

    boolean canGoNext() {
        return currentIndex < imageTypes.size() - 1;
    }

    boolean movePrevious() {
        if (!canGoPrevious()) {
            return false;
        }
        currentIndex--;
        return true;
    }

    boolean moveNext(boolean overflow) {
        if (isEmpty()) {
            return false;
        }
        if (canGoNext()) {
            currentIndex++;
            return true;
        }
        if (overflow) {
            currentIndex = 0;
            return true;
        }
        return false;
    }

    int getCurrentIndex() {
        return currentIndex;
    }

    LoyaltyCardImageType peekNext(boolean overflow) {
        if (isEmpty()) {
            return LoyaltyCardImageType.NONE;
        }

        if (canGoNext()) {
            return imageTypes.get(currentIndex + 1);
        }

        return overflow ? imageTypes.get(0) : getCurrent();
    }

    private int clampIndex(int index) {
        if (imageTypes.isEmpty()) {
            return 0;
        }

        return Math.max(0, Math.min(index, imageTypes.size() - 1));
    }
}

enum LoyaltyCardImageType {
    NONE,
    ICON,
    BARCODE,
    IMAGE_FRONT,
    IMAGE_BACK
}
