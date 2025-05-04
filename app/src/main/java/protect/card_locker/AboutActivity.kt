package protect.card_locker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview

import protect.card_locker.compose.CatimaAboutSection
import protect.card_locker.compose.CatimaTopAppBar
import protect.card_locker.compose.theme.CatimaTheme


class AboutActivity : ComponentActivity() {
    private lateinit var content: AboutContent

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        content = AboutContent(this)
        title = content.pageTitle

        setContent {
            ScreenContent(
                showDonate = BuildConfig.showDonate,
                showRateOnGooglePlay = BuildConfig.showRateOnGooglePlay
            )
        }
    }

    @Composable
    fun ScreenContent(
        showDonate: Boolean,
        showRateOnGooglePlay: Boolean
    ) {
        CatimaTheme {
            Scaffold(
                topBar = { CatimaTopAppBar(title.toString(), onBackPressedDispatcher) }
            ) { innerPadding ->
                Column(
                    modifier = Modifier.padding(innerPadding).verticalScroll(rememberScrollState())
                ) {
                    CatimaAboutSection(
                        stringResource(R.string.version_history),
                        content.versionHistory,
                        onClickUrl = "https://catima.app/changelog/",
                        onClickDialogText = AnnotatedString.fromHtml(
                            htmlString = content.historyHtml,
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                    )
                    CatimaAboutSection(
                        stringResource(R.string.credits),
                        content.copyrightShort,
                        onClickDialogText = AnnotatedString.fromHtml(
                            htmlString = content.contributorInfoHtml,
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                    )
                    CatimaAboutSection(
                        stringResource(R.string.help_translate_this_app),
                        stringResource(R.string.translate_platform),
                        onClickUrl = "https://hosted.weblate.org/engage/catima/"
                    )
                    CatimaAboutSection(
                        stringResource(R.string.license),
                        stringResource(R.string.app_license),
                        onClickUrl = "https://github.com/CatimaLoyalty/Android/blob/main/LICENSE",
                        onClickDialogText = AnnotatedString.fromHtml(
                            htmlString = content.licenseHtml,
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                    )
                    CatimaAboutSection(
                        stringResource(R.string.source_repository),
                        stringResource(R.string.on_github),
                        onClickUrl = "https://github.com/CatimaLoyalty/Android/"
                    )
                    CatimaAboutSection(
                        stringResource(R.string.privacy_policy),
                        stringResource(R.string.and_data_usage),
                        onClickUrl = "https://catima.app/privacy-policy/",
                        onClickDialogText = AnnotatedString.fromHtml(
                            htmlString = content.privacyHtml,
                            linkStyles = TextLinkStyles(
                                style = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        )
                    )
                    if (showDonate) {
                        CatimaAboutSection(
                            stringResource(R.string.donate),
                            "",
                            onClickUrl = "https://catima.app/donate"
                        )
                    }
                    if (showRateOnGooglePlay) {
                        CatimaAboutSection(
                            stringResource(R.string.rate_this_app),
                            stringResource(R.string.on_google_play),
                            onClickUrl = "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
                        )
                    }
                    CatimaAboutSection(
                        stringResource(R.string.report_error),
                        stringResource(R.string.on_github),
                        onClickUrl = "https://github.com/CatimaLoyalty/Android/issues"
                    )
                }
            }
        }
    }

    @Preview
    @Composable
    fun AboutActivityPreview() {
        ScreenContent(
            showDonate = true,
            showRateOnGooglePlay = true
        )
    }
}
