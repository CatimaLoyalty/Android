package protect.card_locker


import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListAdapter
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.DecodeHintType
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import protect.card_locker.databinding.CustomBarcodeScannerBinding
import protect.card_locker.databinding.ScanActivityBinding

/**
 * Custom Scannner Activity extending from Activity to display a custom layout form scanner view.
 * <p>
 * Based on https://github.com/journeyapps/zxing-android-embedded/blob/0fdfbce9fb3285e985bad9971c5f7c0a7a334e7b/sample/src/main/java/example/zxing/CustomScannerActivity.java
 * originally licensed under Apache 2.0
 */
class ScanActivity : CatimaAppCompatActivity() {
    private lateinit var binding: ScanActivityBinding
    private lateinit var customBarcodeScannerBinding: CustomBarcodeScannerBinding

    companion object {
        private const val TAG = "Catima"

        private const val MEDIUM_SCALE_FACTOR_DIP = 460
        private const val COMPAT_SCALE_FACTOR_DIP = 320

        private const val PERMISSION_SCAN_ADD_FROM_IMAGE = 100
        private const val PERMISSION_SCAN_ADD_FROM_PDF = 101
        private const val PERMISSION_SCAN_ADD_FROM_PKPASS = 102

        private const val STATE_SCANNER_ACTIVE = "scannerActive"
    }

    private lateinit var capture: CaptureManager
    private lateinit var barcodeScannerView: DecoratedBarcodeView
    private var cardId: String? = null
    private var addGroup: String? = null
    private var torch = false

    private lateinit var manualAddLauncher: ActivityResultLauncher<Intent>
    // can't use the pre-made contract because that launches the file manager for image type instead of gallery
    private lateinit var photoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var pdfPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var pkpassPickerLauncher: ActivityResultLauncher<Intent>

    private var mScannerActive = true
    private var mHasError = false

    private fun extractIntentFields(intent: Intent) {
        val b = intent.extras
        cardId = b?.getString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID)
        addGroup = b?.getString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP)
        Log.d(TAG, "Scan activity: id=$cardId")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScanActivityBinding.inflate(layoutInflater)
        customBarcodeScannerBinding = CustomBarcodeScannerBinding.bind(binding.zxingBarcodeScanner)
        setTitle(R.string.scanCardBarcode)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)
        setSupportActionBar(binding.toolbar)
        enableToolbarBackButton()

        extractIntentFields(intent)

        manualAddLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleActivityResult(
                    Utils.SELECT_BARCODE_REQUEST,
                    result.resultCode,
                    result.data
                )
            }
        photoPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleActivityResult(
                    Utils.BARCODE_IMPORT_FROM_IMAGE_FILE,
                    result.resultCode,
                    result.data
                )
            }
        pdfPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleActivityResult(
                    Utils.BARCODE_IMPORT_FROM_PDF_FILE,
                    result.resultCode,
                    result.data
                )
            }
        pkpassPickerLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                handleActivityResult(
                    Utils.BARCODE_IMPORT_FROM_PKPASS_FILE,
                    result.resultCode,
                    result.data
                )
            }

        customBarcodeScannerBinding.fabOtherOptions.setOnClickListener {
            setScannerActive(false)

            val list: ArrayList<HashMap<String, Any>> = arrayListOf()
            val texts = arrayOf(
                getString(R.string.addWithoutBarcode),
                getString(R.string.addManually),
                getString(R.string.addFromImage),
                getString(R.string.addFromPdfFile),
                getString(R.string.addFromPkpass)
            )
            val icons = arrayOf(
                R.drawable.baseline_block_24,
                R.drawable.ic_edit,
                R.drawable.baseline_image_24,
                R.drawable.baseline_picture_as_pdf_24,
                R.drawable.local_activity_24px
            )
            val columns = arrayOf("text", "icon")

            for (i in 0 until texts.size) {
                val map: HashMap<String, Any> = hashMapOf()
                map.put(columns[0], texts[i])
                map.put(columns[1], icons[i])
                list.add(map)
            }

            val adapter: ListAdapter = SimpleAdapter(
                this,
                list,
                R.layout.alertdialog_row_with_icon,
                columns,
                intArrayOf(R.id.textView, R.id.imageView)
            )

            val builder = MaterialAlertDialogBuilder(this).apply {
                setTitle(getString(R.string.add_a_card_in_a_different_way))
                setAdapter(adapter) { _, i ->
                    when (i) {
                        0 -> addWithoutBarcode()
                        1 -> addManually()
                        2 -> addFromImage()
                        3 -> addFromPdf()
                        4 -> addFromPkPass()
                        else -> throw IllegalArgumentException(
                            "Unknown 'Add a card in a different way' dialog option: $i"
                        )
                    }
                }
                setOnCancelListener { _ -> setScannerActive(true) }
            }
            builder.show()
        }

        // Configure barcodeScanner
        barcodeScannerView = binding.zxingBarcodeScanner

        val barcodeScannerIntent = Intent().apply {
            val barcodeScannerIntentBundle = Bundle().apply {
                putBoolean(DecodeHintType.ALSO_INVERTED.name, true)
            }
            putExtras(barcodeScannerIntentBundle)
        }
        barcodeScannerView.initializeFromIntent(barcodeScannerIntent)

        // Even though we do the actual decoding with the barcodeScannerView
        // CaptureManager needs to be running to show the camera and scanning bar
        capture = CatimaCaptureManager(this, barcodeScannerView, this::onCaptureManagerError)
        val captureIntent = Intent().apply {
            val captureIntentBundle = Bundle().apply {
                putBoolean(DecodeHintType.ALSO_INVERTED.name, false)
            }
            putExtras(captureIntentBundle)
        }
        capture.initializeFromIntent(captureIntent, savedInstanceState)

        barcodeScannerView.decodeSingle(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                val loyaltyCard = LoyaltyCard().apply {
                    setCardId(result.text)
                    setBarcodeType(CatimaBarcode.fromBarcode(result.barcodeFormat))
                }

                returnResult(ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard))
            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint?>?) {}
        })
    }

    override fun onResume() {
        super.onResume()

        if (mScannerActive) {
            capture.onResume()
        }

        if (!Utils.deviceHasCamera(this)) {
            showCameraError(getString(R.string.noCameraFoundGuideText), false)
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showCameraPermissionMissingText()
        } else {
            hideCameraError()
        }

        scaleScreen()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        capture.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putBoolean(STATE_SCANNER_ACTIVE, mScannerActive)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mScannerActive = savedInstanceState.getBoolean(STATE_SCANNER_ACTIVE)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return barcodeScannerView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            menuInflater.inflate(R.menu.scan_menu, menu)
        }

        barcodeScannerView.setTorchOff()

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        } else if (item.itemId == R.id.action_toggle_flashlight) {
            if (torch) {
                torch = false
                barcodeScannerView.setTorchOff()
                item.setTitle(R.string.turn_flashlight_on)
                item.setIcon(R.drawable.ic_flashlight_off_white_24dp)
            } else {
                torch = true
                barcodeScannerView.setTorchOn()
                item.setTitle(R.string.turn_flashlight_off)
                item.setIcon(R.drawable.ic_flashlight_on_white_24dp)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setScannerActive(isActive: Boolean) {
        if (isActive) {
            barcodeScannerView.resume()
        } else {
            barcodeScannerView.pause()
        }
        mScannerActive = isActive
    }

    private fun returnResult(parseResult: ParseResult) {
        val bundle = parseResult.toLoyaltyCardBundle(this).apply {
            addGroup?.let { putString(LoyaltyCardEditActivity.BUNDLE_ADDGROUP, it) }
        }
        val result = Intent().apply { putExtras(bundle) }
        this.setResult(RESULT_OK, result)
        finish()
    }

    private fun handleActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(resultCode, resultCode, intent)

        val parseResultList: List<ParseResult> =
            Utils.parseSetBarcodeActivityResult(requestCode, resultCode, intent, this)

        if (parseResultList.isEmpty()) {
            setScannerActive(true)
            return
        }


        Utils.makeUserChooseParseResultFromList(
            this,
            parseResultList,
            object : ParseResultListDisambiguatorCallback {
                override fun onUserChoseParseResult(parseResult: ParseResult) {
                    returnResult(parseResult)
                }

                override fun onUserDismissedSelector() {
                    setScannerActive(true)
                }
            })
    }

    private fun addWithoutBarcode() {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this).apply {
            setOnCancelListener { dialogInterface -> setScannerActive(true) }
            // Header
            setTitle(R.string.addWithoutBarcode)
        }

        // Layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val contentPadding = resources.getDimensionPixelSize(R.dimen.alert_dialog_content_padding)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = contentPadding
            topMargin = contentPadding / 2
            rightMargin = contentPadding
        }

        // Description
        val currentTextview = TextView(this).apply {
            text = getString(R.string.enter_card_id)
            layoutParams = params
        }
        layout.addView(currentTextview)

        //EditText with spacing
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            layoutParams = params
        }
        layout.addView(input)

        // Set layout
        builder.setView(layout).apply {

            setPositiveButton(getString(R.string.ok)) { _, _ ->
                val loyaltyCard = LoyaltyCard()
                loyaltyCard.cardId = input.text.toString()
                returnResult(ParseResult(ParseResultType.BARCODE_ONLY, loyaltyCard))
            }
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
        }
        val dialog: AlertDialog = builder.create()

        // Now that the dialog exists, we can bind something that affects the OK button
        input.doOnTextChanged { text, _, _, _ ->
            if (text.isNullOrEmpty()) {
                input.error = getString(R.string.card_id_must_not_be_empty)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            } else {
                input.error = null
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            }
        }

        dialog.show()

        // Disable button (must be done **after** dialog is shown to prevent crash
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        // Set focus on input field
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        input.requestFocus()
    }

    fun addManually() {
        val builder = MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.add_manually_warning_title)
            setMessage(R.string.add_manually_warning_message)
            setPositiveButton(R.string.continue_) { _, _ ->
                val i = Intent(applicationContext, BarcodeSelectorActivity::class.java)
                if (cardId != null) {
                    val b = Bundle()
                    b.putString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID, cardId)
                    i.putExtras(b)
                }
                manualAddLauncher.launch(i)
            }
            setNegativeButton(R.string.cancel) { _, _ -> setScannerActive(true) }
            setOnCancelListener { _ -> setScannerActive(true) }
        }
        builder.show()
    }

    fun addFromImage() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_IMAGE)
    }

    fun addFromPdf() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_PDF)
    }

    fun addFromPkPass() {
        PermissionUtils.requestStorageReadPermission(this, PERMISSION_SCAN_ADD_FROM_PKPASS)
    }

    private fun addFromImageOrFileAfterPermission(
        mimeType: String,
        launcher: ActivityResultLauncher<Intent>,
        chooserText: Int,
        errorMessage: Int
    ) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = mimeType
        val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentIntent.type = mimeType

        val chooserIntent = Intent.createChooser(photoPickerIntent, getString(chooserText))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(contentIntent))
        try {
            launcher.launch(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            setScannerActive(true)
            Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
            Log.e(TAG, "No activity found to handle intent", e)
        }
    }

    fun onCaptureManagerError(errorMessage: String) {
        if (mHasError) {
            // We're already showing an error, ignore this new error
            return
        }

        showCameraError(errorMessage, false)
    }

    private fun showCameraPermissionMissingText() {
        showCameraError(getString(R.string.noCameraPermissionDirectToSystemSetting), true)
    }

    private fun showCameraError(message: String, setOnClick: Boolean) {
        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorMessage.text = message

        setCameraErrorState(true, setOnClick)
    }

    private fun hideCameraError() {
        setCameraErrorState(false, false)
    }

    private fun setCameraErrorState(visible: Boolean, setOnClick: Boolean) {
        mHasError = visible
        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorClickableArea.setOnClickListener(
            if (visible && setOnClick) { _ -> navigateToSystemPermissionSetting() }
            else null
        )
        customBarcodeScannerBinding.cardInputContainer.setBackgroundColor(
            if (visible) obtainThemeAttribute(com.google.android.material.R.attr.colorSurface)
            else Color.TRANSPARENT
        )
        customBarcodeScannerBinding.cameraErrorLayout.root.visibility =
            if (visible) View.VISIBLE else View.GONE
    }

    private fun scaleScreen() {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenHeight: Int = displayMetrics.heightPixels
        val mediumSizePx: Float = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            MEDIUM_SCALE_FACTOR_DIP.toFloat(),
            resources.displayMetrics
        )
        val shouldScaleSmaller = screenHeight < mediumSizePx

        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorIcon.visibility =
            if (shouldScaleSmaller) View.GONE else View.VISIBLE
        customBarcodeScannerBinding.cameraErrorLayout.cameraErrorTitle.visibility =
            if (shouldScaleSmaller) View.GONE else View.VISIBLE
    }

    private fun obtainThemeAttribute(attribute: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attribute, typedValue, true)
        return typedValue.data
    }

    private fun navigateToSystemPermissionSetting() {
        val permissionIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", getPackageName(), null)
        )
        permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(permissionIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        onMockedRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onMockedRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        val granted =
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED

        if (requestCode == CaptureManager.getCameraPermissionReqCode()) {
            if (granted) {
                hideCameraError()
            } else {
                showCameraPermissionMissingText()
            }
        } else if (requestCode in listOf(
                PERMISSION_SCAN_ADD_FROM_IMAGE,
                PERMISSION_SCAN_ADD_FROM_PDF,
                PERMISSION_SCAN_ADD_FROM_PKPASS
            )
        ) {
            if (granted) {
                if (requestCode == PERMISSION_SCAN_ADD_FROM_IMAGE) {
                    addFromImageOrFileAfterPermission(
                        "image/*",
                        photoPickerLauncher,
                        R.string.addFromImage,
                        R.string.failedLaunchingPhotoPicker
                    )
                } else if (requestCode == PERMISSION_SCAN_ADD_FROM_PDF) {
                    addFromImageOrFileAfterPermission(
                        "application/pdf",
                        pdfPickerLauncher,
                        R.string.addFromPdfFile,
                        R.string.failedLaunchingFileManager
                    )
                } else {
                    addFromImageOrFileAfterPermission(
                        "application/*",
                        pkpassPickerLauncher,
                        R.string.addFromPkpass,
                        R.string.failedLaunchingFileManager
                    )
                }
            } else {
                setScannerActive(true)
                Toast.makeText(this, R.string.storageReadPermissionRequired, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}
