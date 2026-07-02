package me.hackerchick.catima.wear.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.WearCard

@Composable
fun CardViewScreen(card: WearCard?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (card == null) {
            androidx.wear.compose.material.CircularProgressIndicator()
        } else {
            CardDetail(card = card)
        }
    }
}

@Composable
private fun CardDetail(card: WearCard) {
    val barcodeValue = card.barcodeId ?: card.cardId
    val barcodeType = card.barcodeType

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val screenSize = minOf(maxWidth, maxHeight)
        val roundInset = screenSize * 0.15f
        val barcodeMaxSize = screenSize - roundInset * 2

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = card.store,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (barcodeType != null) {
                val barcodeBitmap = remember(barcodeValue, barcodeType) {
                    generateBarcode(barcodeValue, barcodeType)
                }

                if (barcodeBitmap != null) {
                    val format = runCatching { BarcodeFormat.valueOf(barcodeType) }.getOrNull()
                    val isSquare = format == BarcodeFormat.QR_CODE
                            || format == BarcodeFormat.AZTEC
                            || format == BarcodeFormat.DATA_MATRIX

                    Image(
                        bitmap = barcodeBitmap.asImageBitmap(),
                        contentDescription = card.cardId,
                        modifier = if (isSquare) {
                            Modifier.size(barcodeMaxSize)
                        } else {
                            Modifier.fillMaxWidth(0.85f).height(60.dp)
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_barcode),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_barcode),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = barcodeValue,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private fun generateBarcode(value: String, formatName: String): Bitmap? {
    return try {
        val format = BarcodeFormat.valueOf(formatName)
        val isSquare = format == BarcodeFormat.QR_CODE
                || format == BarcodeFormat.AZTEC
                || format == BarcodeFormat.DATA_MATRIX

        val width = if (isSquare) 300 else 600
        val height = if (isSquare) 300 else 150

        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val bitMatrix = MultiFormatWriter().encode(value, format, width, height, hints)

        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }

        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) {
        null
    }
}
