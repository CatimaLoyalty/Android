package protect.card_locker.cardimageview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.core.content.FileProvider
import protect.card_locker.BuildConfig
import protect.card_locker.CatimaComponentActivity
import protect.card_locker.ImageLocationType
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.compose.CatimaTopAppBar
import protect.card_locker.compose.theme.CatimaTheme
import protect.card_locker.preferences.Settings

class LoyaltyCardImageViewActivity : CatimaComponentActivity() {
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
                LoyaltyCardImageViewScreen(
                    bitmap = bitmap,
                    contentDescriptionRes = imageLocationType.descriptionRes(),
                    onBackPressedDispatcher = onBackPressedDispatcher,
                    onOpenInGallery = { openInGallery(loyaltyCardId, imageLocationType) }
                )
            }
        }
    }

    private fun openInGallery(loyaltyCardId: Int, imageLocationType: ImageLocationType) {
        val imageFile = Utils.retrieveCardImageAsFile(this, loyaltyCardId, imageLocationType)
        val imageUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, imageFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(imageUri, IMAGE_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.failedToOpenGallery, Toast.LENGTH_LONG).show()
        }
    }

    private fun applyWindowPreferences() {
        val settings = Settings(this)
        val currentWindow = window ?: return

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
            return Intent(context, LoyaltyCardImageViewActivity::class.java)
                .putExtra(BUNDLE_ID, loyaltyCardId)
                .putExtra(BUNDLE_IMAGE_LOCATION_TYPE, imageLocationType.name)
        }
    }
}

@Composable
fun LoyaltyCardImageViewScreen(
    bitmap: Bitmap,
    @StringRes contentDescriptionRes: Int,
    onBackPressedDispatcher: OnBackPressedDispatcher,
    onOpenInGallery: () -> Unit,
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
                onBackPressedDispatcher = onBackPressedDispatcher,
                actions = {
                    var menuExpanded by remember { mutableStateOf(false) }

                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.testTag("card_image_overflow_menu")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.overflowMenu)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.openInGallery)) },
                            onClick = {
                                menuExpanded = false
                                onOpenInGallery()
                            },
                            modifier = Modifier.testTag("card_image_open_in_gallery")
                        )
                    }
                }
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
private const val IMAGE_MIME_TYPE = "image/png"