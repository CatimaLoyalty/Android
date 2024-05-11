package protect.card_locker.ui.screens.about

import android.content.DialogInterface
import android.os.Bundle
import android.text.Spanned
import android.view.View
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.BuildConfig
import protect.card_locker.CatimaAppCompatActivity
import protect.card_locker.R
import protect.card_locker.Utils
import protect.card_locker.databinding.AboutActivityBinding
import protect.card_locker.ui.extensions.load
import java.io.IOException

class AboutActivity : CatimaAppCompatActivity() {

    companion object {
        private const val CHANGELOG_URL = "https://catima.app/changelog/"
        private const val DONATION_URL = "https://catima.app/donate"
        private const val LICENSE_URL = "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE"
        private const val PRIVACY_POLICY_URL = "https://catima.app/privacy-policy/"
        private const val REPO_URL = "https://github.com/CatimaLoyalty/Android/"
        private const val SUPPORT_URL = "https://github.com/CatimaLoyalty/Android/issues"
        private const val TRANSLATION_URL = "https://hosted.weblate.org/engage/catima/"
        private const val GOOGLE_PLAY_URL =
            "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
    }

    private lateinit var binding: AboutActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AboutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val installedFromGooglePlay = Utils.installedFromGooglePlay(this)

        // Toolbar
        binding.toolbar.apply {
            title = getString(R.string.about_title_fmt, getString(R.string.app_name))
            setNavigationOnClickListener { finish() }
        }

        binding.translate.setOnClickListener { load(TRANSLATION_URL) }
        binding.repo.setOnClickListener { load(REPO_URL) }
        binding.reportError.setOnClickListener { load(SUPPORT_URL) }

        // Version history
        binding.versionHistorySub.text =
            getString(R.string.debug_version_fmt, BuildConfig.VERSION_NAME)
        binding.versionHistory.setOnClickListener {
            showHTML(
                R.string.version_history,
                HtmlCompat.fromHtml(getHistory(), HtmlCompat.FROM_HTML_MODE_COMPACT),
                CHANGELOG_URL
            )
        }

        // Credits
        binding.creditsSub.text = getString(R.string.app_copyright_short)
        binding.credits.setOnClickListener {
            showHTML(
                R.string.credits,
                HtmlCompat.fromHtml(
                    AboutContent.getContributorInfo(this),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            )
        }

        // License
        binding.license.setOnClickListener {
            showHTML(
                R.string.license,
                HtmlCompat.fromHtml(
                    Utils.readTextFile(this, R.raw.license),
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                ),
                LICENSE_URL
            )
        }

        // Privacy policy
        binding.privacy.setOnClickListener {
            showHTML(
                R.string.privacy_policy,
                HtmlCompat.fromHtml(getPrivacyInfo(), HtmlCompat.FROM_HTML_MODE_COMPACT),
                PRIVACY_POLICY_URL
            )
        }

        // Hide Google Play rate button if not on Google Play
        binding.rate.apply {
            visibility = if (installedFromGooglePlay) View.VISIBLE else View.GONE
            setOnClickListener { load(GOOGLE_PLAY_URL) }
        }

        // Hide donate button on Google Play (Google Play doesn't allow donation links)
        binding.donate.apply {
            visibility = if (installedFromGooglePlay) View.GONE else View.VISIBLE
            setOnClickListener { load(DONATION_URL) }
        }
    }

    private fun showHTML(@StringRes title: Int, text: Spanned, url: String? = null) {
        // Create dialog
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.ok, null)

        // Add View online button if an URL is linked to this view
        if (!url.isNullOrBlank()) {
            dialog.setNeutralButton(R.string.view_online) { _: DialogInterface?, _: Int ->
                load(url)
            }
        }

        // Show dialog
        dialog.show()
    }

    private fun getPrivacyInfo(): String {
        return try {
            val privacyPolicy = Utils.readTextFile(this, R.raw.privacy)
                .replace("# Privacy Policy\n", "")
            Utils.linkify(Utils.basicMDToHTML(privacyPolicy)).replace("\n", "<br />")
        } catch (ignored: IOException) {
            return String()
        }
    }

    private fun getHistory(): String {
        return try {
            val versionHistory = Utils.readTextFile(this, R.raw.changelog)
                .replace("# Changelog\n\n", "")
            Utils.linkify(Utils.basicMDToHTML(versionHistory))
                .replace("\n", "<br />")
        } catch (ignored: IOException) {
            String()
        }
    }
}
