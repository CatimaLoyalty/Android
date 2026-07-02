package protect.card_locker

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject

class WearDataService : WearableListenerService() {

    companion object {
        private const val TAG = "CatimaWearDataService"
        private const val PATH_CARDS_REQUEST = "/catima/cards/request"
        private const val PATH_CARDS_RESPONSE = "/catima/cards/response"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "Received message: ${messageEvent.path} from ${messageEvent.sourceNodeId}")
        when (messageEvent.path) {
            PATH_CARDS_REQUEST -> sendCardsToNode(messageEvent.sourceNodeId)
        }
    }

    private fun sendCardsToNode(nodeId: String) {
        try {
            val dbHelper = DBHelper(this)
            val db = dbHelper.readableDatabase
            val cursor = db.query(
                DBHelper.LoyaltyCardDbIds.TABLE,
                arrayOf(
                    DBHelper.LoyaltyCardDbIds.ID,
                    DBHelper.LoyaltyCardDbIds.STORE,
                    DBHelper.LoyaltyCardDbIds.CARD_ID,
                    DBHelper.LoyaltyCardDbIds.BARCODE_ID,
                    DBHelper.LoyaltyCardDbIds.BARCODE_TYPE,
                    DBHelper.LoyaltyCardDbIds.HEADER_COLOR,
                    DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS,
                ),
                "${DBHelper.LoyaltyCardDbIds.ARCHIVE_STATUS} = 0",
                null,
                null,
                null,
                "${DBHelper.LoyaltyCardDbIds.STORE} COLLATE NOCASE ASC"
            )

            val array = JSONArray()
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.ID)
                val storeIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.STORE)
                val cardIdIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.CARD_ID)
                val barcodeIdIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_ID)
                val barcodeTypeIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.BARCODE_TYPE)
                val headerColorIdx = it.getColumnIndexOrThrow(DBHelper.LoyaltyCardDbIds.HEADER_COLOR)

                while (it.moveToNext()) {
                    val obj = JSONObject()
                    obj.put("id", it.getInt(idIdx))
                    obj.put("store", it.getString(storeIdx) ?: "")
                    obj.put("cardId", it.getString(cardIdIdx) ?: "")
                    obj.put("barcodeId", if (it.isNull(barcodeIdIdx)) JSONObject.NULL else it.getString(barcodeIdIdx))
                    obj.put("barcodeType", if (it.isNull(barcodeTypeIdx)) JSONObject.NULL else it.getString(barcodeTypeIdx))
                    obj.put("headerColor", if (it.isNull(headerColorIdx)) JSONObject.NULL else it.getInt(headerColorIdx))
                    array.put(obj)
                }
            }

            val json = array.toString()
            val messageClient = Wearable.getMessageClient(this)
            messageClient.sendMessage(nodeId, PATH_CARDS_RESPONSE, json.toByteArray(Charsets.UTF_8))
                .addOnSuccessListener { Log.d(TAG, "Cards sent to node $nodeId (${array.length()} cards)") }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to send cards to node $nodeId", e) }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading cards from DB", e)
        }
    }
}
