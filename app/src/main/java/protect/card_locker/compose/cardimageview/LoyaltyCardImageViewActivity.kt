package protect.card_locker.compose.cardimageview

import android.graphics.Bitmap
import androidx.activity.OnBackPressedDispatcher
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import protect.card_locker.compose.CatimaTopAppBar

@Composable
fun LoyaltyCardImageViewScreen(
    bitmap: Bitmap,
    @StringRes contentDescriptionRes: Int,
    onBackPressedDispatcher: OnBackPressedDispatcher?,
) {
    var scale by remember { mutableFloatStateOf(1F) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val zoomState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1F, MAX_IMAGE_SCALE)
        scale = nextScale
        offset = if (nextScale == 1F) {
            Offset.Zero
        } else {
            (offset + panChange).clampTo(size, nextScale)
        }
    }

    Scaffold(
        topBar = {
            CatimaTopAppBar(
                title = stringResource(contentDescriptionRes),
                onBackPressedDispatcher = onBackPressedDispatcher
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .testTag("card_image_zoom")
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(contentDescriptionRes),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size = it }
                    .pointerInput(bitmap) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1F
                                offset = Offset.Zero
                            }
                        )
                    }
                    .transformable(zoomState)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
                    .testTag("card_image")
            )
        }
    }
}

private fun Offset.clampTo(size: IntSize, scale: Float): Offset {
    if (size.width == 0 || size.height == 0) {
        return this
    }

    val maxX = size.width * (scale - 1F) / 2F
    val maxY = size.height * (scale - 1F) / 2F
    return Offset(
        x = x.coerceIn(-maxX, maxX),
        y = y.coerceIn(-maxY, maxY)
    )
}

private const val MAX_IMAGE_SCALE = 5F
