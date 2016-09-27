package org.bspb.smartbirds.pro.forms;

import com.google.gson.JsonObject;

import org.bspb.smartbirds.pro.backend.Converter;
import org.bspb.smartbirds.pro.backend.SmartBirdsApi;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Created by groupsky on 27.09.16.
 */

public class HerpUploader implements Uploader {
    @Override
    public boolean upload(SmartBirdsApi api, List<String> header, String[] row) throws Exception {
        JsonObject obj = Converter.convertHerp(header, row);
        Response<ResponseBody> res = api.createHerp(obj).execute();
        return res.isSuccessful();
    }
}