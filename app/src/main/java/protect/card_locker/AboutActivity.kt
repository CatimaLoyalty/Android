package protect.card_locker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import protect.card_locker.compose.AboutScreenRoot

object AppURLs {
    const val VERSION_HISTORY = "https://catima.app/changelog"
    const val HELP_TRANSLATE_APP = "https://hosted.weblate.org/engage/catima"
    const val LICENSE = "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE"
    const val REPOSITORY_SOURCE = "https://github.com/CatimaLoyalty/Android"
    const val PRIVACY_POLICY = "https://catima.app/privacy-policy"
    const val DONATE = "https://catima.app/donate"
    const val RATE_THE_APP = "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
    const val REPORT_ERROR = "https://github.com/CatimaLoyalty/Android/issues"
}

class AboutActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenTitle = AboutContent.getPageTitle(this)
        title = screenTitle

        setContent {
            AboutScreenRoot(
                title = screenTitle,
                showDonate = !BuildConfig.DEBUG, // show donate button only in release builds
                showRateOnGooglePlay = !BuildConfig.DEBUG, // show rate button only in release builds
                onBackPressedDispatcher = onBackPressedDispatcher,
            )
        }
    }
}


