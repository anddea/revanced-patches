package com.liskovsoft.sharedutils.okhttp;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import app.revanced.extension.shared.utils.Logger;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpManager {
    private static final String TAG = OkHttpManager.class.getSimpleName();
    private static final int NUM_TRIES = 1;
    private static OkHttpManager sInstance;
    private OkHttpClient mClient;
    private final boolean mEnableProfiler;

    private OkHttpManager(boolean enableProfiler) {
        mEnableProfiler = enableProfiler;
    }

    public static OkHttpManager instance() {
        return instance(true); // profiler is enabled by default
    }

    public static OkHttpManager instance(boolean enableProfiler) {
        if (sInstance == null) {
            sInstance = new OkHttpManager(enableProfiler);
        }

        return sInstance;
    }

    public static void unhold() {
        sInstance = null;
    }

    public Response doRequest(String url) {
        return doRequest(url, getClient());
    }

    public Response doGetRequest(String url, Map<String, String> headers) {
        if (headers == null) {
            Logger.printDebug(() -> "Headers are null... doing regular request...");
            return doGetRequest(url, getClient());
        }

        return doGetRequest(url, getClient(), headers);
    }

    public Response doPostRequest(String url, Map<String, String> headers, String postBody, @Nullable String contentType) {
        return doPostRequest(url, getClient(), headers, postBody, contentType);
    }

    public Response doGetRequest(String url) {
        return doGetRequest(url, getClient());
    }

    public Response doHeadRequest(String url) {
        return doHeadRequest(url, getClient());
    }

    /**
     * NOTE: default method is GET
     */
    public Response doRequest(String url, OkHttpClient client) {
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .build();

        return doRequest(client, okHttpRequest);
    }

    /**
     * NOTE: default method is GET
     */
    public Response doRequest(String url, OkHttpClient client, Map<String, String> headers) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .build();

        return doRequest(client, okHttpRequest);
    }

    private Response doPostRequest(String url, OkHttpClient client, Map<String, String> headers, String body, @Nullable String contentType) {
        if (headers == null) {
            headers = new HashMap<>();
        }

        Request okHttpRequest = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .post(RequestBody.create(contentType != null ? MediaType.parse(contentType) : null, body))
                .build();

        return doRequest(client, okHttpRequest);
    }

    private Response doGetRequest(String url, OkHttpClient client, Map<String, String> headers) {
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .get()
                .build();

        return doRequest(client, okHttpRequest);
    }

    private Response doGetRequest(String url, OkHttpClient client) {
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .get()
                .build();

        return doRequest(client, okHttpRequest);
    }

    private Response doHeadRequest(String url, OkHttpClient client) {
        Request okHttpRequest = new Request.Builder()
                .url(url)
                .head()
                .build();

        return doRequest(client, okHttpRequest);
    }

    //private Response doRequest(OkHttpClient client, Request okHttpRequest) {
    //    Response okHttpResponse = null;
    //    Exception lastEx = null;
    //
    //    for (int tries = NUM_TRIES; tries > 0; tries--) {
    //        try {
    //            okHttpResponse = client.newCall(okHttpRequest).execute();
    //            if (!okHttpResponse.isSuccessful()) {
    //                throw new IllegalStateException("Unexpected code " + okHttpResponse);
    //            }
    //
    //            break; // no exception is thrown - job is done
    //        } catch (Exception ex) {
    //            //Log.e(TAG, ex.getMessage()); // network error, just return null
    //            okHttpResponse = null;
    //            lastEx = ex;
    //        }
    //    }
    //
    //    if (lastEx != null && okHttpResponse == null) { // request failed
    //        lastEx.printStackTrace();
    //        Log.e(TAG, lastEx.getMessage());
    //    }
    //
    //    return okHttpResponse;
    //}

    private Response doRequest(OkHttpClient client, Request okHttpRequest) {
        try {
            return client.newCall(okHttpRequest).execute();
        } catch (IOException ex) {
            String msg = "Interrupted OkHttp request to " + okHttpRequest.url();
            Logger.printException(() -> msg, ex);
            throw new IllegalStateException(msg, ex);
        }
    }

    public OkHttpClient getClient() {
        if (mClient == null) {
            OkHttpCommons.enableProfiler = mEnableProfiler;
            mClient = OkHttpCommons.setupBuilder(new OkHttpClient.Builder()).build();
        }

        return mClient;
    }
}
