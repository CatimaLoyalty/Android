package me.hackerchick.catima.wear

import org.json.JSONArray
import org.json.JSONObject

data class WearCard(
    val id: Int,
    val store: String,
    val cardId: String,
    val barcodeId: String?,
    val barcodeType: String?,
    val headerColor: Int?,
    val note: String?,
    val validFrom: Long?,
    val expiry: Long?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("store", store)
        put("cardId", cardId)
        put("barcodeId", barcodeId ?: JSONObject.NULL)
        put("barcodeType", barcodeType ?: JSONObject.NULL)
        put("headerColor", headerColor ?: JSONObject.NULL)
        put("note", note ?: JSONObject.NULL)
        put("validFrom", validFrom ?: JSONObject.NULL)
        put("expiry", expiry ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(json: JSONObject): WearCard = WearCard(
            id = json.getInt("id"),
            store = json.getString("store"),
            cardId = json.getString("cardId"),
            barcodeId = if (json.isNull("barcodeId")) null else json.optString("barcodeId").takeIf { it.isNotEmpty() },
            barcodeType = if (json.isNull("barcodeType")) null else json.optString("barcodeType").takeIf { it.isNotEmpty() },
            headerColor = if (json.isNull("headerColor")) null else json.getInt("headerColor"),
            note = if (json.isNull("note")) null else json.optString("note").takeIf { it.isNotEmpty() },
            validFrom = if (json.isNull("validFrom")) null else json.optLong("validFrom").takeIf { it > 0 },
            expiry = if (json.isNull("expiry")) null else json.optLong("expiry").takeIf { it > 0 },
        )

        fun listToJson(cards: List<WearCard>): String {
            val array = JSONArray()
            cards.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(json: String): List<WearCard> {
            val array = JSONArray(json)
            return (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
        }
    }
}
