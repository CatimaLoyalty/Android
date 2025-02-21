package protect.card_locker

import android.os.Bundle
import android.text.Spanned
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView

import androidx.annotation.StringRes

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import protect.card_locker.databinding.AboutActivityBinding

class AboutActivity : CatimaAppCompatActivity() {
    private companion object {
        private const val TAG = "Catima"
    }

    private var _binding: AboutActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var content: AboutContent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = AboutActivityBinding.inflate(layoutInflater)
        content = AboutContent(this)
        title = content.pageTitle
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        enableToolbarBackButton()

        binding.apply {
            creditsSub.text = content.copyrightShort
            versionHistorySub.text = content.versionHistory

            versionHistory.tag = "https://catima.app/changelog/"
            translate.tag = "https://hosted.weblate.org/engage/catima/"
            license.tag = "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE"
            repo.tag = "https://github.com/CatimaLoyalty/Android/"
            privacy.tag = "https://catima.app/privacy-policy/"
            reportError.tag = "https://github.com/CatimaLoyalty/Android/issues"
            rate.tag = "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
            donate.tag = "https://catima.app/donate"

            // Hide Google Play rate button if not on Google Play
            rate.visibility = if (BuildConfig.showRateOnGooglePlay) View.VISIBLE else View.GONE
            // Hide donate button on Google Play (Google Play doesn't allow donation links)
            donate.visibility = if (BuildConfig.showDonate) View.VISIBLE else View.GONE
        }

        bindClickListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        content.destroy()
        clearClickListeners()
        _binding = null
    }

    private fun bindClickListeners() {
        binding.apply {
            versionHistory.setOnClickListener { showHistory(it) }
            translate.setOnClickListener { openExternalBrowser(it) }
            license.setOnClickListener { showLicense(it) }
            repo.setOnClickListener { openExternalBrowser(it) }
            privacy.setOnClickListener { showPrivacy(it) }
            reportError.setOnClickListener { openExternalBrowser(it) }
            rate.setOnClickListener { openExternalBrowser(it) }
            donate.setOnClickListener { openExternalBrowser(it) }
            credits.setOnClickListener { showCredits() }
        }
    }

    private fun clearClickListeners() {
        binding.apply {
            versionHistory.setOnClickListener(null)
            translate.setOnClickListener(null)
            license.setOnClickListener(null)
            repo.setOnClickListener(null)
            privacy.setOnClickListener(null)
            reportError.setOnClickListener(null)
            rate.setOnClickListener(null)
            donate.setOnClickListener(null)
            credits.setOnClickListener(null)
        }
    }

    private fun showCredits() {
        showHTML(R.string.credits, content.contributorInfo, null)
    }

    private fun showHistory(view: View) {
        showHTML(R.string.version_history, content.historyInfo, view)
    }

    private fun showLicense(view: View) {
        showHTML(R.string.license, content.licenseInfo, view)
    }

    private fun showPrivacy(view: View) {
        showHTML(R.string.privacy_policy, content.privacyInfo, view)
    }

    private fun showHTML(@StringRes title: Int, text: Spanned, view: View?) {
        val dialogContentPadding = resources.getDimensionPixelSize(R.dimen.alert_dialog_content_padding)
        val textView = TextView(this).apply {
            setText(text)
            Utils.makeTextViewLinksClickable(this, text)
        }

        val scrollView = ScrollView(this).apply {
            addView(textView)
            setPadding(dialogContentPadding, dialogContentPadding / 2, dialogContentPadding, 0)
        }

        MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setView(scrollView)
            setPositiveButton(R.string.ok, null)

            // Add View online button if an URL is linked to this view
            view?.tag?.let {
                setNeutralButton(R.string.view_online) { _, _ -> openExternalBrowser(view) }
            }

            show()
        }
    }

    private fun openExternalBrowser(view: View) {
        val tag = view.tag
        if (tag is String && tag.startsWith("https://")) {
            OpenWebLinkHandler().openBrowser(this, tag)
        }
    }
}