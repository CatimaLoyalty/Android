package protect.card_locker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import protect.card_locker.async.TaskHandler
import protect.card_locker.compose.theme.CatimaTheme
import protect.card_locker.importexport.DataFormat
import protect.card_locker.importexport.ImportExportResult
import protect.card_locker.importexport.ImportExportResultType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ImportExportActivity : CatimaComponentActivity() {

    private var importExporter: ImportExportTask? = null

    private var currentImportDataFormat: DataFormat? = null
    private var exportPassword: String? = null
    private var lastExportUri: Uri? = null
    private var pendingImportUri: Uri? = null

    private lateinit var fileCreateLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileOpenLauncher: ActivityResultLauncher<String>

    private val mTasks = TaskHandler()

    private var dialogState by mutableStateOf<ImportExportDialogState>(ImportExportDialogState.None)

    companion object {
        private const val TAG = "Catima"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fixedEdgeToEdge()

        val fileIntent = intent
        if (fileIntent?.type != null) {
            fileIntent.data?.let { uri ->
                pendingImportUri = uri
                dialogState = ImportExportDialogState.ImportTypeSelection
            }
        }

        // would use ActivityResultContracts.CreateDocument() but mime type cannot be set
        fileCreateLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val intent = result.data
                if (intent == null) {
                    Log.e(TAG, "Activity returned NULL data")
                    return@registerForActivityResult
                }
                val uri = intent.data
                if (uri == null) {
                    Log.e(TAG, "Activity returned NULL uri")
                    return@registerForActivityResult
                }
                // Running this in a thread prevents Android from throwing a NetworkOnMainThreadException for large files
                // FIXME: This is still suboptimal, because showing that the export started is delayed until the network request finishes
                Thread {
                    try {
                        val writer = contentResolver.openOutputStream(uri)
                        Log.d(TAG, "Starting file export with: $result")
                        startExport(writer, uri, exportPassword?.toCharArray(), true)
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to export file: $result", e)
                        onExportComplete(
                            ImportExportResult(
                                ImportExportResultType.GenericFailure,
                                result.toString()
                            ), uri
                        )
                    }
                }.start()
            }

        fileOpenLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
                if (result == null) {
                    Log.e(TAG, "Activity returned NULL data")
                    return@registerForActivityResult
                }
                openFileForImport(result, null)
            }

        val importOptions = buildImportOptions()

        setContent {
            CatimaTheme {
                ImportExportScreen(
                    onBackPressedDispatcher = onBackPressedDispatcher,
                    importOptions = importOptions,
                    dialogState = dialogState,
                    onDialogStateChange = { newState -> dialogState = newState },
                    onExportWithPassword = { password -> startExportFlow(password) },
                    onImportSelected = { option -> handleImportSelection(option) },
                    onImportWithPassword = { _, password ->
                        pendingImportUri?.let { uri ->
                            openFileForImport(uri, password.toCharArray())
                        }
                    },
                    onShareExport = { shareExportedFile() }
                )
            }
        }

        // FIXME: The importer/exporter is currently quite broken
        // To prevent the screen from turning off during import/export and some devices killing Catima as it's no longer foregrounded, force the screen to stay on here
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun buildImportOptions(): List<ImportOption> {
        val importTypesArray = resources.getStringArray(R.array.import_types_array)
        return listOf(
            ImportOption(
                title = importTypesArray.getOrElse(0) { getString(R.string.importCatima) },
                message = getString(R.string.importCatimaMessage),
                dataFormat = DataFormat.Catima
            ),
            ImportOption(
                title = importTypesArray.getOrElse(1) { "Fidme" },
                message = getString(R.string.importFidmeMessage),
                dataFormat = DataFormat.Fidme,
                isBeta = true
            ),
            ImportOption(
                title = importTypesArray.getOrElse(2) { getString(R.string.importLoyaltyCardKeychain) },
                message = getString(R.string.importLoyaltyCardKeychainMessage),
                dataFormat = DataFormat.Catima
            ),
            ImportOption(
                title = importTypesArray.getOrElse(3) { getString(R.string.importVoucherVault) },
                message = getString(R.string.importVoucherVaultMessage),
                dataFormat = DataFormat.VoucherVault
            )
        )
    }

    private fun startExportFlow(password: String) {
        exportPassword = password
        val intentCreateDocumentAction = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "catima.zip")
        }
        try {
            fileCreateLauncher.launch(intentCreateDocumentAction)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                R.string.failedOpeningFileManager,
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "No activity found to handle intent", e)
        }
    }

    private fun handleImportSelection(option: ImportOption) {
        currentImportDataFormat = option.dataFormat

        // If we have a pending URI from intent, use it directly
        pendingImportUri?.let { uri ->
            openFileForImport(uri, null)
            return
        }

        // Otherwise open file picker
        try {
            fileOpenLauncher.launch("*/*")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                applicationContext,
                R.string.failedOpeningFileManager,
                Toast.LENGTH_LONG
            ).show()
            Log.e(TAG, "No activity found to handle intent", e)
        }
    }

    private fun openFileForImport(uri: Uri, password: CharArray?) {
        pendingImportUri = uri
        // Running this in a thread prevents Android from throwing a NetworkOnMainThreadException for large files
        // FIXME: This is still suboptimal, because showing that the import started is delayed until the network request finishes
        Thread {
            try {
                val reader = contentResolver.openInputStream(uri)
                Log.d(TAG, "Starting file import with: $uri")
                startImport(reader, uri, currentImportDataFormat, password, true)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to import file: $uri", e)
                onImportComplete(
                    ImportExportResult(
                        ImportExportResultType.GenericFailure,
                        e.toString()
                    ), currentImportDataFormat
                )
            }
        }.start()
    }

    private fun startImport(
        target: InputStream?,
        targetUri: Uri,
        dataFormat: DataFormat?,
        password: CharArray?,
        closeWhenDone: Boolean
    ) {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false)
        val listener = ImportExportTask.TaskCompleteListener { result, format ->
            onImportComplete(result, format)
            if (closeWhenDone) {
                try {
                    target?.close()
                } catch (ioException: IOException) {
                    ioException.printStackTrace()
                }
            }
        }

        importExporter = ImportExportTask(
            this@ImportExportActivity,
            dataFormat, target, password, listener
        )
        mTasks.executeTask(TaskHandler.TYPE.IMPORT, importExporter)
    }

    private fun startExport(
        target: OutputStream?,
        targetUri: Uri,
        password: CharArray?,
        closeWhenDone: Boolean
    ) {
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, true, false, false)
        val listener = ImportExportTask.TaskCompleteListener { result, _ ->
            onExportComplete(result, targetUri)
            if (closeWhenDone) {
                try {
                    target?.close()
                } catch (ioException: IOException) {
                    ioException.printStackTrace()
                }
            }
        }

        importExporter = ImportExportTask(
            this@ImportExportActivity,
            DataFormat.Catima, target, password, listener
        )
        mTasks.executeTask(TaskHandler.TYPE.EXPORT, importExporter)
    }

    override fun onDestroy() {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false)
        mTasks.flushTaskList(TaskHandler.TYPE.EXPORT, true, false, false)
        super.onDestroy()
    }

    private fun onImportComplete(result: ImportExportResult, dataFormat: DataFormat?) {
        val resultType = result.resultType()

        if (resultType == ImportExportResultType.BadPassword) {
            runOnUiThread {
                dialogState = ImportExportDialogState.ImportPassword(dataFormat!!)
            }
            return
        }

        runOnUiThread {
            dialogState = ImportExportDialogState.ImportResult(result, dataFormat)
        }
    }

    private fun onExportComplete(result: ImportExportResult, path: Uri) {
        lastExportUri = path
        val isSuccess = result.resultType() == ImportExportResultType.Success
        runOnUiThread {
            dialogState = ImportExportDialogState.ExportResult(result, canShare = isSuccess)
        }
    }

    private fun shareExportedFile() {
        lastExportUri?.let { uri ->
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "text/csv"
                // set flag to give temporary permission to external app to use the FileProvider
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(sendIntent, resources.getText(R.string.sendLabel)))
        }
    }
}