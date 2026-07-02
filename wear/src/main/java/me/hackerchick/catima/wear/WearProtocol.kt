package me.hackerchick.catima.wear

import java.util.UUID

object WearProtocol {
    val BT_SERVICE_UUID: UUID = UUID.fromString("e5b4f020-3a7e-4b6d-9f2c-1a8c5d3e7f90")
    const val BT_CMD_CARDS_REQUEST = "CARDS_REQUEST\n"
}
