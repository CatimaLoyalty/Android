package protect.card_locker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import protect.card_locker.async.TaskHandler
import protect.card_locker.databinding.ImportExportActivityBinding
import protect.card_locker.importexport.DataFormat
import protect.card_locker.importexport.ImportExportResult
import protect.card_locker.importexport.ImportExportResultType
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ImportExportActivity : CatimaAppCompatActivity() {
    private lateinit var binding: ImportExportActivityBinding

    private var importExporter: ImportExportTask? = null

    private var importAlertTitle: String? = null
    private var importAlertMessage: String? = null
    private var importDataFormat: DataFormat? = null
    private var exportPassword: String? = null

    private lateinit var fileCreateLauncher: ActivityResultLauncher<Intent>
    private lateinit var fileOpenLauncher: ActivityResultLauncher<String>

    private val mTasks = TaskHandler()

    companion object {
        private const val TAG = "Catima"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ImportExportActivityBinding.inflate(layoutInflater)
        setTitle(R.string.importExport)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        enableToolbarBackButton()

        val fileIntent = intent
        if (fileIntent?.type != null) {
            chooseImportType(fileIntent.data)
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

        // Check that there is a file manager available
        val intentCreateDocumentAction = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, "catima.zip")
        }

        val exportButton: Button = binding.exportButton
        exportButton.setOnClickListener {
            val builder = MaterialAlertDialogBuilder(this@ImportExportActivity)
            builder.setTitle(R.string.exportPassword)

            val container = FrameLayout(this@ImportExportActivity)

            val textInputLayout = TextInputLayout(this@ImportExportActivity).apply {
                endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(50, 10, 50, 0)
                }
            }

            val input = EditText(this@ImportExportActivity).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setHint(R.string.exportPasswordHint)
            }

            textInputLayout.addView(input)
            container.addView(textInputLayout)
            builder.setView(container)
            builder.setPositiveButton(R.string.ok) { _, _ ->
                exportPassword = input.text.toString()
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
            builder.setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.cancel() }
            builder.show()
        }

        // Check that there is a file manager available
        val importFilesystem: Button = binding.importOptionFilesystemButton
        importFilesystem.setOnClickListener { chooseImportType(null) }

        // FIXME: The importer/exporter is currently quite broken
        // To prevent the screen from turning off during import/export and some devices killing Catima as it's no longer foregrounded, force the screen to stay on here
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun openFileForImport(uri: Uri, password: CharArray?) {
        // Running this in a thread prevents Android from throwing a NetworkOnMainThreadException for large files
        // FIXME: This is still suboptimal, because showing that the import started is delayed until the network request finishes
        Thread {
            try {
                val reader = contentResolver.openInputStream(uri)
                Log.d(TAG, "Starting file import with: $uri")
                startImport(reader, uri, importDataFormat, password, true)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to import file: $uri", e)
                onImportComplete(
                    ImportExportResult(
                        ImportExportResultType.GenericFailure,
                        e.toString()
                    ), uri, importDataFormat
                )
            }
        }.start()
    }

    private fun chooseImportType(fileData: Uri?) {
        val betaImportOptions = mutableListOf<CharSequence>()
        betaImportOptions.add("Fidme")
        val importOptions = mutableListOf<CharSequence>()

        for (importOption in resources.getStringArray(R.array.import_types_array)) {
            var option = importOption
            if (betaImportOptions.contains(importOption)) {
                option = "$importOption (BETA)"
            }
            importOptions.add(option)
        }

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.chooseImportType)
            .setItems(importOptions.toTypedArray()) { _, which ->
                when (which) {
                    // Catima
                    0 -> {
                        importAlertTitle = getString(R.string.importCatima)
                        importAlertMessage = getString(R.string.importCatimaMessage)
                        importDataFormat = DataFormat.Catima
                    }
                    // Fidme
                    1 -> {
                        importAlertTitle = getString(R.string.importFidme)
                        importAlertMessage = getString(R.string.importFidmeMessage)
                        importDataFormat = DataFormat.Fidme
                    }
                    // Loyalty Card Keychain
                    2 -> {
                        importAlertTitle = getString(R.string.importLoyaltyCardKeychain)
                        importAlertMessage = getString(R.string.importLoyaltyCardKeychainMessage)
                        importDataFormat = DataFormat.Catima
                    }
                    // Voucher Vault
                    3 -> {
                        importAlertTitle = getString(R.string.importVoucherVault)
                        importAlertMessage = getString(R.string.importVoucherVaultMessage)
                        importDataFormat = DataFormat.VoucherVault
                    }

                    else -> throw IllegalArgumentException("Unknown DataFormat")
                }

                if (fileData != null) {
                    openFileForImport(fileData, null)
                    return@setItems
                }

                MaterialAlertDialogBuilder(this)
                    .setTitle(importAlertTitle)
                    .setMessage(importAlertMessage)
                    .setPositiveButton(R.string.ok) { _, _ ->
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
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        builder.show()
    }

    private fun startImport(
        target: InputStream?,
        targetUri: Uri,
        dataFormat: DataFormat?,
        password: CharArray?,
        closeWhenDone: Boolean
    ) {
        mTasks.flushTaskList(TaskHandler.TYPE.IMPORT, true, false, false)
        val listener = ImportExportTask.TaskCompleteListener { result, dataFormat ->
            onImportComplete(result, targetUri, dataFormat)
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
        val listener = ImportExportTask.TaskCompleteListener { result, dataFormat ->
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun retryWithPassword(dataFormat: DataFormat, uri: Uri) {
        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(R.string.passwordRequired)

        val container = FrameLayout(this@ImportExportActivity)

        val textInputLayout = TextInputLayout(this@ImportExportActivity).apply {
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(50, 10, 50, 0)
            }
        }

        val input = EditText(this@ImportExportActivity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setHint(R.string.exportPasswordHint)
        }

        textInputLayout.addView(input)
        container.addView(textInputLayout)
        builder.setView(container)

        builder.setPositiveButton(R.string.ok) { _, _ ->
            openFileForImport(uri, input.text.toString().toCharArray())
        }
        builder.setNegativeButton(R.string.cancel) { dialogInterface, _ -> dialogInterface.cancel() }

        builder.show()
    }

    private fun buildResultDialogMessage(result: ImportExportResult, isImport: Boolean): String {
        val messageId = if (result.resultType() == ImportExportResultType.Success) {
            if (isImport) R.string.importSuccessful else R.string.exportSuccessful
        } else {
            if (isImport) R.string.importFailed else R.string.exportFailed
        }

        val messageBuilder = StringBuilder(resources.getString(messageId))
        if (result.developerDetails() != null) {
            messageBuilder.append("\n\n")
            messageBuilder.append(resources.getString(R.string.include_if_asking_support))
            messageBuilder.append("\n\n")
            messageBuilder.append(result.developerDetails())
        }

        return messageBuilder.toString()
    }

    private fun onImportComplete(result: ImportExportResult, path: Uri, dataFormat: DataFormat?) {
        val resultType = result.resultType()

        if (resultType == ImportExportResultType.BadPassword) {
            retryWithPassword(dataFormat!!, path)
            return
        }

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(if (resultType == ImportExportResultType.Success) R.string.importSuccessfulTitle else R.string.importFailedTitle)
        builder.setMessage(buildResultDialogMessage(result, true))
        builder.setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun onExportComplete(result: ImportExportResult, path: Uri) {
        val resultType = result.resultType()

        val builder = MaterialAlertDialogBuilder(this)
        builder.setTitle(if (resultType == ImportExportResultType.Success) R.string.exportSuccessfulTitle else R.string.exportFailedTitle)
        builder.setMessage(buildResultDialogMessage(result, false))
        builder.setNeutralButton(R.string.ok) { dialog, _ -> dialog.dismiss() }

        if (resultType == ImportExportResultType.Success) {
            val sendLabel = this@ImportExportActivity.resources.getText(R.string.sendLabel)

            builder.setPositiveButton(sendLabel) { dialog, _ ->
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_STREAM, path)
                    type = "text/csv"
                    // set flag to give temporary permission to external app to use the FileProvider
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                this@ImportExportActivity.startActivity(Intent.createChooser(sendIntent, sendLabel))
                dialog.dismiss()
            }
        }

        builder.create().show()
    }
}