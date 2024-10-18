package protect.card_locker

import android.os.Bundle

class ParseResult(
    val parseResultType: ParseResultType,
    val loyaltyCard: LoyaltyCard) {
    var note: String? = null

    fun toLoyaltyCardBundle(): Bundle {
        when (parseResultType) {
            ParseResultType.FULL -> return loyaltyCard.toBundle(false)
            ParseResultType.BARCODE_ONLY -> {
                val defaultLoyaltyCard = LoyaltyCard()
                defaultLoyaltyCard.setBarcodeType(loyaltyCard.barcodeType)
                defaultLoyaltyCard.setCardId(loyaltyCard.cardId)

                return defaultLoyaltyCard.toBundle(true)
            }
        }
    }
}