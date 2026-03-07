package protect.card_locker

import androidx.activity.OnBackPressedDispatcher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import protect.card_locker.compose.CatimaTopAppBar
import protect.card_locker.importexport.DataFormat
import protect.card_locker.importexport.ImportExportResult
import protect.card_locker.importexport.ImportExportResultType

data class ImportOption(
    val title: String,
    val message: String,
    val dataFormat: DataFormat,
    val isBeta: Boolean = false
)

sealed class ImportExportDialogState {
    data object None : ImportExportDialogState()
    data object ExportPassword : ImportExportDialogState()
    data object ImportTypeSelection : ImportExportDialogState()
    data class ImportConfirmation(val option: ImportOption) : ImportExportDialogState()
    data class ImportPassword(val dataFormat: DataFormat) : ImportExportDialogState()
    data class ImportResult(val result: ImportExportResult, val dataFormat: DataFormat?) :
        ImportExportDialogState()

    data class ExportResult(val result: ImportExportResult, val canShare: Boolean) :
        ImportExportDialogState()
}

@Composable
fun ImportExportScreen(
    onBackPressedDispatcher: OnBackPressedDispatcher?,
    importOptions: List<ImportOption>,
    dialogState: ImportExportDialogState,
    onDialogStateChange: (ImportExportDialogState) -> Unit,
    onExportWithPassword: (String) -> Unit,
    onImportSelected: (ImportOption) -> Unit,
    onImportWithPassword: (DataFormat, String) -> Unit,
    onShareExport: () -> Unit,
) {
    Scaffold(
        topBar = {
            CatimaTopAppBar(
                title = stringResource(R.string.importExport),
                onBackPressedDispatcher = onBackPressedDispatcher
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.importExportHelp),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.exportName),
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.exportOptionExplanation),
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Button(
                onClick = { onDialogStateChange(ImportExportDialogState.ExportPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.exportName))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = stringResource(R.string.importOptionFilesystemTitle),
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.importOptionFilesystemExplanation),
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
            Button(
                onClick = { onDialogStateChange(ImportExportDialogState.ImportTypeSelection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.importOptionFilesystemButton))
            }
        }

        // Dialogs
        when (dialogState) {
            is ImportExportDialogState.ExportPassword -> {
                ExportPasswordDialog(
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) },
                    onConfirm = { password ->
                        onDialogStateChange(ImportExportDialogState.None)
                        onExportWithPassword(password)
                    }
                )
            }

            is ImportExportDialogState.ImportTypeSelection -> {
                ImportTypeSelectionDialog(
                    importOptions = importOptions,
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) },
                    onSelect = { option ->
                        onDialogStateChange(ImportExportDialogState.ImportConfirmation(option))
                    }
                )
            }

            is ImportExportDialogState.ImportConfirmation -> {
                ImportConfirmationDialog(
                    option = dialogState.option,
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) },
                    onConfirm = {
                        onDialogStateChange(ImportExportDialogState.None)
                        onImportSelected(dialogState.option)
                    }
                )
            }

            is ImportExportDialogState.ImportPassword -> {
                ImportPasswordDialog(
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) },
                    onConfirm = { password ->
                        onDialogStateChange(ImportExportDialogState.None)
                        onImportWithPassword(dialogState.dataFormat, password)
                    }
                )
            }

            is ImportExportDialogState.ImportResult -> {
                ImportResultDialog(
                    result = dialogState.result,
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) }
                )
            }

            is ImportExportDialogState.ExportResult -> {
                ExportResultDialog(
                    result = dialogState.result,
                    canShare = dialogState.canShare,
                    onDismiss = { onDialogStateChange(ImportExportDialogState.None) },
                    onShare = {
                        onDialogStateChange(ImportExportDialogState.None)
                        onShareExport()
                    }
                )
            }

            ImportExportDialogState.None -> { /* No dialog */
            }
        }
    }
}

@Composable
private fun ExportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exportPassword)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.exportPasswordHint)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImportTypeSelectionDialog(
    importOptions: List<ImportOption>,
    onDismiss: () -> Unit,
    onSelect: (ImportOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chooseImportType)) },
        text = {
            Column {
                importOptions.forEach { option ->
                    val displayTitle = if (option.isBeta) "${option.title} (BETA)" else option.title
                    Text(
                        text = displayTitle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImportConfirmationDialog(
    option: ImportOption,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(option.title) },
        text = { Text(option.message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.passwordRequired)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.exportPasswordHint)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun ImportResultDialog(
    result: ImportExportResult,
    onDismiss: () -> Unit
) {
    val isSuccess = result.resultType() == ImportExportResultType.Success
    val titleRes = if (isSuccess) R.string.importSuccessfulTitle else R.string.importFailedTitle
    val messageRes = if (isSuccess) R.string.importSuccessful else R.string.importFailed

    val message = buildString {
        append(stringResource(messageRes))
        if (result.developerDetails() != null) {
            append("\n\n")
            append(stringResource(R.string.include_if_asking_support))
            append("\n\n")
            append(result.developerDetails())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Text(
                text = message,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Composable
private fun ExportResultDialog(
    result: ImportExportResult,
    canShare: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val isSuccess = result.resultType() == ImportExportResultType.Success
    val titleRes = if (isSuccess) R.string.exportSuccessfulTitle else R.string.exportFailedTitle
    val messageRes = if (isSuccess) R.string.exportSuccessful else R.string.exportFailed

    val message = buildString {
        append(stringResource(messageRes))
        if (result.developerDetails() != null) {
            append("\n\n")
            append(stringResource(R.string.include_if_asking_support))
            append("\n\n")
            append(result.developerDetails())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Text(
                text = message,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            if (isSuccess && canShare) {
                TextButton(onClick = onShare) {
                    Text(stringResource(R.string.sendLabel))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun ImportExportScreenPreview() {
    val sampleImportOptions = listOf(
        ImportOption(
            title = "Catima",
            message = "Import from Catima backup",
            dataFormat = DataFormat.Catima
        ),
        ImportOption(
            title = "Fidme",
            message = "Import from Fidme",
            dataFormat = DataFormat.Fidme,
            isBeta = true
        ),
        ImportOption(
            title = "Loyalty Card Keychain",
            message = "Import from Loyalty Card Keychain",
            dataFormat = DataFormat.Catima
        ),
        ImportOption(
            title = "Voucher Vault",
            message = "Import from Voucher Vault",
            dataFormat = DataFormat.VoucherVault
        )
    )

    ImportExportScreen(
        onBackPressedDispatcher = null,
        importOptions = sampleImportOptions,
        dialogState = ImportExportDialogState.None,
        onDialogStateChange = {},
        onExportWithPassword = {},
        onImportSelected = {},
        onImportWithPassword = { _, _ -> },
        onShareExport = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ImportTypeSelectionDialogPreview() {
    val sampleImportOptions = listOf(
        ImportOption(
            title = "Catima",
            message = "Import from Catima backup",
            dataFormat = DataFormat.Catima
        ),
        ImportOption(
            title = "Fidme",
            message = "Import from Fidme",
            dataFormat = DataFormat.Fidme,
            isBeta = true
        )
    )

    ImportTypeSelectionDialog(
        importOptions = sampleImportOptions,
        onDismiss = {},
        onSelect = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun ExportPasswordDialogPreview() {
    ExportPasswordDialog(
        onDismiss = {},
        onConfirm = {}
    )
}