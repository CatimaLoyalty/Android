package protect.card_locker.cardview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.dp
import protect.card_locker.CatimaComponentActivity
import protect.card_locker.ImageLocationType
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.compose.theme.CatimaTheme
import protect.card_locker.preferences.Settings

class LoyaltyCardImageActivity : CatimaComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fixedEdgeToEdge()
        applyWindowPreferences()

        val imageLocationType = intent.imageLocationType()
        val loyaltyCardId = intent.getIntExtra(BUNDLE_ID, 0)
        val bitmap = imageLocationType?.let {
            Utils.retrieveCardImage(this, loyaltyCardId, it)
        }

        if (loyaltyCardId == 0 || imageLocationType == null || bitmap == null) {
            Toast.makeText(this, R.string.failedToRetrieveImageFile, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        title = getString(imageLocationType.descriptionRes())

        setContent {
            CatimaTheme {
                LoyaltyCardImageScreen(
                    bitmap = bitmap,
                    contentDescription = stringResource(imageLocationType.descriptionRes()),
                    onBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }

    private fun applyWindowPreferences() {
        val settings = Settings(this)
        val currentWindow = window ?: return

        currentWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        val attributes = currentWindow.attributes
        if (settings.useMaxBrightnessDisplayingBarcode()) {
            attributes.screenBrightness = 1F
        }

        if (settings.keepScreenOn) {
            currentWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        currentWindow.attributes = attributes
    }

    private fun Intent.imageLocationType(): ImageLocationType? {
        val name = getStringExtra(BUNDLE_IMAGE_LOCATION_TYPE) ?: return null
        return runCatching { ImageLocationType.valueOf(name) }.getOrNull()
    }

    private fun ImageLocationType.descriptionRes(): Int {
        return when (this) {
            ImageLocationType.icon -> R.string.thumbnailDescription
            ImageLocationType.front -> R.string.frontImageDescription
            ImageLocationType.back -> R.string.backImageDescription
        }
    }

    companion object {
        const val BUNDLE_ID = "id"
        const val BUNDLE_IMAGE_LOCATION_TYPE = "imageLocationType"

        @JvmStatic
        fun createIntent(
            context: Context,
            loyaltyCardId: Int,
            imageLocationType: ImageLocationType
        ): Intent {
            return Intent(context, LoyaltyCardImageActivity::class.java)
                .putExtra(BUNDLE_ID, loyaltyCardId)
                .putExtra(BUNDLE_IMAGE_LOCATION_TYPE, imageLocationType.name)
        }
    }
}

@Composable
fun LoyaltyCardImageScreen(
    bitmap: Bitmap,
    contentDescription: String,
    onBack: () -> Unit,
) {
    var scale by remember { mutableStateOf(1F) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("card_image_zoom")
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
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

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
                .testTag("card_image_back")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back)
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
