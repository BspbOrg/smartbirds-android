package org.bspb.smartbirds.pro.backend;

import com.google.gson.JsonObject;

import org.bspb.smartbirds.pro.backend.dto.FileId;
import org.bspb.smartbirds.pro.backend.dto.LoginRequest;
import org.bspb.smartbirds.pro.backend.dto.LoginResponse;
import org.bspb.smartbirds.pro.backend.dto.Nomenclature;
import org.bspb.smartbirds.pro.backend.dto.ResponseEnvelope;
import org.bspb.smartbirds.pro.backend.dto.ResponseListEnvelope;
import org.bspb.smartbirds.pro.backend.dto.SpeciesNomenclature;
import org.bspb.smartbirds.pro.backend.dto.Zone;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

/**
 * Created by dani on 08.08.16.
 */
public interface SmartBirdsApi {

    @POST("session")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @GET("nomenclature")
    Call<ResponseListEnvelope<Nomenclature>> nomenclatures(@Query("limit") int limit, @Query("offset") int offset);

    @GET("species")
    Call<ResponseListEnvelope<SpeciesNomenclature>> species(@Query("limit") int limit, @Query("offset") int offset);

    @POST("birds")
    Call<ResponseBody> createBirds(@Body JsonObject request);

    @POST("herp")
    Call<ResponseBody> createHerp(@Body JsonObject request);

    @POST("ciconia")
    Call<ResponseBody> createCiconia(@Body JsonObject request);

    @POST("cbm")
    Call<ResponseBody> createCbm(@Body JsonObject request);

    @Multipart
    @POST("storage")
    Call<ResponseEnvelope<FileId>> upload(@Part MultipartBody.Part file);

    @GET("zone?limit=-1&nomenclature=false&status=owned")
    Call<ResponseListEnvelope<Zone>> listZones();
}
