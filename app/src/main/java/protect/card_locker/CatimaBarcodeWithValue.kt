package protect.card_locker

class CatimaBarcodeWithValue(
    private val mCatimaBarcode: CatimaBarcode?,
    private val mValue: String?
) {
    fun catimaBarcode(): CatimaBarcode? {
        return mCatimaBarcode
    }

    fun value(): String? {
        return mValue
    }
}
