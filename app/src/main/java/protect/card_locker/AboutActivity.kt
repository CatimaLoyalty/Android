package protect.card_locker

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.databinding.AboutActivityBinding

class AboutActivity : CatimaAppCompatActivity() {
    private var binding: AboutActivityBinding? = null
    private var content: AboutContent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = AboutActivityBinding.inflate(layoutInflater)
            .also { this.binding = it }
        val content = AboutContent(this)
            .also { this.content = it }

        title = content.pageTitle

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        enableToolbarBackButton()

        val copyright = binding.creditsSub
        copyright.text = content.copyright
        val versionHistory = binding.versionHistorySub
        versionHistory.text = content.versionHistory

        binding.versionHistory.tag = "https://catima.app/changelog/"
        binding.translate.tag = "https://hosted.weblate.org/engage/catima/"
        binding.license.tag = "https://github.com/CatimaLoyalty/Android/blob/master/LICENSE"
        binding.repo.tag = "https://github.com/CatimaLoyalty/Android/"
        binding.privacy.tag = "https://catima.app/privacy-policy/"
        binding.reportError.tag = "https://github.com/CatimaLoyalty/Android/issues"
        binding.rate.tag = "https://play.google.com/store/apps/details?id=me.hackerchick.catima"

        bindClickListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        content?.destroy()
        content = null
        clearClickListeners()
        binding = null
    }

    private fun bindClickListeners() {
        val binding = binding ?: return

        val openExternalBrowser = View.OnClickListener { view: View ->
            val tag = view.tag
            if (tag is String && tag.startsWith("https://")) {
                OpenWebLinkHandler().openBrowser(this, tag)
            }
        }

        binding.versionHistory.setOnClickListener(openExternalBrowser)
        binding.translate.setOnClickListener(openExternalBrowser)
        binding.license.setOnClickListener(openExternalBrowser)
        binding.repo.setOnClickListener(openExternalBrowser)
        binding.privacy.setOnClickListener(openExternalBrowser)
        binding.reportError.setOnClickListener(openExternalBrowser)
        binding.rate.setOnClickListener(openExternalBrowser)
        binding.credits.setOnClickListener { showCredits() }
    }

    private fun clearClickListeners() {
        val binding = binding ?: return
        binding.versionHistory.setOnClickListener(null)
        binding.translate.setOnClickListener(null)
        binding.license.setOnClickListener(null)
        binding.repo.setOnClickListener(null)
        binding.privacy.setOnClickListener(null)
        binding.reportError.setOnClickListener(null)
        binding.rate.setOnClickListener(null)
        binding.credits.setOnClickListener(null)
    }

    private fun showCredits() {
        val content = content ?: return
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