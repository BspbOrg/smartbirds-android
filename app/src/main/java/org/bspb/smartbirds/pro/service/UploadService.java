package org.bspb.smartbirds.pro.service;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.googlecode.jcsv.CSVStrategy;
import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.ServiceAction;
import org.apache.commons.net.ftp.FTPClient;
import org.bspb.smartbirds.pro.BuildConfig;
import org.bspb.smartbirds.pro.SmartBirdsApplication;
import org.bspb.smartbirds.pro.backend.Backend;
import org.bspb.smartbirds.pro.backend.dto.FileId;
import org.bspb.smartbirds.pro.backend.dto.ResponseEnvelope;
import org.bspb.smartbirds.pro.content.MonitoringManager;
import org.bspb.smartbirds.pro.enums.EntryType;
import org.bspb.smartbirds.pro.events.EEventBus;
import org.bspb.smartbirds.pro.events.StartingUpload;
import org.bspb.smartbirds.pro.events.UploadCompleted;
import org.bspb.smartbirds.pro.forms.convert.Converter;
import org.bspb.smartbirds.pro.forms.upload.Uploader;
import org.bspb.smartbirds.pro.tools.SmartBirdsCSVEntryParser;
import org.bspb.smartbirds.pro.ui.utils.FTPClientUtils;
import org.bspb.smartbirds.pro.ui.utils.NomenclaturesBean;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

import static org.bspb.smartbirds.pro.content.Monitoring.Status.finished;
import static org.bspb.smartbirds.pro.content.Monitoring.Status.uploaded;
import static org.bspb.smartbirds.pro.tools.Reporting.logException;

@EIntentService
public class UploadService extends IntentService {

    private static final String TAG = SmartBirdsApplication.TAG + ".UploadService";
    public static boolean isUploading;

    @Bean
    EEventBus eventBus;
    @Bean
    Backend backend;

    @Bean
    NomenclaturesBean nomenclaturesBean;

    @Bean
    MonitoringManager monitoringManager;

    protected UploadService() {
        super("Upload Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @ServiceAction
    void uploadAll() {
        Log.d(TAG, "uploading all finished monitorings");
        isUploading = true;
        eventBus.post(new StartingUpload());
        try {
            File baseDir = getExternalFilesDir(null);
            for (String monitoringCode : monitoringManager.monitoringCodesForStatus(finished)) {
                File monitoringDir = new File(baseDir, monitoringCode);
                if (!monitoringDir.exists()) {
                    String msg = "Missing folder "+monitoringDir.getPath();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    logException(new Exception(msg));
                    continue;
                }
                if (!monitoringDir.isDirectory()) {
                    String msg = monitoringDir.getPath()+" is not a folder";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    logException(new Exception(msg));
                    continue;
                }
                upload(monitoringDir.getAbsolutePath(), false);
            }

            // upload legacy monitorings
            for (String monitoring : baseDir.list()) {
                if (!monitoring.endsWith("-up")) continue;
                File monitoringDir = new File(baseDir, monitoring);
                if (!monitoringDir.isDirectory()) continue;
                upload(monitoringDir.getAbsolutePath(), true);
            }
        } finally {
            isUploading = false;
            eventBus.post(new UploadCompleted());
        }
    }

    @ServiceAction()
    void upload(String monitoringPath, boolean legacy) {
        Log.d(TAG, String.format("uploading %s", monitoringPath));
        File file = new File(monitoringPath);
        String monitoringName = file.getName();
        if (legacy) monitoringName = monitoringName.replace("-up", "");
        Log.d(TAG, String.format("uploading %s", monitoringName));

        try {
            uploadOnServer(monitoringPath, monitoringName);
            if (legacy) {
                file.renameTo(new File(monitoringPath.replace("-up", "")));
            } else {
                monitoringManager.updateStatus(monitoringName, uploaded);
            }
        } catch (Throwable e) {
            logException(e);
            Toast.makeText(
                    this,
                    String.format("Could not upload %s to server!\n" +
                            "You will need to manually export.", monitoringName),
                    Toast.LENGTH_SHORT).show();
            try {
                uploadOnFtp(monitoringPath, monitoringName);
            } catch (Throwable t) {
                logException(t);
                Toast.makeText(
                        this,
                        String.format("Could not upload %s to ftp!", monitoringName),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadOnServer(String monitoringPath, String monitoringName) throws Exception {
        File file = new File(monitoringPath);

        // map between filenames and their ids
        Map<String, JsonObject> fileObjs = new HashMap<>();

        // first upload images
        for (String subfile : file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches("Pic\\d+\\.jpg") || "track.gpx".equals(name);
            }
        })) {
            try {
                fileObjs.put(subfile, uploadFile(new File(file, subfile)));
            } catch (Throwable t) {
                logException(t);
                Toast.makeText(this,
                        String.format("Could not upload %s of %s to smartbirds.org!", subfile, monitoringName),
                        Toast.LENGTH_SHORT).show();
            }
        }

        if (!fileObjs.containsKey("track.gpx") && new File(file, "track.gpx").exists()) {
            // try again
            fileObjs.put("track.gpx", uploadFile(new File(file, "track.gpx")));
        }


        // then upload forms
        for (String subfile : file.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(".*\\.csv");
            }
        })) {
            uploadForm(monitoringName, file, subfile, fileObjs);
        }
    }

    private void uploadForm(String monitoringName, File base, String filename, Map<String, JsonObject> fileObjs) throws Exception {
        for (EntryType entryType : EntryType.values()) {
            if (entryType.filename.equalsIgnoreCase(filename)) {
                uploadForm(monitoringName, new File(base, filename), entryType.getConverter(this), entryType.getUploader(), fileObjs);
                return;
            }
        }
        Log.w(TAG, "unhandled form file: " + filename);
    }

    private JsonObject uploadFile(File file) throws IOException {
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        Call<ResponseEnvelope<FileId>> call = backend.api().upload(body);
        Response<ResponseEnvelope<FileId>> response = call.execute();
        if (!response.isSuccessful()) {
            throw new IOException("Server error: " + response.code() + " - " + response.message());
        }

        JsonObject fileObj = new JsonObject();
        fileObj.addProperty("url", String.format("%sstorage/%s", BuildConfig.BACKEND_BASE_URL, response.body().data.id));
        return fileObj;
    }

    private void uploadForm(String monitoringName, File file, Converter converter, Uploader uploader, Map<String, JsonObject> fileObjs) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        try {
            CSVReader<String[]> csvReader = new CSVReaderBuilder<String[]>(new InputStreamReader(new BufferedInputStream(fis))).strategy(CSVStrategy.DEFAULT).entryParser(new SmartBirdsCSVEntryParser()).build();
            try {
                List<String> header = csvReader.readHeader();
                for (String[] row : csvReader) {
                    HashMap<String, String> csv = new HashMap<>();
                    Iterator<String> it = header.iterator();
                    String columnName;
                    for (int idx = 0; it.hasNext() && idx < row.length; idx++) {
                        columnName = it.next();
                        csv.put(columnName, row[idx]);
                    }

                    JsonObject data = converter.convert(csv);

                    // convert pictures
                    JsonArray pictures = new JsonArray();
                    int idx = 0;
                    while (true) {
                        String fieldName = "Picture" + idx;
                        idx++;
                        if (!csv.containsKey(fieldName)) break;
                        String filename = csv.get(fieldName);
                        if (TextUtils.isEmpty(filename)) continue;
                        JsonObject fileObj = fileObjs.get(filename);
                        if (fileObj == null) {
                            final String error = String.format("Missing image %s for %s", filename, monitoringName);
                            logException(new IllegalStateException(error));
                            Toast.makeText(this,
                                    error,
                                    Toast.LENGTH_SHORT).show();
                            continue;
                        }
                        pictures.add(fileObj);
                    }
                    data.add("pictures", pictures);

                    // convert gpx
                    if (!fileObjs.containsKey("track.gpx")) {
                        String msg = "Missing track.gpx file for "+monitoringName;
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                        logException(new IllegalStateException(msg));
                    } else {
                        data.add("track", fileObjs.get("track.gpx").get("url"));
                    }

                    Call<ResponseBody> call = uploader.upload(backend.api(), data);
                    Response<ResponseBody> response = call.execute();
                    if (!response.isSuccessful()) {
                        String error = "";
                        try {
                            error += response.code() + ": " + response.message();
                            error += "\n" + response.errorBody().string();
                        } catch (Throwable t) {
                            logException(t);
                        }
                        throw new IOException("Couldn't upload form: " + error);
                    }
                }
            } finally {
                csvReader.close();
            }
        } finally {
            fis.close();
        }
    }

    private void uploadOnFtp(String monitoringPath, String monitoringName) throws IOException {
        FTPClient ftpClient = FTPClientUtils.connect();
        Log.d(TAG, String.format("mkdir %s", monitoringName));
        ftpClient.makeDirectory(monitoringName);
        File file = new File(monitoringPath);
        for (String subfile : file.list()) {
            doUpload(ftpClient, new File(file, subfile), monitoringName + '/');
        }
    }

    private void doUpload(FTPClient ftp, File localFile, String remotePrefix) throws IOException {
        if (localFile.isDirectory()) {
            Log.d(TAG, String.format("mkdir %s", remotePrefix + localFile.getName()));
            ftp.makeDirectory(remotePrefix + localFile.getName());
            for (String subfile : localFile.list())
                doUpload(ftp, new File(localFile, subfile), remotePrefix + localFile.getName() + '/');
        } else {
            FileInputStream localStream = new FileInputStream(localFile);
            Log.d(TAG, String.format("store %s (%d)", remotePrefix + localFile.getName() + ".tmp", localStream.available()));
            ftp.storeFile(remotePrefix + localFile.getName() + ".tmp", localStream);
            localStream.close();
            Log.d(TAG, String.format("rename %s -> %s", remotePrefix + localFile.getName() + ".tmp", remotePrefix + localFile.getName()));
            ftp.rename(remotePrefix + localFile.getName() + ".tmp", remotePrefix + localFile.getName());
        }
    }

}
