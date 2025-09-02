package com.liskovsoft.sharedutils.okhttp.interceptors;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.zip.Inflater;

import app.revanced.extension.shared.utils.Utils;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.RealResponseBody;
import okio.GzipSource;
import okio.InflaterSource;
import okio.Okio;
import okio.Source;

public class UnzippingInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Response response;

        try {
            response = chain.proceed(chain.request());
        } catch (NullPointerException e) { // proxy error?
            response = createEmptyResponse(chain);
        }

        return unzip(response);
    }

    // copied from okhttp3.internal.http.HttpEngine (because is private)
    private Response unzip(final Response response) {
        if (response == null || response.body() == null) {
            return response;
        }

        //check if we have gzip response
        String contentEncoding = response.headers().get("Content-Encoding");

        //this is used to decompress gzipped responses
        if (contentEncoding != null && contentEncoding.equals("gzip")) {
            long contentLength = response.body().contentLength();
            GzipSource responseBody = new GzipSource(response.body().source());
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(response.body().contentType().toString(), contentLength
                    , Okio.buffer(responseBody))).build();
        } else if (contentEncoding != null && contentEncoding.equals("deflate")) {
            long contentLength = response.body().contentLength();
            InflaterSource responseBody = new InflaterSource(response.body().source(), new Inflater());
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(response.body().contentType().toString(), contentLength
                    , Okio.buffer(responseBody))).build();
        } else if (contentEncoding != null && contentEncoding.equals("br")) {
            long contentLength = response.body().contentLength();
            Source responseBody = Okio.source(Utils.getBrotliInputStream(response.body().source().inputStream()));
            Headers strippedHeaders = response.headers().newBuilder().build();
            return response.newBuilder().headers(strippedHeaders).body(new RealResponseBody(response.body().contentType().toString(), contentLength
                    , Okio.buffer(responseBody))).build();
        } else {
            return response;
        }
    }

    private Response createEmptyResponse(Chain chain) {
        Response.Builder builder = new Response.Builder();
        builder.request(chain.request());
        builder.protocol(Protocol.HTTP_1_1);
        builder.code(204);
        builder.message("Empty response");
        builder.body(ResponseBody.create(MediaType.get("text/plain; charset=UTF-8"), ""));

        return builder.build();
    }
}
