package protect.card_locker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.zxing.BarcodeFormat
import java.util.ArrayList
import protect.card_locker.databinding.BarcodeSelectorActivityBinding

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
class BarcodeSelectorActivity : CatimaAppCompatActivity(), BarcodeSelectorAdapter.BarcodeSelectorListener {
    private lateinit var binding: BarcodeSelectorActivityBinding

    private companion object {
        private const val TAG = "Catima"

        // Result this activity will return
        const val BARCODE_CONTENTS = "contents"
        const val BARCODE_FORMAT = "format"
    }

    private val typingDelayHandler = Handler(Looper.getMainLooper())
    private val INPUT_DELAY = 250

    private lateinit var mAdapter: BarcodeSelectorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BarcodeSelectorActivityBinding.inflate(layoutInflater)
        setTitle(R.string.selectBarcodeTitle)
        setContentView(binding.root)
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        enableToolbarBackButton()

        val cardId = binding.cardId
        val mBarcodeList = binding.barcodes
        mAdapter = BarcodeSelectorAdapter(this, ArrayList(), this)
        mBarcodeList.adapter = mAdapter

        cardId.addTextChangedListener(object : SimpleTextWatcher() {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // Delay the input processing so we avoid overload
                typingDelayHandler.removeCallbacksAndMessages(null)

                typingDelayHandler.postDelayed({
                    Log.d(TAG, "Entered text: $s")

                    runOnUiThread {
                        generateBarcodes(s.toString())
                    }
                }, INPUT_DELAY.toLong())
            }
        })

        val b = intent.extras
        val initialCardId = b?.getString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID)

        if (initialCardId != null) {
            cardId.setText(initialCardId)
        } else {
            generateBarcodes("")
        }
    }

    private fun generateBarcodes(value: String) {
        // Update barcodes
        val barcodes = ArrayList<CatimaBarcodeWithValue>()
        for (barcodeFormat in CatimaBarcode.barcodeFormats) {
            val catimaBarcode = CatimaBarcode.fromBarcode(barcodeFormat)
            barcodes.add(CatimaBarcodeWithValue(catimaBarcode, value))
        }
        mAdapter.setBarcodes(barcodes)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRowClicked(inputPosition: Int, view: View) {
        val barcodeWithValue = mAdapter.getItem(inputPosition)
        val catimaBarcode = barcodeWithValue?.catimaBarcode()

        if (!mAdapter.isValid(view)) {
            Toast.makeText(this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show()
            return
        }

        val barcodeFormat = catimaBarcode?.format()?.name
        val value = barcodeWithValue?.value()

        Log.d(TAG, "Selected barcode type $barcodeFormat")

        val result = Intent()
        result.putExtra(BARCODE_FORMAT, barcodeFormat)
        result.putExtra(BARCODE_CONTENTS, value)
        setResult(RESULT_OK, result)
        finish()
    }
}
