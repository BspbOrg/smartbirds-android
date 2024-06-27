package org.bspb.smartbirds.pro.ui;

import android.content.Intent;
import android.os.Bundle;

import org.bspb.smartbirds.pro.BuildConfig;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LogoutEvent;
import org.bspb.smartbirds.pro.events.ResumeMonitoringEvent;
import org.bspb.smartbirds.pro.events.StartMonitoringEvent;
import org.bspb.smartbirds.pro.prefs.SmartBirdsPrefs;
import org.bspb.smartbirds.pro.prefs.UserPrefs_;
import org.bspb.smartbirds.pro.service.SyncService;
import org.bspb.smartbirds.pro.service.SyncService_;
import org.bspb.smartbirds.pro.ui.fragment.MainFragment_;


public class MainActivity extends BaseActivity {

    EEventBus bus = EEventBus.getInstance();
    UserPrefs_ prefs;
    SmartBirdsPrefs globalPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new UserPrefs_(this);
        globalPrefs = new SmartBirdsPrefs(this);

        createFragment();

        requireAuthentication();
        if (!isFinishing()) {
            if (globalPrefs.getRunningMonitoring()) {
                startActivity(MonitoringActivity.newIntent(this));
            } else if (globalPrefs.getVersionCode() != BuildConfig.VERSION_CODE) {
                // Sync data if app is updated
                if (!SyncService.Companion.isWorking()) {
                    SyncService_.intent(this).initialSync().start();
                }
                globalPrefs.setVersionCode(BuildConfig.VERSION_CODE);
            }
        }
    }

    void createFragment() {
        if (getSupportFragmentManager().findFragmentById(R.id.container) == null)
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, MainFragment_.builder().build())
                    .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requireAuthentication();
        bus.register(this);
    }

    @Override
    protected void onStop() {
        bus.unregister(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        requireAuthentication();
    }

    public void onEvent(StartMonitoringEvent event) {
        startActivity(new Intent(this, StartMonitoringActivity.class));
    }

    public void onEvent(ResumeMonitoringEvent event) {
        startActivity(MonitoringActivity.newIntent(this));
    }

    public void onEvent(LogoutEvent event) {
        requireAuthentication();
    }

    protected void requireAuthentication() {
        if (!prefs.isAuthenticated().get() && !isFinishing()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

}
