package me.hackerchick.catima.wear.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.WearCard

@Composable
fun CardViewScreen(card: WearCard?, onAboutClick: (WearCard) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (card == null) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = card.store,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )

                Spacer(modifier = Modifier.height(4.dp))

                CardDetail(
                    card = card,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onAboutClick(card) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    label = {
                        Text(
                            text = stringResource(R.string.about_card),
                            maxLines = 1,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOnAtMaxBrightness() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        val window = activity.window
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val params = window.attributes
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
        window.attributes = params
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            val restore = window.attributes
            restore.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            window.attributes = restore
        }
    }
}

@Composable
private fun CardDetail(card: WearCard, modifier: Modifier = Modifier) {
    val barcodeValue = card.barcodeId ?: card.cardId
    val barcodeType = card.barcodeType

    KeepScreenOnAtMaxBrightness()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (barcodeType != null) {
            val barcodeResult = remember(barcodeValue, barcodeType) {
                generateBarcode(barcodeValue, barcodeType)
            }

            if (barcodeResult != null) {
                val barcodeModifier = if (barcodeResult.isSquare) {
                    val barcodeSize = minOf(maxWidth * 0.68f, maxHeight)
                    Modifier.size(barcodeSize)
                } else {
                    Modifier
                        .width(maxWidth * 0.76f)
                        .height(minOf(60.dp, maxHeight))
                }

                Image(
                    bitmap = barcodeResult.bitmap.asImageBitmap(),
                    contentDescription = card.cardId,
                    modifier = barcodeModifier,
                )
            } else {
                Text(
                    text = stringResource(R.string.no_barcode),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.no_barcode),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 32.dp),
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
