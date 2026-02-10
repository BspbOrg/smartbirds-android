package org.bspb.smartbirds.pro.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.service.DataService;
import org.bspb.smartbirds.pro.ui.fragment.CurrentMonitoringCommonFormFragment;

/**
 * Created by groupsky on 14-10-21.
 */
public class EditCurrentCommonFormActivity extends BaseActivity {

    private static final String TAG = SmartBirdsApplication.TAG + ".StartMonitoring";
    public static final String EXTRA_IS_FINISHING = "isFinishing";

    CurrentMonitoringCommonFormFragment formFragment;
    boolean isFinishing = false;

    @Override
    protected int getInsetContainerId() {
        return R.id.container;
    }

    public static Intent intent(Context context) {
        return intent(context, false);
    }

    public static Intent intent(Context context, boolean isFinishing) {
        Intent intent = new Intent(context, EditCurrentCommonFormActivity.class);
        intent.putExtra(EXTRA_IS_FINISHING, isFinishing);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            isFinishing = extras.getBoolean(EXTRA_IS_FINISHING, false);
        }

        startService(DataService.Companion.intent(this));
        setContentView(R.layout.activity_start_monitoring);
        createFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.monitoring_edit_common_form, menu);
        {
            MenuItem menuSubmit = menu.findItem(R.id.action_submit);
            setMenuSubmit(menuSubmit);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_submit) {
            save();
            return true;
        }
        if (itemId == android.R.id.home) {
            cancel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setMenuSubmit(MenuItem menuSubmit) {
        if (isFinishing) {
            menuSubmit.setTitle(R.string.menu_monitoring_finish);
        } else {
            menuSubmit.setTitle(R.string.menu_monitoring_save);
        }
    }

    void createFragment() {
        formFragment = (CurrentMonitoringCommonFormFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        if (formFragment == null) {
            formFragment = CurrentMonitoringCommonFormFragment.Companion.newInstance(isFinishing);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, formFragment)
                    .commit();
        }

        if (isFinishing) {
            setTitle(R.string.title_activity_finish);
        } else {
            setTitle(R.string.title_activity_edit);
        }
    }

    void save() {
        if (formFragment.validate()) {
            formFragment.save();
            setResult(RESULT_OK);
            finish();
        }
    }

    void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

}
