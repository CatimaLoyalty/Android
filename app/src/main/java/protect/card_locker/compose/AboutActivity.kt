package protect.card_locker.compose

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import protect.card_locker.OpenWebLinkHandler
import protect.card_locker.R

@Composable
fun CatimaAboutSection(title: String, message: String, onClickUrl: String? = null, onClickDialogText: AnnotatedString? = null) {
    val activity = LocalActivity.current

    val openDialog = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable {
                if (onClickDialogText != null) {
                    openDialog.value = true
                } else if (onClickUrl != null) {
                    OpenWebLinkHandler().openBrowser(activity, onClickUrl)
                }
            }
    ) {
        Row {
            Column(modifier = Modifier.weight(1F)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(text = message)
            }
            Text(modifier = Modifier.align(Alignment.CenterVertically),
                text = ">",
                style = MaterialTheme.typography.titleMedium
            )
        }
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
                    modifier = Modifier.verticalScroll(rememberScrollState()))
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