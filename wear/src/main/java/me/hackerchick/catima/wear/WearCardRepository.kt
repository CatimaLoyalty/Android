package me.hackerchick.catima.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object WearCardRepository {
    private val _cards = MutableStateFlow<List<WearCard>?>(null)
    val cards: StateFlow<List<WearCard>?> = _cards.asStateFlow()

    private val _cardDetail = MutableStateFlow<WearCard?>(null)
    val cardDetail: StateFlow<WearCard?> = _cardDetail.asStateFlow()

    private val _phoneNotReachable = MutableStateFlow(false)
    val phoneNotReachable: StateFlow<Boolean> = _phoneNotReachable.asStateFlow()

    fun setPhoneNotReachable() {
        if (_cards.value == null) _phoneNotReachable.value = true
    }

    fun updateCards(json: String) {
        _phoneNotReachable.value = false
        _cards.value = WearCard.listFromJson(json)
    }

    fun updateCardDetail(json: String) {
        _cardDetail.value = WearCard.fromJson(org.json.JSONObject(json))
    }

    fun clearCardDetail() {
        _cardDetail.value = null
    }
}
