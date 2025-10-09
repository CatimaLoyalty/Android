package protect.card_locker

import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import protect.card_locker.GroupCursorAdapter.GroupAdapterListener
import protect.card_locker.databinding.ManageGroupsActivityBinding

class ManageGroupsActivity : CatimaAppCompatActivity(), GroupAdapterListener {
    private lateinit var binding: ManageGroupsActivityBinding
    private lateinit var mDatabase: SQLiteDatabase
    private lateinit var mHelpText: TextView
    private lateinit var mGroupList: RecyclerView
    private lateinit var mAdapter: GroupCursorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ManageGroupsActivityBinding.inflate(layoutInflater)
        setTitle(R.string.groups)
        setContentView(binding.root)
        Utils.applyWindowInsets(binding.root)
        setSupportActionBar(binding.toolbar)
        enableToolbarBackButton()

        mDatabase = DBHelper(this).writableDatabase
    }

    override fun onResume() {
        super.onResume()

        with(binding.fabAdd) {
            setOnClickListener { v: View ->
                createGroup()
            }
            bringToFront()
        }

        mGroupList = binding.include.list
        mHelpText = binding.include.helpText

        // Init group list
        LinearLayoutManager(applicationContext).apply {
            mGroupList.layoutManager = this
        }
        mGroupList.setItemAnimator(DefaultItemAnimator())
        mAdapter = GroupCursorAdapter(this, null, this)
        mGroupList.setAdapter(mAdapter)

        updateGroupList()
    }

    private fun updateGroupList() {
        mAdapter.swapCursor(DBHelper.getGroupCursor(mDatabase))

        if (DBHelper.getGroupCount(mDatabase) == 0) {
            mGroupList.visibility = View.GONE
            mHelpText.visibility = View.VISIBLE

            return
        }

        mGroupList.visibility = View.VISIBLE
        mHelpText.visibility = View.GONE
    }

    private fun invalidateHomescreenActiveTab() {
        val activeTabPref = getSharedPreferences(
            getString(R.string.sharedpreference_active_tab),
            MODE_PRIVATE
        )
        activeTabPref.edit {
            putInt(getString(R.string.sharedpreference_active_tab), 0)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun createGroup() {
        val builder: AlertDialog.Builder = MaterialAlertDialogBuilder(this)

        // Header
        builder.setTitle(R.string.enter_group_name)

        // Layout
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val contentPadding =
                resources.getDimensionPixelSize(R.dimen.alert_dialog_content_padding)
            leftMargin = contentPadding
            topMargin = contentPadding / 2
            rightMargin = contentPadding
        }

        // EditText with spacing
        val input = EditText(this)
        input.setInputType(InputType.TYPE_CLASS_TEXT)
        input.setLayoutParams(params)
        layout.addView(input)

        // Set layout
        builder.setView(layout)

        // Buttons
        builder.setPositiveButton(getString(R.string.ok)) { dialog: DialogInterface, which: Int ->
            DBHelper.insertGroup(mDatabase, input.text.trim().toString())
            updateGroupList()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, which: Int ->
            dialog.cancel()
        }
        val dialog = builder.create()

        // Now that the dialog exists, we can bind something that affects the OK button
        input.doOnTextChanged { s: CharSequence?, start: Int, before: Int, count: Int ->
            val groupName = s?.trim().toString()

            if (groupName.isEmpty()) {
                input.error = getString(R.string.group_name_is_empty)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false)
                return@doOnTextChanged
            }

            if (DBHelper.getGroup(mDatabase, groupName) != null) {
                input.error = getString(R.string.group_name_already_in_use)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false)
                return@doOnTextChanged
            }

            input.error = null
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true)
        }

        dialog.apply {
            show()
            // Disable button (must be done **after** dialog is shown to prevent crash
            getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false)
            // Set focus on input field
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }

        input.requestFocus()
    }

    private fun getGroupName(view: View): String {
        val groupNameTextView = view.findViewById<TextView>(R.id.name)
        return groupNameTextView.text.toString()
    }

    private fun moveGroup(view: View, up: Boolean) {
        val groups = DBHelper.getGroups(mDatabase)
        val groupName = getGroupName(view)

        val currentIndex = DBHelper.getGroup(mDatabase, groupName).order

        // Reinsert group in correct position
        val newIndex: Int = if (up) {
            currentIndex - 1
        } else {
            currentIndex + 1
        }

        // Don't try to move out of bounds
        if (newIndex < 0 || newIndex >= groups.size) {
            return
        }

        val group = groups.removeAt(currentIndex)
        groups.add(newIndex, group)

        // Update database
        DBHelper.reorderGroups(mDatabase, groups)

        // Update UI
        updateGroupList()

        // Ordering may have changed, so invalidate
        invalidateHomescreenActiveTab()
    }

    override fun onMoveDownButtonClicked(view: View) {
        moveGroup(view, false)
    }

    override fun onMoveUpButtonClicked(view: View) {
        moveGroup(view, true)
    }

    override fun onEditButtonClicked(view: View) {
        Intent(this, ManageGroupActivity::class.java).apply {
            putExtra("group", getGroupName(view))
            startActivity(this)
        }
    }

    override fun onDeleteButtonClicked(view: View) {
        val groupName = getGroupName(view)

        MaterialAlertDialogBuilder(this).apply {
            setTitle(R.string.deleteConfirmationGroup)
            setMessage(groupName)

            setPositiveButton(getString(R.string.ok)) { dialog: DialogInterface, which: Int ->
                DBHelper.deleteGroup(mDatabase, groupName)
                updateGroupList()
                // Delete may change ordering, so invalidate
                invalidateHomescreenActiveTab()
            }
            setNegativeButton(getString(R.string.cancel)) { dialog: DialogInterface, which: Int ->
                dialog.cancel()
            }
        }.create().show()
    }
}
