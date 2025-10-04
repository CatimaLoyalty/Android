package protect.card_locker.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import protect.card_locker.LoyaltyCard
import protect.card_locker.LoyaltyCardField
import protect.card_locker.async.TaskHandler

data class LoyaltyCardEditState(
    val loyaltyCardDateType: LoyaltyCardDateType? = null,
)
class LoyaltyCardEditActivityViewModel : ViewModel() {

    private val _state = MutableStateFlow(LoyaltyCardEditState())
    val state : StateFlow<LoyaltyCardEditState> = _state.asStateFlow()

    var initialized: Boolean = false
    var hasChanged: Boolean = false

    var taskHandler: TaskHandler = TaskHandler();

    var addGroup: String? = null
    var openSetIconMenu: Boolean = false
    var loyaltyCardId: Int = 0
    var updateLoyaltyCard: Boolean = false
    var duplicateFromLoyaltyCardId: Boolean = false
    var importLoyaltyCardUri: Uri? = null

    var tabIndex: Int = 0
    var requestedImageType: Int = 0
    var tempLoyaltyCardField: LoyaltyCardField? = null

    var loyaltyCard: LoyaltyCard = LoyaltyCard()

    fun onAction(action: LoyaltyCardEditUiAction) {
        when (action) {
            is LoyaltyCardEditUiAction.SetLoyaltyCardDateType -> {
                _state.value = _state.value.copy(loyaltyCardDateType = action.loyaltyCardDateType)
            }
        }
    }
}

enum class LoyaltyCardDateType {
    VALID_FROM, EXPIRY
}

sealed interface LoyaltyCardEditUiAction {
    data class SetLoyaltyCardDateType(val loyaltyCardDateType: LoyaltyCardDateType) : LoyaltyCardEditUiAction
}