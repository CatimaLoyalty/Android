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

public class ManageGroupActivity extends CatimaAppCompatActivity implements ManageGroupCursorAdapter.CardAdapterListener
{

    private final DBHelper mDB = new DBHelper(this);
    private ManageGroupCursorAdapter mAdapter;

    protected Group mGroup = null;
    private RecyclerView mCardList;
    private TextView mHelpText;
    private EditText mGroupNameText;

    private HashMap<Integer, Boolean> mAdapterState;
    private String mCurrentGroupName;

    private boolean mGroupNameNotInUse;

    @Override
    protected void onCreate(Bundle inputSavedInstanceState)
    {
        super.onCreate(inputSavedInstanceState);
        setContentView(R.layout.activity_manage_group);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mHelpText = findViewById(R.id.helpText);
        mCardList = findViewById(R.id.list);
        FloatingActionButton saveButton = findViewById(R.id.fabSave);

        mGroupNameText = findViewById(R.id.editTextGroupName);

        Intent intent = getIntent();
        String groupId = intent.getStringExtra("group");
        if (groupId == null){
            throw(new IllegalArgumentException("this activity expects a group loaded into it's intent"));
        }
        Log.d("groupId", "groupId: " + groupId);
        mGroup = mDB.getGroup(groupId);
        if (mGroup == null){
            throw(new IllegalArgumentException("cannot load group " + groupId + " from database"));
        }
        mAdapter = new ManageGroupCursorAdapter(this, null, this, mGroup);
        mCardList.setAdapter(mAdapter);
        registerForContextMenu(mCardList);

        if (inputSavedInstanceState != null) {
            ArrayList<Integer> adapterState = inputSavedInstanceState.getIntegerArrayList("mAdapterState");
            if (adapterState != null) {
                integerArrayToAdapterState(adapterState);
            }
            mCurrentGroupName = inputSavedInstanceState.getString("mCurrentGroupName");
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null){
            throw(new RuntimeException("mActionBar is not expected to be null here"));
        }
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        saveButton.setOnClickListener(v -> {
            String currentGroupName = mGroupNameText.getText().toString();
            if(!currentGroupName.trim().equals(mGroup._id)){
                if(!mGroupNameNotInUse) {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.group_name_already_in_use, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if(currentGroupName.trim().length() == 0){
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.group_name_is_empty, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
            }

            mAdapter.commitToDatabase();
            Toast toast = Toast.makeText(getApplicationContext(), R.string.group_updated, Toast.LENGTH_SHORT);
            if(!currentGroupName.trim().equals(mGroup._id)){
                mDB.updateGroup(mGroup._id, currentGroupName.trim());
            }
            toast.show();
            finish();
        });
        mHelpText.setText(getResources().getText(R.string.noGiftCardsGroup));
    }

    private void checkIfGroupNameIsInUse(){
        mGroupNameNotInUse = false;
        String currentGroupName = mGroupNameText.getText().toString();
        if (!mGroup._id.equals(currentGroupName.trim())) {
            Group group = mDB.getGroup(currentGroupName.trim());
            if (group != null) {
                mGroupNameNotInUse = false;
                mGroupNameText.setError(getResources().getText(R.string.group_name_already_in_use));
            } else {
                mGroupNameNotInUse = true;
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        setTitle(getString(R.string.editGroup, mGroup._id));

        if (mCurrentGroupName == null){
            mGroupNameText.setText(mGroup._id);
        }else{
            mGroupNameText.setText(mCurrentGroupName);
            checkIfGroupNameIsInUse();
        }

        mGroupNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mGroupNameText.setError(null);
                checkIfGroupNameIsInUse();
            }
        });

        if (mAdapterState != null){
            mAdapter.importInGroupState(mAdapterState);
        }
        updateLoyaltyCardList();
    }

    private ArrayList<Integer> adapterStateToIntegerArray(){
        ArrayList<Integer> ret = new ArrayList<>(mAdapterState.size() * 2);
        for (Map.Entry<Integer, Boolean> entry : mAdapterState.entrySet()) {
            ret.add(entry.getKey());
            ret.add(entry.getValue()?1:0);
        }
        return ret;
    }

    private void integerArrayToAdapterState(ArrayList<Integer> in) {
        boolean isKey = true;
        int cardId = 0;
        mAdapterState = new HashMap<>();
        for (int value : in) {
            if (isKey) {
                cardId = value;
            } else {
                mAdapterState.put(cardId, value == 1);
            }
            isKey = !isKey;
        }
        if(!isKey){
            throw(new RuntimeException("failed restoring mAdapterState from integer array list"));
        }
    }


    @Override
    protected void onSaveInstanceState (@NonNull Bundle outState){
        super.onSaveInstanceState(outState);
        if(mAdapterState != null){
            outState.putIntegerArrayList("mAdapterState", adapterStateToIntegerArray());
        }
        if(mCurrentGroupName != null){
            outState.putString("mCurrentGroupName", mCurrentGroupName);
        }

    }

    @Override
    protected void onPause(){
        super.onPause();

        mAdapterState = mAdapter.exportInGroupState();
        mCurrentGroupName = mGroupNameText.getText().toString();
    }

    private void updateLoyaltyCardList() {
        mAdapter.swapCursor(mDB.getLoyaltyCardCursor("", null, DBHelper.LoyaltyCardOrder.Alpha, DBHelper.LoyaltyCardOrderDirection.Ascending));

        if(mAdapter.getCountFromCursor() == 0)
        {
            mCardList.setVisibility(View.GONE);
            mHelpText.setVisibility(View.VISIBLE);
        }else {
            mCardList.setVisibility(View.VISIBLE);
            mHelpText.setVisibility(View.GONE);
        }
    }

    private void leaveWithoutSaving(){
        if (hasChanged()){
            AlertDialog.Builder builder = new AlertDialog.Builder(ManageGroupActivity.this);
            builder.setTitle(R.string.leaveWithoutSaveTitle);
            builder.setMessage(R.string.leaveWithoutSaveConfirmation);
            builder.setPositiveButton(R.string.confirm, (dialog, which) -> finish());
            builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
            AlertDialog dialog = builder.create();
            dialog.show();
        }else{
            finish();
        }
    }

    @Override
    public void onBackPressed()
    {
        leaveWithoutSaving();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private boolean hasChanged(){
        return mAdapter.hasChanged() || !mGroup._id.equals(mGroupNameText.getText().toString().trim());
    }

    @Override
    public void onRowLongClicked(int inputPosition)
    {
        // do nothing for now
    }

    @Override
    public void onRowClicked(int inputPosition)
    {
        mAdapter.toggleSelection(inputPosition);

    }
}
