package org.bspb.smartbirds.pro.forms.upload;

import com.google.gson.JsonObject;

import org.bspb.smartbirds.pro.backend.SmartBirdsApi;

import okhttp3.ResponseBody;
import retrofit2.Call;

/**
 * Created by groupsky on 28.09.16.
 */

public class CbmUploader implements Uploader {
    @Override
    public Call<ResponseBody> upload(SmartBirdsApi api, JsonObject data) {
        return api.createCbm(data);
    }
}
