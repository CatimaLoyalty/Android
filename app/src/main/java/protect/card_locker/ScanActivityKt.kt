package protect.card_locker


import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import protect.card_locker.databinding.CustomBarcodeScannerBinding
import protect.card_locker.databinding.ScanActivityBinding


class ScanActivityKt : CatimaAppCompatActivity() {
    private lateinit var binding: ScanActivityBinding
    private lateinit var customeBarcodeScannerBinding: CustomBarcodeScannerBinding

    companion object {
        private const val Tag = "Catima"

        private const val MEDIUM_SCALE_FACTOR_DIP = 460
        private const val COMPAT_SCALE_FACTOR_DIP = 320

        private const val PERMISSION_SCAN_ADD_FROM_IMAGE = 100
        private const val PERMISSION_SCAN_ADD_FROM_PDF = 101
        private const val PERMISSION_SCAN_ADD_FROM_PKPASS = 102

        private const val STATE_SCANNER_ACTIVE = "scannerActive"
    }

    private var capture: CaptureManager? = null
    private var barcodeScannerView: DecoratedBarcodeView? = null
    private var cardId: String? = null
    private var addGroup: String? = null
    private var torch = false

    private lateinit var manualAddLauncher: ActivityResultLauncher<Intent>
    private lateinit var photoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var pdfPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var pkpassPickerLauncher: ActivityResultLauncher<Intent>

    private var mScannerActive = true
    private var mHasError = false



}