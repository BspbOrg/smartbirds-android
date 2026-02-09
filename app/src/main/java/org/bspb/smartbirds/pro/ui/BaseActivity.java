package org.bspb.smartbirds.pro.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.bspb.smartbirds.pro.service.DataService;

/**
 * This is the parent for all activities.
 * Each new activity should extend this class instead of API Activity class.
 * <p>
 * Created by Ilian Georgiev.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        setupWindowInsets();
    }

    /**
     * Return the resource ID of the container view that should receive window insets.
     * Return 0 to disable automatic inset handling (for custom implementations).
     *
     * @return The resource ID of the container view, or 0 to skip automatic handling
     */
    protected abstract int getInsetContainerId();

    /**
     * Setup window insets for edge-to-edge display.
     * Override this method for custom inset handling (e.g., multiple containers).
     */
    protected void setupWindowInsets() {
        int containerId = getInsetContainerId();
        if (containerId == 0) {
            return; // Skip automatic handling
        }

        View container = findViewById(containerId);
        if (container != null) {
            applyWindowInsets(container);
        }
    }

    /**
     * Apply window insets as padding to the given view.
     * Override for custom inset application logic (e.g., preserving margins).
     *
     * @param container The view to apply insets to
     */
    protected void applyWindowInsets(View container) {
        ViewCompat.setOnApplyWindowInsetsListener(container, (view, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(DataService.Companion.intent(this), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(mConnection);
    }
}
