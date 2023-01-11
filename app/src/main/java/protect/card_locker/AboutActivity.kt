package protect.card_locker

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.activity.compose.setContent
import protect.card_locker.ui.About
import protect.card_locker.ui.CatimaTheme

class AboutActivity : CatimaAppCompatActivity() {
    private var content: AboutContent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatimaTheme {
                About(
                    navigateUp = { onBackPressedDispatcher.onBackPressed() }
                ) {
                    when (it) {
                        is UrlAboutEntry -> OpenWebLinkHandler().openBrowser(this, it.url)
                        is CreditsAboutEntry -> showCredits()
                    }
                }
            }
        }
        val content = AboutContent(this)
            .also { this.content = it }

        title = content.pageTitle
    }

    override fun onDestroy() {
        super.onDestroy()
        content?.destroy()
        content = null
    }

    private fun showCredits() {
        val content = content!!

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.credits)
            .setMessage(content.contributorInfo)
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    companion object {
        private const val TAG = "Catima"
    }
}