package protect.card_locker

import android.content.DialogInterface
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.LoyaltyCardCursorAdapter.CardAdapterListener
import protect.card_locker.databinding.ActivityManageGroupBinding

class ManageGroupActivity : CatimaAppCompatActivity(), CardAdapterListener {
    private lateinit var binding: ActivityManageGroupBinding
    private lateinit var mDatabase: SQLiteDatabase
    private lateinit var mAdapter: ManageGroupCursorAdapter

    private lateinit var mGroup: Group
    private lateinit var mCardList: RecyclerView
    private lateinit var noGroupCardsText: TextView
    private lateinit var mGroupNameText: EditText

    private var mGroupNameNotInUse = false

    override fun onCreate(inputSavedInstanceState: Bundle?) {
        super.onCreate(inputSavedInstanceState)
        binding = ActivityManageGroupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Utils.applyWindowInsetsAndFabOffset(binding.getRoot(), binding.fabSave)
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        mDatabase = DBHelper(this).writableDatabase
        noGroupCardsText = binding.include.noGroupCardsText
        mCardList = binding.include.list

        val saveButton = binding.fabSave
        mGroupNameText = binding.editTextGroupName

        mGroupNameText.doAfterTextChanged {
            mGroupNameNotInUse = true
            mGroupNameText.error = null
            val currentGroupName = mGroupNameText.getText().toString().trim { it <= ' ' }
            if (currentGroupName.isEmpty()) {
                mGroupNameText.error = getResources().getText(R.string.group_name_is_empty)
                return@doAfterTextChanged
            }
            if (mGroup._id != currentGroupName) {
                if (DBHelper.getGroup(mDatabase, currentGroupName) != null) {
                    mGroupNameNotInUse = false
                    mGroupNameText.error =
                        getResources().getText(R.string.group_name_already_in_use)
                } else {
                    mGroupNameNotInUse = true
                }
            }
        }

        val intent = getIntent()
        val groupId = intent.getStringExtra("group")
            ?: throw (IllegalArgumentException("this activity expects a group loaded into it's intent"))
        Log.d("groupId", "groupId: $groupId")
        mGroup = DBHelper.getGroup(mDatabase, groupId)
        mGroupNameText.setText(mGroup._id)
        setTitle(getString(R.string.editGroup, mGroup._id))
        mAdapter = ManageGroupCursorAdapter(this, null, this, mGroup, null)
        mCardList.setAdapter(mAdapter)
        registerForContextMenu(mCardList)

        if (inputSavedInstanceState != null) {
            mAdapter.importInGroupState(
                integerArrayToAdapterState(
                    inputSavedInstanceState.getIntegerArrayList(SAVE_INSTANCE_ADAPTER_STATE)
                        .orEmpty()
                )
            )
            mGroupNameText.setText(
                inputSavedInstanceState.getString(
                    SAVE_INSTANCE_CURRENT_GROUP_NAME
                )
            )
        }

        enableToolbarBackButton()

        saveButton.setOnClickListener { v: View ->
            val currentGroupName = mGroupNameText.getText().toString().trim { it <= ' ' }
            if (currentGroupName != mGroup._id) {
                when {
                    currentGroupName.isEmpty() -> {
                        Toast.makeText(
                            applicationContext,
                            R.string.group_name_is_empty,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    !mGroupNameNotInUse -> {
                        Toast.makeText(
                            applicationContext,
                            R.string.group_name_already_in_use,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
            }

            mAdapter.commitToDatabase()
            if (currentGroupName != mGroup._id) {
                DBHelper.updateGroup(mDatabase, mGroup._id, currentGroupName)
            }
            Toast.makeText(applicationContext, R.string.group_updated, Toast.LENGTH_SHORT)
                .show()
            finish()
        }
        // this setText is here because content_main.xml is reused from main activity
        noGroupCardsText.text = getResources().getText(R.string.noGiftCardsGroup)
        updateLoyaltyCardList()

        onBackPressedDispatcher.addCallback(
            owner = this,
            onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveWithoutSaving()
                }
            })
    }

    private fun adapterStateToIntegerArray(adapterState: HashMap<Int, Boolean>): ArrayList<Int> {
        val ret = ArrayList<Int>(adapterState.size * 2)
        for (entry in adapterState.entries) {
            ret.add(entry.key)
            ret.add(if (entry.value) 1 else 0)
        }
        return ret
    }

    private fun integerArrayToAdapterState(list: List<Int>): Map<Int, Boolean> {
        val ret = mutableMapOf<Int, Boolean>()
        if (list.size % 2 != 0) {
            throw (RuntimeException("failed restoring adapterState from integer array list"))
        }
        var i = 0
        while (i < list.size) {
            ret.put(list[i], list[i + 1] == 1)
            i += 2
        }
        return ret
    }

    override fun onCreateOptionsMenu(inputMenu: Menu): Boolean {
        menuInflater.inflate(R.menu.card_details_menu, inputMenu)

        return super.onCreateOptionsMenu(inputMenu)
    }

    override fun onOptionsItemSelected(inputItem: MenuItem): Boolean {
        val id = inputItem.itemId

        if (id == R.id.action_display_options) {
            mAdapter.showDisplayOptionsDialog()
            invalidateOptionsMenu()

            return true
        }

        return super.onOptionsItemSelected(inputItem)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putIntegerArrayList(
            SAVE_INSTANCE_ADAPTER_STATE,
            adapterStateToIntegerArray(mAdapter.exportInGroupState())
        )
        outState.putString(SAVE_INSTANCE_CURRENT_GROUP_NAME, mGroupNameText.getText().toString())
    }

    private fun updateLoyaltyCardList() {
        mAdapter.swapCursor(DBHelper.getLoyaltyCardCursor(mDatabase))

        if (mAdapter.itemCount == 0) {
            mCardList.visibility = View.GONE
            noGroupCardsText.visibility = View.VISIBLE
        } else {
            mCardList.visibility = View.VISIBLE
            noGroupCardsText.visibility = View.GONE
        }
    }

    private fun leaveWithoutSaving() {
        if (hasChanged()) {
            MaterialAlertDialogBuilder(this@ManageGroupActivity).apply {
                setTitle(R.string.leaveWithoutSaveTitle)
                setMessage(R.string.leaveWithoutSaveConfirmation)
                setPositiveButton(R.string.confirm) { dialog: DialogInterface, _ ->
                    finish()
                }
                setNegativeButton(R.string.cancel) { dialog: DialogInterface, _ ->
                    dialog.dismiss()
                }
            }.create().show()
        } else {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun hasChanged(): Boolean {
        return mAdapter.hasChanged() || mGroup._id != mGroupNameText.getText().toString()
            .trim { it <= ' ' }
    }

    override fun onRowLongClicked(inputPosition: Int) {
        mAdapter.toggleSelection(inputPosition)
    }

    override fun onRowClicked(inputPosition: Int) {
        mAdapter.toggleSelection(inputPosition)
    }

    private companion object {

        const val SAVE_INSTANCE_ADAPTER_STATE = "adapterState"
        const val SAVE_INSTANCE_CURRENT_GROUP_NAME = "currentGroupName"
    }
}
