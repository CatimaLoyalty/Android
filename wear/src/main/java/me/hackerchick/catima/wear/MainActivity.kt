package me.hackerchick.catima.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import me.hackerchick.catima.wear.ui.CardListScreen
import me.hackerchick.catima.wear.ui.CardViewScreen
import me.hackerchick.catima.wear.ui.theme.CatimaWearTheme
import protect.card_locker.shared.BluetoothPermissionHelper

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CatimaWear"
    }

    @Volatile private var fetchInFlight = false
    @Volatile private var protocolIncompatible = false

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestCardsFromPhone()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WearCardRepository.loadCache(this)

        setContent {
            CatimaWearTheme {
                val navController = rememberSwipeDismissableNavController()
                val cards by WearCardRepository.cards.collectAsState()
                val syncStatus by WearCardRepository.syncStatus.collectAsState()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "card_list"
                ) {
                    composable("card_list") {
                        CardListScreen(
                            cards = cards,
                            syncStatus = syncStatus,
                            onCardClick = { card ->
                                navController.navigate("card_view/${card.id}")
                            }
                        )
                    }
                    composable(
                        route = "card_view/{cardId}",
                        arguments = listOf(navArgument("cardId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val cardId = backStackEntry.arguments?.getInt("cardId") ?: return@composable
                        val card = cards?.find { it.id == cardId }
                        CardViewScreen(card = card)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        maybeRequestCards()
    }

    private fun maybeRequestCards() {
        if (fetchInFlight || protocolIncompatible) return
        BluetoothPermissionHelper.requestBluetoothConnectIfNeeded(
            this,
            btPermissionLauncher
        ) {
            requestCardsFromPhone()
        }
    }

    private fun requestCardsFromPhone() {
        fetchInFlight = true
        WearCardRepository.setSyncStatus(SyncStatus.SYNCING)
        BluetoothCardClient.fetchCards(this) { json, status ->
            fetchInFlight = false
            when (status) {
                SyncStatus.OK -> {
                    if (json != null) {
                        Log.d(TAG, "Got cards via Bluetooth")
                        WearCardRepository.updateCards(this, json)
                    } else {
                        WearCardRepository.setSyncStatus(SyncStatus.PHONE_NOT_REACHABLE)
                    }
                }
                SyncStatus.VERSION_INCOMPATIBLE -> {
                    Log.w(TAG, "Wear and phone protocol versions are incompatible")
                    protocolIncompatible = true
                    WearCardRepository.setSyncStatus(status)
                }
                else -> WearCardRepository.setSyncStatus(status)
            }
        }
    }

}
