package protect.card_locker

import android.os.Bundle
import androidx.activity.compose.setContent
import protect.card_locker.compose.AboutScreenContent
import protect.card_locker.compose.theme.CatimaTheme

class AboutActivity : CatimaComponentActivity() {
    private lateinit var content: AboutContent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fixedEdgeToEdge()

        content = AboutContent(this)
        title = content.pageTitle

        setContent {
            CatimaTheme {
                AboutScreenContent(
                    content = content,
                    showDonate = BuildConfig.showDonate,
                    showRateOnGooglePlay = BuildConfig.showRateOnGooglePlay,
                    onBackPressedDispatcher = onBackPressedDispatcher
                )
            }
        }
    }
}
