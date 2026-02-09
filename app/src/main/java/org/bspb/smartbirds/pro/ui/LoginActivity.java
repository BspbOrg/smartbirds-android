package org.bspb.smartbirds.pro.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.backend.LoginResultEvent;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LoginStateEvent;
import org.bspb.smartbirds.pro.events.UserDataEvent;
import org.bspb.smartbirds.pro.prefs.UserPrefs;
import org.bspb.smartbirds.pro.sync.AuthenticationManager;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = SmartBirdsApplication.TAG + ".LoginActivity";

    // UI references.
    EditText mEmailView;
    EditText mPasswordView;
    View mProgressView;
    View mLoginFormView;
    View mGdprPanel;
    CheckBox mGdprConsent;
    TextView mGdprInfo;
    TextView mError;

    EEventBus bus = EEventBus.getInstance();
    UserPrefs prefs;

    private boolean isLoginRunning;
    private boolean missingGdpr = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        prefs = new UserPrefs(this);
        setContentView(R.layout.activity_login);
        setupWindowInsets();
        initViews();
    }

    /**
     * Setup window insets to handle edge-to-edge display.
     * Adds system bar insets to existing padding to prevent content overlap.
     */
    private void setupWindowInsets() {
        View loginRoot = findViewById(R.id.login_root);
        ViewCompat.setOnApplyWindowInsetsListener(loginRoot, (view, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Add system bar insets to existing padding
            int existingPaddingLeft = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            int existingPaddingTop = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
            int existingPaddingRight = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
            int existingPaddingBottom = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);

            view.setPadding(
                    existingPaddingLeft + insets.left,
                    existingPaddingTop + insets.top,
                    existingPaddingRight + insets.right,
                    existingPaddingBottom + insets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initViews() {
        mEmailView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        mProgressView = findViewById(R.id.login_progress);
        mLoginFormView = findViewById(R.id.login_form);
        mGdprPanel = findViewById(R.id.gdpr_panel);
        mGdprConsent = findViewById(R.id.gdpr_consent);
        mGdprInfo = findViewById(R.id.gdpr_info);
        mError = findViewById(R.id.login_error);

        findViewById(R.id.register_button).setOnClickListener((view) -> register());
        findViewById(R.id.gdpr_info).setOnClickListener((view) -> showGdprInfo());
        findViewById(R.id.email_sign_in_button).setOnClickListener((view) -> attemptLogin());
        mPasswordView.setOnEditorActionListener((textView, actionId, event) -> {
            attemptLogin();
            return true;
        });

        if (!TextUtils.isEmpty(prefs.getUsername())) {
            mEmailView.setText(prefs.getUsername());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bus.registerSticky(this);
    }

    @Override
    protected void onStop() {
        bus.unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        bus.removeStickyEvent(LoginResultEvent.class);
        super.onDestroy();
    }


    protected void register() {
        String registerUrl = getString(R.string.register_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(registerUrl)));
    }

    protected void showGdprInfo() {
        String gdprUrl = getString(R.string.gdpr_info_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(gdprUrl)));
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    protected void attemptLogin() {
        if (isLoginRunning) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mError.setText("");
        mError.setVisibility(View.GONE);
        bus.removeStickyEvent(LoginResultEvent.class);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        Boolean gdprConsent = null;
        if (missingGdpr) {
            gdprConsent = mGdprConsent.isChecked();
        }


        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for gdpr consent
        if (missingGdpr) {
            if (!gdprConsent) {
                mGdprConsent.setError(getString(R.string.error_missing_gdpr_consent));
                focusView = mGdprConsent;
                cancel = true;
            }
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            AuthenticationManager authManager = new AuthenticationManager(this);
            authManager.login(email, password, gdprConsent);
        }
    }


    private boolean isEmailValid(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void onEventMainThread(LoginStateEvent loginRunning) {
        Log.d(TAG, String.format("login running: %s", loginRunning));
        showProgress(isLoginRunning = loginRunning.isRunning());
    }

    public void onEventMainThread(LoginResultEvent loginResult) {
        Log.d(TAG, String.format("login result: %s", loginResult));
        mGdprPanel.setVisibility(View.GONE);
        mError.setVisibility(View.GONE);
        mError.setText("");
        missingGdpr = false;
        switch (loginResult.status) {
            case SUCCESS:
                bus.postSticky(new UserDataEvent(loginResult.user));
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            case CONNECTIVITY:
                mEmailView.setError("connectivity error");
                mEmailView.requestFocus();
                break;
            case PASSWORD_SHORT:
                mPasswordView.setError(getString(R.string.error_invalid_password));
                mPasswordView.requestFocus();
                break;
            case BAD_PASSWORD:
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
                break;
            case MISSING_GDPR:
                missingGdpr = true;
                mGdprPanel.setVisibility(View.VISIBLE);
                mError.setVisibility(View.VISIBLE);
                mError.setText(loginResult.message);
                break;
            case ERROR:
                if (loginResult.message != null) {
                    mError.setVisibility(View.VISIBLE);
                    mError.setText(loginResult.message);
                }
                break;
        }
    }
}
