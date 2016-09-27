package org.bspb.smartbirds.pro.ui.fragment;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.LongClick;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.bspb.smartbirds.pro.R;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.ExportFailedEvent;
import org.bspb.smartbirds.pro.events.ExportPreparedEvent;
import org.bspb.smartbirds.pro.events.StartMonitoringEvent;
import org.bspb.smartbirds.pro.events.StartingUpload;
import org.bspb.smartbirds.pro.events.UploadCompleted;
import org.bspb.smartbirds.pro.service.ExportService_;
import org.bspb.smartbirds.pro.service.NomenclatureService_;
import org.bspb.smartbirds.pro.service.UploadService_;

import java.io.File;
import java.util.Date;

@EFragment(R.layout.fragment_main)
public class MainFragment extends Fragment {

    @Bean
    EEventBus bus;

    private AlertDialog uploadingDialog;
    private AlertDialog exportDialog;

    @ViewById(R.id.not_synced_count)
    TextView notSyncedCountView;

    @Override
    public void onStart() {
        super.onStart();
        bus.register(this);
    }

    @Override
    public void onStop() {
        bus.unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        showNotSyncedCount();
    }

    @Click(R.id.btn_start_birds)
    void startBirdsClicked() {
        bus.postSticky(new StartMonitoringEvent());
    }

    @Click(R.id.btn_upload)
    void uploadBtnClicked() {
        UploadService_.intent(getActivity()).uploadAll().start();
    }

    @Click(R.id.btn_export)
    void exportBtnClicked() {
        exportDialog = ProgressDialog.show(getActivity(), getString(R.string.export_dialog_title), getString(R.string.export_dialog_text), true);
        ExportService_.intent(getActivity()).prepareForExport().start();
    }

    @Click(R.id.btn_info)
    void infoBtnClicked() {
        final float density = getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.info_dialog_title));
        TextView view = new TextView(getActivity());
        view.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        view.setText(Html.fromHtml(getString(R.string.info_text), new Html.ImageGetter() {
            @Override
            public Drawable getDrawable(String s) {
                Drawable drawable = null;
                if ("logo_bspb".equals(s)) {
                    drawable = getResources().getDrawable(R.drawable.logo_bspb);
                } else if ("logo_mtel".equals(s)) {
                    drawable = getResources().getDrawable(R.drawable.logo_mtel);
                }
                drawable.setBounds(0, 0, (int) (drawable.getIntrinsicWidth() * density),
                        (int) (drawable.getIntrinsicHeight() * density));
                return drawable;

            }
        }, null));
        builder.setView(view);
        builder.create().show();
    }

    @Click(R.id.btn_help)
    void helpBtnClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(R.string.help_url)));
        startActivity(intent);
    }

    @UiThread
    public void onEvent(StartingUpload event) {
        uploadingDialog = ProgressDialog.show(getActivity(), getString(R.string.upload_dialog_title), getString(R.string.upload_dialog_text), true);
    }

    @UiThread
    public void onEvent(UploadCompleted event) {
        if (uploadingDialog != null) {
            uploadingDialog.cancel();
        }
        showNotSyncedCount();
    }

    @UiThread
    public void onEvent(ExportPreparedEvent event) {
        if (exportDialog != null) {
            exportDialog.cancel();
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_subject));
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.export_text, new Date().toString()));
        intent.putExtra(Intent.EXTRA_STREAM, event.uri);
        startActivity(Intent.createChooser(intent, getString(R.string.export_app_chooser)));
    }

    @UiThread
    public void onEvent(ExportFailedEvent event) {
        if (exportDialog != null) {
            exportDialog.cancel();
        }
        Toast.makeText(getActivity(), getString(R.string.export_failed_error), Toast.LENGTH_LONG).
                show();
    }

    @LongClick(R.id.btn_export)
    public boolean displayDescription(View v) {
        if (TextUtils.isEmpty(v.getContentDescription())) return false;
        final int[] screenPos = new int[2];
        final Rect displayFrame = new Rect();
        v.getLocationOnScreen(screenPos);
        v.getWindowVisibleDisplayFrame(displayFrame);

        final Context context = v.getContext();
        final int width = v.getWidth();
        final int height = v.getHeight();
        final int midy = screenPos[1] + height / 2;
        int referenceX = screenPos[0] + width / 2;
        if (ViewCompat.getLayoutDirection(v) == ViewCompat.LAYOUT_DIRECTION_LTR) {
            final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            referenceX = screenWidth - referenceX; // mirror
        }
        Toast cheatSheet = Toast.makeText(context, v.getContentDescription(), Toast.LENGTH_SHORT);
        if (midy < displayFrame.height()) {
            // Show along the top; follow action buttons
            cheatSheet.setGravity(Gravity.TOP | GravityCompat.END, referenceX, height);
        } else {
            // Show along the bottom center
            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
        }
        cheatSheet.show();
        return true;
    }

    protected void showNotSyncedCount() {
        File baseDir = getActivity().getExternalFilesDir(null);
        int notSyncedCount = 0;
        for (String monitoring : baseDir.list()) {
            if (monitoring.endsWith("-up")) {
                notSyncedCount++;
            }
        }
        notSyncedCountView.setText(getString(R.string.not_synced_count, notSyncedCount));
    }
}
