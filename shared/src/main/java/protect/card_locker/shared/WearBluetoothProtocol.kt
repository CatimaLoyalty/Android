package protect.card_locker.shared

import java.util.UUID

object WearBluetoothProtocol {
    val BT_SERVICE_UUID: UUID = UUID.fromString("e5b4f020-3a7e-4b6d-9f2c-1a8c5d3e7f90")
    const val BT_SERVICE_NAME = "CatimaWear"
    const val PROTOCOL_VERSION = 1
    const val PROTOCOL_MINOR_VERSION = 0
    const val BT_CMD_VERSIONS = "/VERSIONS"
    const val BT_CMD_TOKEN_PREFIX = "TOKEN:"
    const val BT_CMD_VERSION_PREFIX = "/V$PROTOCOL_VERSION/"
    const val BT_CMD_CARDS_PAGE_PREFIX = "${BT_CMD_VERSION_PREFIX}CARDS_REQUEST_PAGE/"
    const val BT_RESPONSE_NOT_AUTHORIZED = "NOT_AUTHORIZED"
    const val PAGE_SIZE = 10
}
