package protect.card_locker;


import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

public class ManageGroupActivity extends CatimaAppCompatActivity implements ManageGroupCursorAdapter.CardAdapterListener {

    private final DBHelper mDB = new DBHelper(this);
    private ManageGroupCursorAdapter mAdapter;

    private final String SAVE_INSTANCE_ADAPTER_STATE = "adapterState";
    private final String SAVE_INSTANCE_CURRENT_GROUP_NAME = "currentGroupName";

    protected Group mGroup = null;
    private RecyclerView mCardList;
    private TextView mHelpText;
    private EditText mGroupNameText;

    private boolean mGroupNameNotInUse;

    @Override
    protected void onCreate(Bundle inputSavedInstanceState) {
        super.onCreate(inputSavedInstanceState);
        setContentView(R.layout.activity_manage_group);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelpText = findViewById(R.id.helpText);
        mCardList = findViewById(R.id.list);
        FloatingActionButton saveButton = findViewById(R.id.fabSave);

        mGroupNameText = findViewById(R.id.editTextGroupName);

        mGroupNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mGroupNameNotInUse = true;
                mGroupNameText.setError(null);
                String currentGroupName = mGroupNameText.getText().toString().trim();
                if (currentGroupName.length() == 0) {
                    mGroupNameText.setError(getResources().getText(R.string.group_name_is_empty));
                    return;
                }
                if (!mGroup._id.equals(currentGroupName)) {
                    if (mDB.getGroup(currentGroupName) != null) {
                        mGroupNameNotInUse = false;
                        mGroupNameText.setError(getResources().getText(R.string.group_name_already_in_use));
                    } else {
                        mGroupNameNotInUse = true;
                    }
                }
            }
        });

        Intent intent = getIntent();
        String groupId = intent.getStringExtra("group");
        if (groupId == null) {
            throw (new IllegalArgumentException("this activity expects a group loaded into it's intent"));
        }
        Log.d("groupId", "groupId: " + groupId);
        mGroup = mDB.getGroup(groupId);
        if (mGroup == null) {
            throw (new IllegalArgumentException("cannot load group " + groupId + " from database"));
        }
        mGroupNameText.setText(mGroup._id);
        setTitle(getString(R.string.editGroup, mGroup._id));
        mAdapter = new ManageGroupCursorAdapter(this, null, this, mGroup);
        mCardList.setAdapter(mAdapter);
        registerForContextMenu(mCardList);

        if (inputSavedInstanceState != null) {
            mAdapter.importInGroupState(integerArrayToAdapterState(inputSavedInstanceState.getIntegerArrayList(SAVE_INSTANCE_ADAPTER_STATE)));
            mGroupNameText.setText(inputSavedInstanceState.getString(SAVE_INSTANCE_CURRENT_GROUP_NAME));
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw (new RuntimeException("mActionBar is not expected to be null here"));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        saveButton.setOnClickListener(v -> {
            String currentGroupName = mGroupNameText.getText().toString().trim();
            if (!currentGroupName.equals(mGroup._id)) {
                if (currentGroupName.length() == 0) {
                    Toast.makeText(getApplicationContext(), R.string.group_name_is_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!mGroupNameNotInUse) {
                    Toast.makeText(getApplicationContext(), R.string.group_name_already_in_use, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            mAdapter.commitToDatabase();
            if (!currentGroupName.equals(mGroup._id)) {
                mDB.updateGroup(mGroup._id, currentGroupName);
            }
            Toast.makeText(getApplicationContext(), R.string.group_updated, Toast.LENGTH_SHORT).show();
            finish();
        });
        // this setText is here because content_main.xml is reused from main activity
        mHelpText.setText(getResources().getText(R.string.noGiftCardsGroup));
        updateLoyaltyCardList();
    }

    private ArrayList<Integer> adapterStateToIntegerArray(HashMap<Integer, Boolean> adapterState) {
        ArrayList<Integer> ret = new ArrayList<>(adapterState.size() * 2);
        for (Map.Entry<Integer, Boolean> entry : adapterState.entrySet()) {
            ret.add(entry.getKey());
            ret.add(entry.getValue() ? 1 : 0);
        }
        return ret;
    }

    private HashMap<Integer, Boolean> integerArrayToAdapterState(ArrayList<Integer> in) {
        HashMap<Integer, Boolean> ret = new HashMap<>();
        if (in.size() % 2 != 0) {
            throw (new RuntimeException("failed restoring adapterState from integer array list"));
        }
        for (int i = 0; i < in.size(); i += 2) {
            ret.put(in.get(i), in.get(i + 1) == 1);
        }
        return ret;
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putIntegerArrayList(SAVE_INSTANCE_ADAPTER_STATE, adapterStateToIntegerArray(mAdapter.exportInGroupState()));
        outState.putString(SAVE_INSTANCE_CURRENT_GROUP_NAME, mGroupNameText.getText().toString());
    }

    private void updateLoyaltyCardList() {
        mAdapter.swapCursor(mDB.getLoyaltyCardCursor());

        if (mAdapter.getCountFromCursor() == 0) {
            mCardList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);
        } else {
            mCardList.setVisibility(View.VISIBLE);
            mHelpText.setVisibility(View.GONE);
        }
    }

    private void leaveWithoutSaving() {
        if (hasChanged()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ManageGroupActivity.this);
            builder.setTitle(R.string.leaveWithoutSaveTitle);
            builder.setMessage(R.string.leaveWithoutSaveConfirmation);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> finish());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        leaveWithoutSaving();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean hasChanged() {
        return mAdapter.hasChanged() || !mGroup._id.equals(mGroupNameText.getText().toString().trim());
    }

    @Override
    public void onRowLongClicked(int inputPosition) {
        // do nothing for now
    }

    @Override
    public void onRowClicked(int inputPosition) {
        mAdapter.toggleSelection(inputPosition);

    }
}
