/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.adapter;

import com.dragons.aurora.playstoreapiv2.AuthException;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.HttpClientAdapter;

import org.apache.commons.collections4.MapUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.shared.utils.Logger;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttpClientAdapter extends HttpClientAdapter {

    private final OkHttpClient client;

    public OkHttpClientAdapter() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final HashMap<HttpUrl, List<Cookie>> cookieStore = new HashMap<>();

                    @Override
                    public void saveFromResponse(@NotNull HttpUrl url, @NotNull List<Cookie> cookies) {
                        cookieStore.put(url, cookies);
                    }

                    @NotNull
                    @Override
                    public List<Cookie> loadForRequest(@NotNull HttpUrl url) {
                        List<Cookie> cookies = cookieStore.get(url);
                        return cookies != null ? cookies : new ArrayList<>();
                    }
                });
        client = builder.build();
    }

    @Override
    public byte[] get(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(buildUrl(url, params))
                .get();
        return request(requestBuilder, headers);
    }

    @Override
    public byte[] post(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        FormBody.Builder bodyBuilder = new FormBody.Builder();
        if (MapUtils.isNotEmpty(params)) {
            for (String name : params.keySet()) {
                String value = params.get(name);
                if (value != null) {
                    bodyBuilder.add(name, value);
                }
            }
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(bodyBuilder.build());

        return post(url, requestBuilder, headers);
    }

    @Override
    @SuppressWarnings("deprecation")
    public byte[] post(String url, byte[] body, Map<String, String> headers) throws IOException {
        if (!headers.containsKey("Content-Type")) {
            headers.put("Content-Type", "application/x-protobuf");
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-protobuf"), body));

        return post(url, requestBuilder, headers);
    }

    private byte[] post(String url, Request.Builder requestBuilder, Map<String, String> headers) throws IOException {
        requestBuilder.url(url);
        return request(requestBuilder, headers);
    }


    private byte[] request(Request.Builder requestBuilder, Map<String, String> headers) throws IOException {
        Request request = requestBuilder
                .headers(Headers.of(headers))
                .build();
        int code;
        byte[] content = new byte[0];
        try (Response response = client.newCall(request).execute()) {
            code = response.code();
            var body = response.body();
            if (body != null) {
                content = body.bytes();
            }
        }
        if (content.length == 0) return null;

        if (code == 401 || code == 403) {
            AuthException authException = new AuthException("Auth error", code);
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if ("NeedsBrowser".equals(authResponse.get("Error"))) {
                authException.setTwoFactorUrl(authResponse.get("Url"));
            }
            throw authException;
        } else if (code == 404) {
            Map<String, String> authResponse = GooglePlayAPI.parseResponse(new String(content));
            if ("UNKNOWN_ERR".equals(authResponse.get("Error"))) {
                Logger.printException(() -> "Unknown error occurred: " + code);
            } else
                Logger.printException(() -> "App not found: " + code);
        } else if (code == 429) {
            Logger.printException(() -> "Rate-limiting enabled, you are making too many requests: " + code);
        } else if (code >= 500) {
            Logger.printException(() -> "Server error: " + code);
        } else if (code >= 400) {
            Logger.printException(() -> "Malformed request: " + code);
        }
        return content;
    }

    public String buildUrl(String url, Map<String, String> params) {
        var httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) return null;
        HttpUrl.Builder urlBuilder = httpUrl.newBuilder();
        if (null != params && !params.isEmpty()) {
            for (String name : params.keySet()) {
                urlBuilder.addQueryParameter(name, params.get(name));
            }
        }
        return urlBuilder.build().toString();
    }
}