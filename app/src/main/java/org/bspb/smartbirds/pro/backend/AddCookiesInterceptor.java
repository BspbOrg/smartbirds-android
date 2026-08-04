package org.bspb.smartbirds.pro.backend;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AddCookiesInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        for (String cookie : SecureCookieStore.INSTANCE.readCookies()) {
            builder.addHeader("Cookie", cookie);
        }
        return chain.proceed(builder.build());
    }
}
