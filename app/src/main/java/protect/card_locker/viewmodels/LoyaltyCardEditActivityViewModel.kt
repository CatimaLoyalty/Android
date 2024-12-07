package protect.card_locker.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import protect.card_locker.LoyaltyCard
import protect.card_locker.LoyaltyCardField
import protect.card_locker.async.TaskHandler

class LoyaltyCardEditActivityViewModel : ViewModel() {
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
}
