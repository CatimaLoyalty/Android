package me.hackerchick.catima.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WearDataListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearProtocol.PATH_CARDS_RESPONSE -> {
                val json = String(messageEvent.data, Charsets.UTF_8)
                WearCardRepository.updateCards(json)
            }
            WearProtocol.PATH_CARD_DETAIL_RESPONSE -> {
                val json = String(messageEvent.data, Charsets.UTF_8)
                WearCardRepository.updateCardDetail(json)
            }
        }
    }
}
