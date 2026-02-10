package org.bspb.smartbirds.pro.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.ui.fragment.StatsFragment;

public class StatsActivity extends BaseActivity {

    @Override
    protected int getInsetContainerId() {
        return R.id.container;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);
        createFragment();
    }

    void createFragment() {
        if (getSupportFragmentManager().findFragmentById(R.id.container) == null)
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new StatsFragment())
                    .commit();
    }
}
