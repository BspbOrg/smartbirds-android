package org.bspb.smartbirds.pro.backend;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Response;

public class ReceivedCookiesInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());

        List<String> setCookies = originalResponse.headers("Set-Cookie");
        if (!setCookies.isEmpty()) {
            HashSet<String> cookies = new HashSet<>(SecureCookieStore.INSTANCE.readCookies());
            cookies.addAll(setCookies);
            SecureCookieStore.INSTANCE.writeCookies(cookies);
        }

        return originalResponse;
    }
}
