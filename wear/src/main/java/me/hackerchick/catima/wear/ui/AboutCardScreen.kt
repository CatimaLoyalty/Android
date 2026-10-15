package me.hackerchick.catima.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.WearCard
import java.text.DateFormat
import java.util.Date

@Composable
fun AboutCardScreen(card: WearCard?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (card == null) {
            CircularProgressIndicator()
        } else {
            AboutCardDetail(card = card)
        }
    }
}

@Composable
private fun AboutCardDetail(card: WearCard) {
    val columnState = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = columnState,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    text = card.store,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                )
            }

            item {
                AboutCardRow(
                    label = stringResource(R.string.card_id_label),
                    value = card.cardId,
                )
            }

            val barcodeId = card.barcodeId
            if (barcodeId != null && barcodeId != card.cardId) {
                item {
                    AboutCardRow(
                        label = stringResource(R.string.barcode_value_label),
                        value = barcodeId,
                    )
                }
            }

            val barcodeType = card.barcodeType
            if (barcodeType != null) {
                item {
                    AboutCardRow(
                        label = stringResource(R.string.barcode_type_label),
                        value = barcodeType,
                    )
                }
            }

            val note = card.note
            if (note != null) {
                item {
                    AboutCardRow(
                        label = stringResource(R.string.note_label),
                        value = note,
                    )
                }
            }

            val expiry = card.expiry
            if (expiry != null) {
                item {
                    AboutCardRow(
                        label = stringResource(R.string.expiry_label),
                        value = DateFormat.getDateInstance().format(Date(expiry)),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutCardRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 14.sp,
        )
    }
}
