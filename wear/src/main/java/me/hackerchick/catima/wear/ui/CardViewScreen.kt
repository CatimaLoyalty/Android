package me.hackerchick.catima.wear.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
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
            CircularProgressIndicator()
        } else {
            CardDetail(card = card)
        }
    }
}

@Composable
private fun KeepScreenOnAtMaxBrightness() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val originalBrightness = window.attributes.screenBrightness
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = params
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val restore = window.attributes
            restore.screenBrightness = originalBrightness
            window.attributes = restore
        }
    }
}

@Composable
private fun CardDetail(card: WearCard) {
    val barcodeValue = card.barcodeId ?: card.cardId
    val barcodeType = card.barcodeType

    KeepScreenOnAtMaxBrightness()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.White)
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
                color = androidx.compose.ui.graphics.Color.Black,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (barcodeType != null) {
                val barcodeResult = remember(barcodeValue, barcodeType) {
                    generateBarcode(barcodeValue, barcodeType)
                }

                if (barcodeResult != null) {
                    Image(
                        bitmap = barcodeResult.bitmap.asImageBitmap(),
                        contentDescription = card.cardId,
                        modifier = if (barcodeResult.isSquare) {
                            Modifier.size(barcodeMaxSize)
                        } else {
                            Modifier.fillMaxWidth(0.85f).height(60.dp)
                        },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_barcode),
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.Black,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_barcode),
                    textAlign = TextAlign.Center,
                    color = androidx.compose.ui.graphics.Color.Black,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = barcodeValue,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = androidx.compose.ui.graphics.Color.Black,
            )
        }
    }
}

private data class BarcodeResult(val bitmap: Bitmap, val isSquare: Boolean)

private fun isBarcodeSquare(format: BarcodeFormat): Boolean =
    format == BarcodeFormat.QR_CODE
            || format == BarcodeFormat.AZTEC
            || format == BarcodeFormat.DATA_MATRIX

private fun generateBarcode(value: String, formatName: String): BarcodeResult? {
    return try {
        val format = BarcodeFormat.valueOf(formatName)
        val isSquare = isBarcodeSquare(format)

        val width = if (isSquare) 300 else 600
        val height = if (isSquare) 300 else 150

        val hints = mapOf(EncodeHintType.MARGIN to 0)
        val bitMatrix = MultiFormatWriter().encode(value, format, width, height, hints)

        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }

        BarcodeResult(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888), isSquare)
    } catch (_: Exception) {
        null
    }
}
