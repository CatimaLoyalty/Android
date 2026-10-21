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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScreenScaffoldDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import me.hackerchick.catima.wear.R
import me.hackerchick.catima.wear.WearCard
import java.text.DateFormat
import java.util.Date

@Composable
fun CardViewScreen(card: WearCard?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
private fun CardDetail(card: WearCard) {
    KeepScreenOnAtMaxBrightness()

    val columnState = rememberTransformingLazyColumnState()
    val defaultContentPadding = ScreenScaffoldDefaults.contentPadding
    val metadataPadding =
        defaultContentPadding.calculateLeftPadding(LocalLayoutDirection.current) * 2

    // Size the barcode page to exactly fill the viewport, so the barcode is
    // shown fullscreen at full brightness and the card details below are only
    // revealed by scrolling down, like Google Pay on Wear OS
    val viewportHeight = columnState.layoutInfo.viewportSize.height
    val barcodePageHeight = if (viewportHeight > 0) {
        with(LocalDensity.current) { viewportHeight.toDp() }
    } else {
        LocalConfiguration.current.screenHeightDp.dp
    }

    ScreenScaffold(
        scrollState = columnState,
        contentPadding = PaddingValues(0.dp),
    ) {
        val transformationSpec = rememberTransformationSpec()
        TransformingLazyColumn(
            state = columnState,
            contentPadding = PaddingValues(bottom = barcodePageHeight * 0.4f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                BarcodePage(
                    card = card,
                    modifier = Modifier.height(barcodePageHeight),
                )
            }

            item {
                Text(
                    text = card.store,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContentTransformation(scrollProgress)
                            }
                        }
                        .padding(horizontal = metadataPadding)
                        .padding(bottom = 4.dp),
                )
            }

            item {
                CardDetailRow(
                    label = stringResource(R.string.card_id_label),
                    value = card.cardId,
                    modifier = Modifier
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContentTransformation(scrollProgress)
                            }
                        }
                        .padding(horizontal = metadataPadding),
                )
            }

            val barcodeId = card.barcodeId
            if (barcodeId != null && barcodeId != card.cardId) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.barcode_value_label),
                        value = barcodeId,
                        modifier = Modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }

            val barcodeType = card.barcodeType
            if (barcodeType != null) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.barcode_type_label),
                        value = barcodeType,
                        modifier = Modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }

            val note = card.note
            if (note != null) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.note_label),
                        value = note,
                        modifier = Modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }

            val validFrom = card.validFrom
            if (validFrom != null) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.valid_from_label),
                        value = formatDate(validFrom),
                        modifier = Modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }

            val validFrom = card.validFrom
            if (validFrom != null) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.valid_from_label),
                        value = DateFormat.getDateInstance().format(Date(validFrom)),
                        modifier = Modifier
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }

            val expiry = card.expiry
            if (expiry != null) {
                item {
                    CardDetailRow(
                        label = stringResource(R.string.expiry_label),
                        value = formatDate(expiry),
                        modifier = Modifier
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContentTransformation(scrollProgress)
                                }
                            }
                            .padding(horizontal = metadataPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun BarcodePage(card: WearCard, modifier: Modifier = Modifier) {
    val barcodeValue = card.barcodeId ?: card.cardId
    val barcodeType = card.barcodeType

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(ComposeColor.White)
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
                color = ComposeColor.Black,
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
                        color = ComposeColor.Black,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.no_barcode),
                    textAlign = TextAlign.Center,
                    color = ComposeColor.Black,
                )
            }
        }
    }
}

@Composable
private fun CardDetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 16.sp,
        )
    }
}

private fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance().format(Date(epochMillis))

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
