package me.hackerchick.catima.wear

import android.content.Context
import androidx.core.content.edit

object WearCardStore {
    private const val PREFS_NAME = "catima_wear_cards"
    private const val KEY_CARDS_JSON = "cards_json"

    fun load(context: Context): List<WearCard>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CARDS_JSON, null) ?: return null
        return runCatching { WearCard.listFromJson(json) }.getOrNull()
    }

    fun save(context: Context, cards: List<WearCard>) {
        val json = WearCard.listToJson(cards)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_CARDS_JSON, json)
            }
    }
}
