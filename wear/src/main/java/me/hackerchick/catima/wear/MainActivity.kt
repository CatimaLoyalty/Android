package me.hackerchick.catima.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CatimaWear"
    }

    @Volatile private var fetchInFlight = false
    private var protocolIncompatible = false

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requestCardsFromPhone()
        }
    }

    private fun requestCardsFromPhone() {
        fetchInFlight = true
        WearCardRepository.setSyncing(true)
        BluetoothCardClient.fetchCards(this) { json, status ->
            fetchInFlight = false
            when (status) {
                BluetoothCardClient.FetchStatus.SUCCESS -> {
                    if (json != null) {
                        Log.d(TAG, "Got cards via Bluetooth")
                        WearCardRepository.updateCards(this, json)
                    } else {
                        WearCardRepository.setPhoneNotReachable()
                    }
                }
                BluetoothCardClient.FetchStatus.PHONE_OUTDATED -> {
                    Log.w(TAG, "Phone app is outdated")
                    WearCardRepository.setPhoneOutdated()
                }
                BluetoothCardClient.FetchStatus.WATCH_OUTDATED -> {
                    Log.w(TAG, "Wear app is outdated")
                    protocolIncompatible = true
                    WearCardRepository.setWatchOutdated()
                }
                BluetoothCardClient.FetchStatus.NO_DEVICE -> {
                    Log.w(TAG, "Bluetooth failed, phone not reachable")
                    WearCardRepository.setPhoneNotReachable()
                }
                BluetoothCardClient.FetchStatus.PERMISSION_DENIED -> {
                    Log.w(TAG, "Bluetooth permission denied")
                    WearCardRepository.setPermissionDenied()
                }
                BluetoothCardClient.FetchStatus.BLUETOOTH_DISABLED -> {
                    Log.w(TAG, "Bluetooth is disabled")
                    WearCardRepository.setBluetoothDisabled()
                }
            }
        }
    }

}
