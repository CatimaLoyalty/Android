package protect.card_locker

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import protect.card_locker.databinding.BarcodeWidgetConfigureActivityBinding
import protect.card_locker.preferences.Settings

class BarcodeWidgetConfigureActivity : CatimaAppCompatActivity(),
    LoyaltyCardCursorAdapter.CardAdapterListener {

    private lateinit var binding: BarcodeWidgetConfigureActivityBinding
    private lateinit var mDatabase: SQLiteDatabase
    private lateinit var mAdapter: LoyaltyCardCursorAdapter
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = BarcodeWidgetConfigureActivityBinding.inflate(layoutInflater)
        mDatabase = DBHelper(this).readableDatabase

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        setResult(RESULT_CANCELED)

        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)

        binding.toolbar.apply {
            setTitle(R.string.barcode_widget_configure_title)
            setSupportActionBar(this)
        }

        if (DBHelper.getLoyaltyCardCount(mDatabase) == 0) {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val cardCursor = DBHelper.getLoyaltyCardCursor(mDatabase)
        mAdapter = LoyaltyCardCursorAdapter(this, cardCursor, this, null)
        binding.list.adapter = mAdapter
    }

    override fun onResume() {
        super.onResume()
        val layoutManager = binding.list.layoutManager as GridLayoutManager?
        layoutManager?.spanCount = Settings(this).getPreferredColumnCount()
    }

    override fun onRowClicked(position: Int) {
        val cursor = DBHelper.getLoyaltyCardCursor(mDatabase)
        cursor.moveToPosition(position)
        val card = LoyaltyCard.fromCursor(this, cursor)

        BarcodeWidget.saveCardPref(this, appWidgetId, card.id)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        BarcodeWidget().onUpdate(this, appWidgetManager, intArrayOf(appWidgetId))

        val resultValue = Intent().putExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            appWidgetId
        )
        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onRowLongClicked(position: Int) {
    }
}
