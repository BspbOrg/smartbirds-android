package org.bspb.smartbirds.pro.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.snackbar.Snackbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EditorAction;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.backend.Backend;
import org.bspb.smartbirds.pro.backend.LoginResultEvent;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.LoginStateEvent;
import org.bspb.smartbirds.pro.events.UserDataEvent;
import org.bspb.smartbirds.pro.prefs.UserPrefs_;
import org.bspb.smartbirds.pro.sync.AuthenticationManager;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static org.bspb.smartbirds.pro.tools.Reporting.logException;

/**
 * A login screen that offers login via email/password.
 */
@EActivity(R.layout.activity_login)
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    private static final String TAG = SmartBirdsApplication.TAG + ".LoginActivity";

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;

    // UI references.
    @ViewById(R.id.email)
    AutoCompleteTextView mEmailView;
    @ViewById(R.id.password)
    EditText mPasswordView;
    @ViewById(R.id.login_progress)
    View mProgressView;
    @ViewById(R.id.login_form)
    View mLoginFormView;
    @ViewById(R.id.gdpr_panel)
    View mGdprPanel;
    @ViewById(R.id.gdpr_consent)
    CheckBox mGdprConsent;
    @ViewById(R.id.gdpr_info)
    TextView mGdprInfo;
    @ViewById(R.id.login_error)
    TextView mError;

    @Bean
    Backend backend;
    @Bean
    EEventBus bus;
    @Pref
    UserPrefs_ prefs;
    @Bean
    AuthenticationManager authenticationManager;

    private boolean isLoginRunning;
    private boolean missingGdpr = false;

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

    @AfterViews
    void initLoginForm() {
        // Set up the login form.
        populateAutoComplete();

        if (prefs.username().exists()) {
            mEmailView.setText(prefs.username().get());
        }
    }


    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (ActivityCompat.checkSelfPermission(this, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            try {
                Snackbar.make(mEmailView, R.string.login_permission_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            @TargetApi(Build.VERSION_CODES.M)
                            public void onClick(View v) {
                                requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                            }
                        }).show();
                return false;
            } catch (Throwable t) {
                // we get IAE because we don't extend the Theme.AppCompat, but that messes up styling of the fields
                logException(t);
            }
        }
        requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        Toast.makeText(this, R.string.login_permission_rationale, Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
                return;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Click(R.id.register_button)
    protected void register() {
        String registerUrl = getString(R.string.register_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(registerUrl)));
    }

    @Click(R.id.gdpr_info)
    protected void showGdprInfo() {
        String gdprUrl = getString(R.string.gdpr_info_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(gdprUrl)));
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    @Click(R.id.email_sign_in_button)
    @EditorAction(R.id.password)
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
            authenticationManager.login(email, password, gdprConsent);
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
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (cursor == null) return;
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
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
                MainActivity_.intent(this).start();
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

