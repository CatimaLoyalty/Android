package me.hackerchick.catima.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.SyncStatus
import me.hackerchick.catima.wear.WearCard
import protect.card_locker.shared.ForegroundColorHelper

@Composable
fun CardListScreen(
    cards: List<WearCard>?,
    syncStatus: SyncStatus,
    onCardClick: (WearCard) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cards == null && syncStatus == SyncStatus.PHONE_NOT_REACHABLE -> {
                Text(
                    text = stringResource(R.string.phone_not_connected),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            cards == null && syncStatus.labelRes != null -> {
                Text(
                    text = stringResource(syncStatus.labelRes),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            cards == null -> {
                CircularProgressIndicator()
            }
            cards.isEmpty() && syncStatus != SyncStatus.SYNCING -> {
                Text(
                    text = stringResource(R.string.no_cards),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> {
                val columnState = rememberTransformingLazyColumnState()
                ScreenScaffold(
                    scrollState = columnState,
                ) { contentPadding ->
                    val transformationSpec = rememberTransformationSpec()
                    TransformingLazyColumn(
                        state = columnState,
                        contentPadding = contentPadding,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(cards, key = { it.id }) { card ->
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                transformation = SurfaceTransformation(transformationSpec),
                                label = {
                                    Text(
                                        text = card.store,
                                        maxLines = 1,
                                    )
                                },
                                onClick = { onCardClick(card) },
                                colors = if (card.headerColor != null) {
                                    ButtonDefaults.buttonColors(
                                        containerColor = Color(card.headerColor),
                                        contentColor = if (ForegroundColorHelper.needsDarkForeground(
                                                card.headerColor
                                            )
                                        ) Color.Black else Color.White
                                    )
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                            )
                        }
                    }
                }
                val footerLabel = syncStatus.labelRes
                if (footerLabel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.75f))
                    ) {
                        Text(
                            text = stringResource(footerLabel),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            modifier = Modifier
                                .padding(40.dp, 4.dp, 40.dp, 20.dp)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
