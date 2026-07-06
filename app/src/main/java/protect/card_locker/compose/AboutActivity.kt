package protect.card_locker.compose

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import protect.card_locker.AboutContent
import protect.card_locker.OpenWebLinkHandler
import protect.card_locker.R

@Composable
fun AboutScreenContent(
    content: AboutContent,
    showDonate: Boolean = true,
    showRateOnGooglePlay: Boolean = false,
    onBackPressedDispatcher: OnBackPressedDispatcher? = null,
) {
    Scaffold(
        topBar = { CatimaTopAppBar(content.pageTitle.toString(), onBackPressedDispatcher) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            CatimaAboutSection(
                stringResource(R.string.version_history),
                content.versionHistory,
                modifier = Modifier.testTag("card_version_history"),
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
                modifier = Modifier.testTag("card_credits"),
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
                modifier = Modifier.testTag("card_translate"),
                onClickUrl = "https://hosted.weblate.org/engage/catima/"
            )
            CatimaAboutSection(
                stringResource(R.string.license),
                stringResource(R.string.app_license),
                modifier = Modifier.testTag("card_license"),
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
                modifier = Modifier.testTag("card_source_github"),
                onClickUrl = "https://github.com/CatimaLoyalty/Android/"
            )
            CatimaAboutSection(
                stringResource(R.string.privacy_policy),
                stringResource(R.string.and_data_usage),
                modifier = Modifier.testTag("card_privacy_policy"),
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
                    modifier = Modifier.testTag("card_donate"),
                    onClickUrl = "https://catima.app/donate"
                )
            }
            if (showRateOnGooglePlay) {
                CatimaAboutSection(
                    stringResource(R.string.rate_this_app),
                    stringResource(R.string.on_google_play),
                    modifier = Modifier.testTag("card_rate_google"),
                    onClickUrl = "https://play.google.com/store/apps/details?id=me.hackerchick.catima"
                )
            }
            CatimaAboutSection(
                stringResource(R.string.report_error),
                stringResource(R.string.on_github),
                modifier = Modifier.testTag("card_report_error"),
                onClickUrl = "https://github.com/CatimaLoyalty/Android/issues"
            )
        }
    }
}

@Composable
fun CatimaAboutSection(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onClickUrl: String? = null,
    onClickDialogText: AnnotatedString? = null,
) {
    val activity = LocalActivity.current

    val openDialog = remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                if (onClickDialogText != null) {
                    openDialog.value = true
                } else if (onClickUrl != null) {
                    OpenWebLinkHandler().openBrowser(activity, onClickUrl)
                }
            }
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = message)
        }
        Text(modifier = Modifier.align(Alignment.CenterVertically).semantics() { hideFromAccessibility() },
            text = ">",
            style = MaterialTheme.typography.bodyMedium
        )
    }
    if (openDialog.value && onClickDialogText != null) {
        AlertDialog(
            icon = {},
            title = {
                Text(text = title)
            },
            text = {
                Text(
                    text = onClickDialogText,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            onDismissRequest = {
                openDialog.value = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                if (onClickUrl != null) {
                    TextButton(
                        onClick = {
                            OpenWebLinkHandler().openBrowser(activity, onClickUrl)
                        }
                    ) {
                        Text(stringResource(R.string.view_online))
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun AboutActivityPreview() {
    AboutScreenContent(AboutContent(LocalContext.current))
}
