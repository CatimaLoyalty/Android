package protect.card_locker

import android.os.Bundle
import android.text.Spanned
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.compose.CatimaAboutSection
import protect.card_locker.compose.CatimaTopAppBar
import protect.card_locker.compose.theme.CatimaTheme


class AboutActivity : ComponentActivity() {
    private companion object {
        private const val TAG = "Catima"
    }

    private lateinit var content: AboutContent

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = AboutContent(this)
        title = content.pageTitle

        setContent {
            CatimaTheme {
                Scaffold(
                    topBar = { CatimaTopAppBar(title.toString(), onBackPressedDispatcher) }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())) {
                        CatimaAboutSection(
                            stringResource(R.string.version_history),
                            content.versionHistory,
                            {
                                showHTML(
                                    stringResource(R.string.version_history),
                                    content.historyInfo,
                                    this as ComponentActivity,
                                    "https://catima.app/changelog/"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.credits),
                            content.copyrightShort,
                            {
                                showHTML(
                                    stringResource(R.string.credits),
                                    content.contributorInfo,
                                    this as ComponentActivity,
                                    null
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.help_translate_this_app),
                            stringResource(R.string.translate_platform),
                            {
                                OpenWebLinkHandler().openBrowser(
                                    this as ComponentActivity?,
                                    "https://hosted.weblate.org/engage/catima/"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.license),
                            stringResource(R.string.app_license),
                            {
                                showHTML(
                                    stringResource(R.string.license),
                                    content.licenseInfo,
                                    this as ComponentActivity,
                                    "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.source_repository),
                            stringResource(R.string.on_github),
                            {
                                OpenWebLinkHandler().openBrowser(
                                    this as ComponentActivity?,
                                    "https://github.com/CatimaLoyalty/Android/"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.privacy_policy),
                            stringResource(R.string.and_data_usage),
                            {
                                showHTML(
                                    stringResource(R.string.privacy_policy),
                                    content.privacyInfo,
                                    this as ComponentActivity,
                                    "https://catima.app/privacy-policy/"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.donate),
                            "",
                            {
                                OpenWebLinkHandler().openBrowser(
                                    this as ComponentActivity?,
                                    "https://catima.app/donate"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.rate_this_app),
                            stringResource(R.string.on_google_play),
                            {
                                OpenWebLinkHandler().openBrowser(
                                    this as ComponentActivity?,
                                    "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
                                )
                            }
                        )
                        CatimaAboutSection(
                            stringResource(R.string.report_error),
                            stringResource(R.string.on_github),
                            {
                                OpenWebLinkHandler().openBrowser(
                                    this as ComponentActivity?,
                                    "https://github.com/CatimaLoyalty/Android/issues"
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showHTML(title: String, text: Spanned, activity: ComponentActivity, url: String?) {
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

            // Add View online button if an URL is given
            url?.let {
                setNeutralButton(R.string.view_online) { _, _ -> OpenWebLinkHandler().openBrowser(activity, url) }
            }

            show()
        }
    }
}
