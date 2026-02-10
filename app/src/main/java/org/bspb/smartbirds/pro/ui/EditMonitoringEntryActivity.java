package org.bspb.smartbirds.pro.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.EntrySubmitted;
import org.bspb.smartbirds.pro.service.DataService;

/**
 * Created by groupsky on 16.03.17.
 */

public class EditMonitoringEntryActivity extends BaseActivity {

    public static final String EXTRA_ENTRY_ID = "entryId";
    public static final String EXTRA_ENTRY_TYPE = "entryType";

    long entryId;
    EntryType entryType;

    Fragment fragment;

    EEventBus eventBus = EEventBus.getInstance();

    @Override
    protected int getInsetContainerId() {
        return R.id.container;
    }

    public static Intent newIntent(Context context, long entryId, EntryType entryType) {
        Intent intent = new Intent(context, EditMonitoringEntryActivity.class);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_ENTRY_TYPE, entryType);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(DataService.Companion.intent(this));
        setContentView(R.layout.activity_form);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(EXTRA_ENTRY_ID)) {
                entryId = extras.getLong(EXTRA_ENTRY_ID);
            }
            if (extras.containsKey(EXTRA_ENTRY_TYPE)) {
                entryType = (EntryType) extras.getSerializable(EXTRA_ENTRY_TYPE);
            }
        }

        setupFragment();
    }

    protected void setupFragment() {
        fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (entryId <= 0 || entryType == null) {
            this.finish();
            return;
        }
        if (fragment == null) {
            fragment = entryType.loadFragment(entryId);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            return confirmCancel();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!confirmCancel()) super.onBackPressed();
    }

    private boolean confirmCancel() {
        if (fragment == null) return false;
        if (!(fragment instanceof EntryType.EntryFragment)) return false;
        if (!((EntryType.EntryFragment) fragment).isDirty()) return false;

        //Ask the user if they want to quit
        new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_alert)
                .setTitle(R.string.cancel_edit_entry)
                .setMessage(R.string.really_cancel_edit_entry)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
        return true;
    }

    @Override
    protected void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }

    public void onEvent(EntrySubmitted event) {
        setResult(RESULT_OK);
        finish();
    }
}
