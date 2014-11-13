package org.bspb.smartbirds.pro.ui.utils;

import android.content.Context;
import android.util.Log;

import com.googlecode.jcsv.reader.CSVReader;
import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;
import org.apache.commons.net.ftp.FTPClient;
import org.bspb.smartbirds.pro.SmartBirdsApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dani on 14-11-12.
 */
@EBean(scope = EBean.Scope.Singleton)
public class NomenclaturesBean {

    private static final String TAG = SmartBirdsApplication.TAG + ".NomenclaturesBean";

    private static final String NOMENCLATURES_FILE_NAME = "nomenclatures.csv";
    private static final String FTP_PATH = "nomenclatures/nomenclatures.csv";

    private Map<String, List<String>> data;

    @RootContext
    Context context;

    public NomenclaturesBean() {
        data = new HashMap<String, List<String>>();
    }

    @AfterInject
    @Background
    public void loadData() {
        try {
            File nomenclatures = new File(context.getExternalFilesDir(null), NOMENCLATURES_FILE_NAME);
            InputStream inputStream;
            if (nomenclatures.exists() && nomenclatures.length() > 0) {
                inputStream = new FileInputStream(nomenclatures);
            } else {
                inputStream = context.getAssets().open(NOMENCLATURES_FILE_NAME);
            }

            CSVReader<String[]> csv = CSVReaderBuilder.newDefaultReader(new InputStreamReader(inputStream));
            List<String> keys = csv.readHeader();

            for (String key : keys) {
                data.put(key, new ArrayList<String>());
            }

            List<String[]> rows = csv.readAll();
            if (rows != null && !rows.isEmpty()) {
                for (String[] row : rows) {
                    for (int i = 0; i < row.length; i++) {
                        if (row[i] != null && !row[i].equals("")) {
                            data.get(keys.get(i)).add(row[i]);
                        }
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Background
    public void loadDataFromServer() {
        FTPClient ftpClient = FTPClientUtils.connect();
        try {
            File file = new File(context.getExternalFilesDir(null), NOMENCLATURES_FILE_NAME);
            FileOutputStream out = new FileOutputStream(file);

            ftpClient.retrieveFile(FTP_PATH, out);
            loadData();
        } catch (IOException e) {
            Log.e(TAG, String.format("error while downloading nomenclatures: %s", e.getMessage()), e);
        }

    }

    public List<String> getNomenclature(String key) {
        return data.get(key);
    }

}
