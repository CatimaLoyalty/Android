package me.hackerchick.catima.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCardsFromPhone()

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

    private fun requestCardsFromPhone() {
        val messageClient = Wearable.getMessageClient(this)
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                Log.d(TAG, "Connected nodes: ${nodes.map { it.displayName }}")
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected nodes found - phone may not be paired or app not installed")
                    WearCardRepository.setPhoneNotReachable()
                } else {
                    nodes.forEach { node ->
                        Log.d(TAG, "Sending card request to ${node.displayName} (${node.id})")
                        messageClient.sendMessage(node.id, WearProtocol.PATH_CARDS_REQUEST, ByteArray(0))
                            .addOnSuccessListener { Log.d(TAG, "Request sent to ${node.displayName}") }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to send request to ${node.displayName}", e)
                                WearCardRepository.setPhoneNotReachable()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get connected nodes", e)
                WearCardRepository.setPhoneNotReachable()
            }
    }
}
