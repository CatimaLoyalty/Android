package protect.card_locker

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.GridLayoutManager
import protect.card_locker.LoyaltyCardCursorAdapter.CardAdapterListener
import protect.card_locker.databinding.CardShortcutConfigureActivityBinding
import protect.card_locker.preferences.Settings

class CardShortcutConfigure : CatimaAppCompatActivity(), CardAdapterListener, MenuProvider {
    
    private lateinit var binding: CardShortcutConfigureActivityBinding
    private lateinit var mDatabase: SQLiteDatabase
    private lateinit var mAdapter: LoyaltyCardCursorAdapter
    
    private companion object {
        private const val TAG: String = "Catima"
    }
    
    public override fun onCreate(savedInstanceBundle: Bundle?) {
        super.onCreate(savedInstanceBundle)
        addMenuProvider(this)
        binding = CardShortcutConfigureActivityBinding.inflate(layoutInflater)
        mDatabase = DBHelper(this).readableDatabase
        
        // Set the result to CANCELED.
        // This will cause nothing to happen if the back button is pressed.
        setResult(RESULT_CANCELED)
        
        setContentView(binding.getRoot())
        Utils.applyWindowInsets(binding.getRoot())
        
        binding.toolbar.apply {
            setTitle(R.string.shortcutSelectCard)
            setSupportActionBar(this)
        }
        
        // If there are no cards, bail
        if (DBHelper.getLoyaltyCardCount(mDatabase) == 0) {
            Toast.makeText(this, R.string.noCardsMessage, Toast.LENGTH_LONG).show()
            finish()
        }
        
        val cardCursor = DBHelper.getLoyaltyCardCursor(mDatabase, DBHelper.LoyaltyCardArchiveFilter.All)
        mAdapter = LoyaltyCardCursorAdapter(this, cardCursor, this, null)
        binding.list.setAdapter(mAdapter)
    }
    
    override fun onResume() {
        super.onResume()
        
        val layoutManager = binding.list.layoutManager as GridLayoutManager?
        layoutManager?.setSpanCount(Settings(this).getPreferredColumnCount())
    }
    
    private fun onClickAction(position: Int) {
        val selected = DBHelper.getLoyaltyCardCursor(mDatabase, DBHelper.LoyaltyCardArchiveFilter.All)
        selected.moveToPosition(position)
        val loyaltyCard = LoyaltyCard.fromCursor(this, selected)
        
        Log.d(TAG, "Creating shortcut for card ${loyaltyCard.store}, ${loyaltyCard.id}")
        
        val shortcut = ShortcutHelper.createShortcutBuilder(this, loyaltyCard).build()
        
        setResult(RESULT_OK,
                  ShortcutManagerCompat.createShortcutResultIntent(this, shortcut))
        
        finish()
    }
    
    override fun onCreateMenu(inputMenu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.card_details_menu, inputMenu)
    }
    
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == R.id.action_display_options) {
            mAdapter.showDisplayOptionsDialog()
        }
        return true
    }
    
    override fun onRowClicked(inputPosition: Int) {
        onClickAction(inputPosition)
    }
    
    override fun onRowLongClicked(inputPosition: Int) {
        // do nothing
    }
}