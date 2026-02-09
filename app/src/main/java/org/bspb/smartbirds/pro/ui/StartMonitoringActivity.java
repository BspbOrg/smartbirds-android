package org.bspb.smartbirds.pro.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.events.CancelMonitoringEvent;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.MonitoringStartedEvent;
import org.bspb.smartbirds.pro.events.StartMonitoringEvent;
import org.bspb.smartbirds.pro.service.DataService;
import org.bspb.smartbirds.pro.ui.fragment.CurrentMonitoringCommonFormFragment;

public class StartMonitoringActivity extends BaseActivity {

    private static final String TAG = SmartBirdsApplication.TAG + ".StartMonitoring";

    EEventBus bus = EEventBus.getInstance();
    CurrentMonitoringCommonFormFragment formFragment;

    @Override
    protected int getInsetContainerId() {
        return R.id.container;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_monitoring);
        startService(DataService.Companion.intent(this));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelMonitoring();
            }
        });

        createFragment();
    }

    void createFragment() {
        formFragment = (CurrentMonitoringCommonFormFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        if (formFragment == null) {
            formFragment = new CurrentMonitoringCommonFormFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, formFragment)
                    .commit();
        }
    }

    public void onEvent(MonitoringStartedEvent event) {
        Log.d(TAG, "onMonitoringStartedEvent");
        bus.removeStickyEvent(StartMonitoringEvent.class);
        bus.removeStickyEvent(MonitoringStartedEvent.class);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        bus.registerSticky(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        bus.unregister(this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.monitoring_common_form, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId_ = item.getItemId();
        if (itemId_ == android.R.id.home) {
            cancelMonitoring();
            return true;
        }
        if (itemId_ == R.id.action_submit) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void cancelMonitoring() {
        Log.d(TAG, "cancelMonitoring");
        bus.removeStickyEvent(StartMonitoringEvent.class);
        bus.removeStickyEvent(MonitoringStartedEvent.class);
        bus.post(new CancelMonitoringEvent());
        if (!isFinishing())
            finish();
    }

    public void save() {
        if (formFragment.validate()) {
            formFragment.save();
            startActivity(MonitoringActivity.newIntent(this));
            finish();
        }
    }
}
