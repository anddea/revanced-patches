package com.liskovsoft.googlecommon.common.helpers;

import com.liskovsoft.googlecommon.common.converters.gson.GsonConverterFactory;
import com.liskovsoft.googlecommon.common.converters.gson.WithGson;
import com.liskovsoft.googlecommon.common.converters.regexp.WithRegExp;
import com.liskovsoft.googlecommon.common.converters.regexp.converter.RegExpConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.ConnectException;

import app.revanced.extension.shared.utils.Logger;
import retrofit2.Call;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RetrofitHelper {
    // Ignored when specified url is absolute
    private static final String DEFAULT_BASE_URL = "https://www.youtube.com";

    private static <T> T withGson(Class<T> clazz) {
        return buildRetrofit(GsonConverterFactory.create()).create(clazz);
    }

    private static <T> T withRegExp(Class<T> clazz) {
        return buildRetrofit(RegExpConverterFactory.create()).create(clazz);
    }

    //public static <T> T get(Call<T> wrapper) {
    //    Response<T> response = getResponse(wrapper);
    //
    //    //handleResponseErrors(response);
    //
    //    return response != null ? response.body() : null;
    //}

    public static <T> T get(Call<T> wrapper) {
        return get(wrapper, false);
    }

    public static <T> T get(Call<T> wrapper, boolean skipAuth) {
        if (skipAuth) {
            RetrofitOkHttpHelper.addAuthSkip(wrapper.request());
        }

        Response<T> response = getResponse(wrapper);

        return response != null ? response.body() : null;
    }

    public static <T> Response<T> getResponse(Call<T> wrapper) {
        try {
            return wrapper.execute();
        } catch (ConnectException e) {
            Logger.printException(() -> "getResponse failed: server is down?", e);
        } catch (IOException e) {
            Logger.printException(() -> "getResponse failed", e);
        }

        return null;
    }

    public static Retrofit buildRetrofit(Converter.Factory factory) {
        Retrofit.Builder builder = createBuilder();

        return builder
                .addConverterFactory(factory)
                .build();
    }

    private static Retrofit.Builder createBuilder() {
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(DEFAULT_BASE_URL);

        retrofitBuilder.client(RetrofitOkHttpHelper.getClient());

        return retrofitBuilder;
    }

    public static <T> T create(Class<T> clazz) {
        Annotation[] annotations = clazz.getAnnotations();

        for (Annotation annotation : annotations) {
            if (annotation instanceof WithRegExp) {
                return withRegExp(clazz);
            } else if (annotation instanceof WithGson) {
                return withGson(clazz);
            }
        }

        throw new IllegalStateException("RetrofitHelper: unknown class: " + clazz.getName());
    }
}
