package protect.card_locker

import android.content.Context
import android.os.Bundle

class ParseResult(
    val parseResultType: ParseResultType,
    val loyaltyCard: LoyaltyCard) {
    var note: String? = null

    fun toLoyaltyCardBundle(context: Context): Bundle {
        when (parseResultType) {
            ParseResultType.FULL -> return loyaltyCard.toBundle(context, listOf())
            ParseResultType.BARCODE_ONLY -> {
                val defaultLoyaltyCard = LoyaltyCard()
                defaultLoyaltyCard.setBarcodeId(null)
                defaultLoyaltyCard.setBarcodeType(loyaltyCard.barcodeType)
                defaultLoyaltyCard.setCardId(loyaltyCard.cardId)

                return defaultLoyaltyCard.toBundle(
                    context,
                    listOf(
                        LoyaltyCard.BUNDLE_LOYALTY_CARD_BARCODE_ID,
                        LoyaltyCard.BUNDLE_LOYALTY_CARD_BARCODE_TYPE,
                        LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID
                    )
                )
            }
        }
    }
}
