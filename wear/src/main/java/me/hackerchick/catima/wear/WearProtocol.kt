package me.hackerchick.catima.wear

import java.util.UUID

object WearProtocol {
    const val PATH_CARDS_REQUEST = "/catima/cards/request"
    const val PATH_CARDS_RESPONSE = "/catima/cards/response"
    const val PATH_CARD_DETAIL_REQUEST = "/catima/card/detail/request"
    const val PATH_CARD_DETAIL_RESPONSE = "/catima/card/detail/response"

    const val KEY_CARDS_JSON = "cards_json"
    const val KEY_CARD_ID = "card_id"
    const val KEY_CARD_STORE = "card_store"
    const val KEY_CARD_CARD_ID = "card_card_id"
    const val KEY_CARD_BARCODE_ID = "card_barcode_id"
    const val KEY_CARD_BARCODE_TYPE = "card_barcode_type"
    const val KEY_CARD_HEADER_COLOR = "card_header_color"

    val BT_SERVICE_UUID: UUID = UUID.fromString("e5b4f020-3a7e-4b6d-9f2c-1a8c5d3e7f90")
    const val BT_CMD_CARDS_REQUEST = "CARDS_REQUEST\n"
}
