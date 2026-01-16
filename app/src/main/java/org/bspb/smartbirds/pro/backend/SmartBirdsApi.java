package org.bspb.smartbirds.pro.backend;

import com.google.gson.JsonObject;

import org.bspb.smartbirds.pro.backend.dto.BaseResponse;
import org.bspb.smartbirds.pro.backend.dto.CheckSessionRequest;
import org.bspb.smartbirds.pro.backend.dto.DownloadsResponse;
import org.bspb.smartbirds.pro.backend.dto.FileId;
import org.bspb.smartbirds.pro.backend.dto.LoginRequest;
import org.bspb.smartbirds.pro.backend.dto.LoginResponse;
import org.bspb.smartbirds.pro.backend.dto.MapLayerItem;
import org.bspb.smartbirds.pro.backend.dto.Nomenclature;
import org.bspb.smartbirds.pro.backend.dto.ResponseEnvelope;
import org.bspb.smartbirds.pro.backend.dto.ResponseListEnvelope;
import org.bspb.smartbirds.pro.backend.dto.UploadFormResponse;
import org.bspb.smartbirds.pro.backend.dto.Zone;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by dani on 08.08.16.
 */
public interface SmartBirdsApi {

    @POST("session")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @PUT("session")
    Call<LoginResponse> checkSession(@Body CheckSessionRequest checkSessionRequest);

    @GET("nomenclature")
    Call<ResponseListEnvelope<Nomenclature>> nomenclatures(@Query("limit") int limit, @Query("offset") int offset);

    @GET("species")
    Call<ResponseListEnvelope<Nomenclature>> species(@Query("limit") int limit, @Query("offset") int offset);

    @POST("birds")
    Call<UploadFormResponse> createBirds(@Body JsonObject request);

    @POST("herptiles")
    Call<UploadFormResponse> createHerptile(@Body JsonObject request);

    @POST("mammals")
    Call<UploadFormResponse> createMammal(@Body JsonObject request);

    @POST("invertebrates")
    Call<UploadFormResponse> createInvertebrates(@Body JsonObject request);

    @POST("ciconia")
    Call<UploadFormResponse> createCiconia(@Body JsonObject request);

    @POST("cbm")
    Call<UploadFormResponse> createCbm(@Body JsonObject request);

    @POST("plants")
    Call<UploadFormResponse> createPlants(@Body JsonObject request);

    @POST("threats")
    Call<UploadFormResponse> createThreats(@Body JsonObject request);

    @Multipart
    @POST("storage")
    Call<ResponseEnvelope<FileId>> upload(@Part MultipartBody.Part file);

    @GET("zone?limit=-1&status=owned")
    Call<ResponseListEnvelope<Zone>> listZones();

    @GET
    Call<DownloadsResponse> getDownloads(@Url String url);

    @POST("pylons")
    Call<UploadFormResponse> createPylons(@Body JsonObject request);

    @POST("pylons-casualties")
    Call<UploadFormResponse> createPylonsCasualties(@Body JsonObject request);

    @POST("birds-migrations")
    Call<UploadFormResponse> createBirdsMigrations(@Body JsonObject request);

    @GET("pois")
    Call<ResponseListEnvelope<Nomenclature>> pois(@Query("limit") int limit, @Query("offset") int offset);

    @POST("fishes")
    Call<UploadFormResponse> createFish(@Body JsonObject request);

    @GET("map-layers")
    Call<ResponseListEnvelope<MapLayerItem>> mapLayers(@Query("limit") int limit, @Query("offset") int offset);

    @POST("bats")
    Call<UploadFormResponse> createBat(@Body JsonObject request);

    @POST("bears")
    Call<UploadFormResponse> createBear(@Body JsonObject request);

    @DELETE("user/{id}")
    Call<BaseResponse> deleteUser(@Path("id") long id);
}
