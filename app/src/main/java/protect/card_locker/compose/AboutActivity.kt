package protect.card_locker.compose

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import protect.card_locker.AboutContent
import protect.card_locker.AppURLs
import protect.card_locker.OpenWebLinkHandler
import protect.card_locker.R
import protect.card_locker.compose.theme.CatimaTheme


@Composable
fun AboutScreenRoot(
    title: String,
    showDonate: Boolean,
    showRateOnGooglePlay: Boolean,
    onBackPressedDispatcher: OnBackPressedDispatcher? = null
) {
    CatimaTheme {
        Scaffold(
            topBar = {
                CatimaTopAppBar(title, onBackPressedDispatcher)
            }
        ) { innerPadding ->
            AboutScreenContent(
                modifier = Modifier.padding(innerPadding),
                showDonate = showDonate,
                showRateOnGooglePlay = showRateOnGooglePlay,
            )
        }
    }
}


@Composable
fun AboutScreenContent(
    modifier: Modifier = Modifier,
    showDonate: Boolean,
    showRateOnGooglePlay: Boolean,
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        CatimaAboutSection(
            title = stringResource(R.string.version_history),
            message = if (activity == null) "" else AboutContent.getVersionHistory(activity),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.VERSION_HISTORY)
                }
            },
            onClickDialogText = AnnotatedString.fromHtml(
                htmlString = AboutContent.getHistoryHtml(context),
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )
        )
        HorizontalDivider()
        CatimaAboutSection(
            title = stringResource(R.string.credits),
            message = AboutContent.getCopyrightShort(context),
            onClickDialogText = AnnotatedString.fromHtml(
                htmlString = AboutContent.getContributorInfoHtml(context),
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )
        )
        HorizontalDivider()
        CatimaAboutSection(
            title = stringResource(R.string.help_translate_this_app),
            message = stringResource(R.string.translate_platform),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.HELP_TRANSLATE_APP)
                }
            }
        )
        HorizontalDivider()
        CatimaAboutSection(
            title = stringResource(R.string.license),
            message = stringResource(R.string.app_license),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.LICENSE)
                }
            },
            onClickDialogText = AnnotatedString.fromHtml(
                htmlString = AboutContent.getLicenseHtml(context),
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )
        )
        HorizontalDivider()
        CatimaAboutSection(
            title = stringResource(R.string.source_repository),
            message = stringResource(R.string.on_github),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.REPOSITORY_SOURCE)
                }
            },
        )
        HorizontalDivider()
        CatimaAboutSection(
            title = stringResource(R.string.privacy_policy),
            message = stringResource(R.string.and_data_usage),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.PRIVACY_POLICY)
                }
            },
            onClickDialogText = AnnotatedString.fromHtml(
                htmlString = AboutContent.getPrivacyHtml(context),
                linkStyles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            ),
        )
        HorizontalDivider()
        if (showDonate) {
            CatimaAboutSection(
                title = stringResource(R.string.donate),
                openURL = {
                    activity?.let {
                        OpenWebLinkHandler.openURL(it, AppURLs.DONATE)
                    }
                },
            )
            HorizontalDivider()
        }
        if (showRateOnGooglePlay) {
            CatimaAboutSection(
                title = stringResource(R.string.rate_this_app),
                message = stringResource(R.string.on_google_play),
                openURL = {
                    activity?.let {
                        OpenWebLinkHandler.openURL(it, AppURLs.RATE_THE_APP)
                    }
                },
            )
            HorizontalDivider()
        }
        CatimaAboutSection(
            title = stringResource(R.string.report_error),
            message = stringResource(R.string.on_github),
            openURL = {
                activity?.let {
                    OpenWebLinkHandler.openURL(it, AppURLs.REPORT_ERROR)
                }
            },
        )
    }
}

@Composable
fun CatimaAboutSection(
    modifier: Modifier = Modifier,
    title: String,
    message: String? = null,
    onClickDialogText: AnnotatedString? = null,
    openURL: (() -> Unit)? = null,
) {
    var openDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .testTag(title)
            .heightIn(min = 60.dp)
            .clickable {
                when {
                    onClickDialogText != null -> openDialog = true

                    openURL != null -> {
                        openDialog = false
                        openURL()
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            if (!message.isNullOrEmpty()) Text(text = message)
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
        )
    }
    if (openDialog && onClickDialogText != null) {
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
                openDialog = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = if (openURL == null) null else {
                {
                    TextButton(
                        onClick = {
                            openDialog = false
                            openURL()
                        }
                    ) {
                        Text(stringResource(R.string.view_online))
                    }
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AboutActivityPreview() {
    AboutScreenContent(
        showDonate = true,
        showRateOnGooglePlay = true,
    )
}