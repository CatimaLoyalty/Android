package protect.card_locker;

public interface BarcodeValuesListDisambiguatorCallback {
    void onUserChoseBarcode(BarcodeValues barcodeValues);
    void onUserDismissedSelector();
}
