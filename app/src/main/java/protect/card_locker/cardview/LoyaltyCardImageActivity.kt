package protect.card_locker.cardview

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import protect.card_locker.CatimaComponentActivity
import protect.card_locker.ImageLocationType
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.compose.LoyaltyCardImageScreen
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
                    contentDescriptionRes = imageLocationType.descriptionRes(),
                    onBackPressedDispatcher = onBackPressedDispatcher
                )
            }
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
            return Intent(context, LoyaltyCardImageActivity::class.java)
                .putExtra(BUNDLE_ID, loyaltyCardId)
                .putExtra(BUNDLE_IMAGE_LOCATION_TYPE, imageLocationType.name)
        }
    }
}
