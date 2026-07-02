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
import com.google.android.gms.wearable.Wearable
import me.hackerchick.catima.wear.ui.CardListScreen
import me.hackerchick.catima.wear.ui.CardViewScreen
import me.hackerchick.catima.wear.ui.theme.CatimaWearTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "CatimaWear"
    }

    @Volatile private var fetchInFlight = false

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        requestCardsFromPhone()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatimaWearTheme {
                val navController = rememberSwipeDismissableNavController()
                val cards by WearCardRepository.cards.collectAsState()
                val phoneNotReachable by WearCardRepository.phoneNotReachable.collectAsState()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = "card_list"
                ) {
                    composable("card_list") {
                        CardListScreen(
                            cards = cards,
                            phoneNotReachable = phoneNotReachable,
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
        if (fetchInFlight) return
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
        WearCardRepository.reset()
        BluetoothCardClient.fetchCards(this) { json ->
            fetchInFlight = false
            if (json != null) {
                Log.d(TAG, "Got cards via Bluetooth")
                WearCardRepository.updateCards(json)
            } else {
                Log.d(TAG, "Bluetooth failed, trying GMS Wearable")
                requestCardsViaGms()
            }
        }
    }

    private fun requestCardsViaGms() {
        val messageClient = Wearable.getMessageClient(this)
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "GMS connected nodes: ${nodes.map { it.displayName }}")
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No GMS nodes found")
                    WearCardRepository.setPhoneNotReachable()
                } else {
                    nodes.forEach { node ->
                        messageClient.sendMessage(node.id, WearProtocol.PATH_CARDS_REQUEST, ByteArray(0))
                            .addOnSuccessListener { Log.d(TAG, "GMS request sent to ${node.displayName}") }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "GMS send failed to ${node.displayName}", e)
                                WearCardRepository.setPhoneNotReachable()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get GMS nodes", e)
                WearCardRepository.setPhoneNotReachable()
            }
    }
}
