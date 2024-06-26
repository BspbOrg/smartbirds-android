package org.bspb.smartbirds.pro.ui;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.events.CancelMonitoringEvent;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.MonitoringStartedEvent;
import org.bspb.smartbirds.pro.events.StartMonitoringEvent;
import org.bspb.smartbirds.pro.service.DataService;
import org.bspb.smartbirds.pro.ui.fragment.CurrentMonitoringCommonFormFragment;
import org.bspb.smartbirds.pro.ui.fragment.CurrentMonitoringCommonFormFragment_;

@EActivity(R.layout.activity_start_monitoring)
@OptionsMenu(R.menu.monitoring_common_form)
public class StartMonitoringActivity extends BaseActivity {

    private static final String TAG = SmartBirdsApplication.TAG + ".StartMonitoring";

    @Bean
    EEventBus bus;
    CurrentMonitoringCommonFormFragment formFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataService.Companion.intent(this).start();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cancelMonitoring();
            }
        });
    }

    @AfterViews
    void createFragment() {
        formFragment = (CurrentMonitoringCommonFormFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        if (formFragment == null) {
            formFragment = CurrentMonitoringCommonFormFragment_.builder().build();
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

    @OptionsItem(android.R.id.home)
    void cancelMonitoring() {
        Log.d(TAG, "cancelMonitoring");
        bus.removeStickyEvent(StartMonitoringEvent.class);
        bus.removeStickyEvent(MonitoringStartedEvent.class);
        bus.post(new CancelMonitoringEvent());
        if (!isFinishing())
            finish();
    }

    @OptionsItem(R.id.action_submit)
    public void save() {
        if (formFragment.validate()) {
            formFragment.save();
            MonitoringActivity_.intent(this).start();
            finish();
        }
    }
}
