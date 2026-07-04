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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.WearCard

@Composable
fun CardListScreen(
    cards: List<WearCard>?,
    syncing: Boolean,
    phoneNotReachable: Boolean,
    phoneOutdated: Boolean,
    watchOutdated: Boolean,
    onCardClick: (WearCard) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when {
            cards == null && phoneNotReachable -> {
                Text(
                    text = stringResource(R.string.phone_not_connected),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            cards == null && phoneOutdated -> {
                Text(
                    text = stringResource(R.string.phone_outdated),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            cards == null && watchOutdated -> {
                Text(
                    text = stringResource(R.string.watch_outdated),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            cards == null -> {
                CircularProgressIndicator()
            }
            cards.isEmpty() && !syncing -> {
                Text(
                    text = stringResource(R.string.no_cards),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(cards) { card ->
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    text = card.store,
                                    maxLines = 1,
                                )
                            },
                            onClick = { onCardClick(card) },
                            colors = if (card.headerColor != null) {
                                ChipDefaults.chipColors(backgroundColor = Color(card.headerColor))
                            } else {
                                ChipDefaults.primaryChipColors()
                            },
                        )
                    }
                    if (syncing) {
                        item {
                            Text(
                                text = stringResource(R.string.syncing),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    } else if (phoneNotReachable) {
                        item {
                            Text(
                                text = stringResource(R.string.sync_failed),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    } else if (phoneOutdated) {
                        item {
                            Text(
                                text = stringResource(R.string.phone_outdated),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    } else if (watchOutdated) {
                        item {
                            Text(
                                text = stringResource(R.string.watch_outdated),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
