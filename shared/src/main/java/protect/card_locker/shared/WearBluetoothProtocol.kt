package protect.card_locker.shared

import java.util.UUID

object WearBluetoothProtocol {
    val BT_SERVICE_UUID: UUID = UUID.fromString("e5b4f020-3a7e-4b6d-9f2c-1a8c5d3e7f90")
    const val BT_SERVICE_NAME = "CatimaWear"
    const val PROTOCOL_VERSION = 2
    const val BT_CMD_VERSIONS = "/VERSIONS"
    const val BT_CMD_AUTH = "/V${PROTOCOL_VERSION}/AUTH"
    const val BT_CMD_AUTH_RESET = "/V${PROTOCOL_VERSION}/AUTH_RESET"
    const val BT_CMD_CARDS_PAGE_PREFIX = "/V${PROTOCOL_VERSION}/CARDS_REQUEST_PAGE/"
    const val BT_RESPONSE_AUTH_REQUIRED = "AUTH_REQUIRED"
    const val BT_RESPONSE_AUTH_OK = "AUTH_OK"
    const val BT_RESPONSE_KEY_PREFIX = "KEY:"
    const val BT_RESPONSE_UNAUTHORIZED = "UNAUTHORIZED"
    const val PAGE_SIZE = 10
}
