package protect.card_locker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import protect.card_locker.BarcodeSelectorAdapter.BarcodeSelectorListener
import protect.card_locker.databinding.BarcodeSelectorActivityBinding

/**
 * This activity is callable and will allow a user to enter
 * barcode data and generate all barcodes possible for
 * the data. The user may then select any barcode, where its
 * data and type will be returned to the caller.
 */
class BarcodeSelectorActivity : CatimaAppCompatActivity(), BarcodeSelectorListener, MenuProvider {
    
    private lateinit var binding: BarcodeSelectorActivityBinding
    private lateinit var mAdapter: BarcodeSelectorAdapter
    
    companion object {
        private const val TAG = "Catima"
        
        // Result this activity will return
        const val BARCODE_CONTENTS = "contents"
        const val BARCODE_FORMAT = "format"
        
        const val INPUT_DELAY = 250L
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addMenuProvider(this)
        binding = BarcodeSelectorActivityBinding.inflate(layoutInflater)
        setTitle(R.string.selectBarcodeTitle)
        setContentView(binding.getRoot())
        Utils.applyWindowInsets(binding.getRoot())
        setSupportActionBar(binding.toolbar)
        enableToolbarBackButton()
        
        var typingDelayJob: Job? = null
        val cardId = binding.cardId
        val mBarcodeList = binding.barcodes
        mAdapter = BarcodeSelectorAdapter(this, ArrayList<CatimaBarcodeWithValue?>(), this)
        mBarcodeList.adapter = mAdapter
        
        cardId.doOnTextChanged { s, _, _, _ ->
            typingDelayJob?.cancel()
            typingDelayJob =
                lifecycleScope.launch {
                    delay(INPUT_DELAY) // Delay the input processing so we avoid overload
                    Log.d(TAG, "Entered text: $s")
                    generateBarcodes(s.toString())
                }
        }
        
        val initialCardId = intent.extras?.getString(LoyaltyCard.BUNDLE_LOYALTY_CARD_CARD_ID)
        
        initialCardId?.let {
            cardId.setText(initialCardId)
        } ?: generateBarcodes("")
        
    }
    
    private fun generateBarcodes(value: String?) {
        // Update barcodes
        val barcodes = ArrayList<CatimaBarcodeWithValue?>()
        CatimaBarcode.barcodeFormats.forEach {
            val catimaBarcode = CatimaBarcode.fromBarcode(it)
            barcodes.add(CatimaBarcodeWithValue(catimaBarcode, value))
        }
        mAdapter.setBarcodes(barcodes)
    }
    
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {}
    
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == android.R.id.home) {
            setResult(RESULT_CANCELED)
            finish()
        }
        
        return true
    }
    
    override fun onRowClicked(inputPosition: Int, view: View) {
        val barcodeWithValue = mAdapter.getItem(inputPosition)
        val catimaBarcode = barcodeWithValue!!.catimaBarcode()
        
        if (!mAdapter.isValid(view)) {
            Toast.makeText(this, getString(R.string.wrongValueForBarcodeType), Toast.LENGTH_LONG).show()
            return
        }
        
        val barcodeFormat = catimaBarcode.format().name
        val value = barcodeWithValue.value()
        
        Log.d(TAG, "Selected barcode type $barcodeFormat")
        
        Intent().apply {
            putExtra(BARCODE_FORMAT, barcodeFormat)
            putExtra(BARCODE_CONTENTS, value)
            setResult(RESULT_OK, this)
        }
        finish()
    }
    
}