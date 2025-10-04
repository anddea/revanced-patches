package com.liskovsoft.googlecommon.common.converters.regexp.converter;

import androidx.annotation.NonNull;

import com.liskovsoft.googlecommon.common.converters.regexp.typeadapter.RegExpTypeAdapter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public final class RegExpConverterFactory extends Converter.Factory {
    public static RegExpConverterFactory create() {
        return new RegExpConverterFactory();
    }

    private RegExpConverterFactory() {
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(@NonNull Type type,
                                                            @NonNull Annotation[] annotations,
                                                            @NonNull Retrofit retrofit) {
        return new RegExpResponseBodyConverter<>(new RegExpTypeAdapter<>(type));
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(@NonNull Type type,
                                                          @NonNull Annotation[] parameterAnnotations,
                                                          @NonNull Annotation[] methodAnnotations,
                                                          @NonNull Retrofit retrofit) {
        return new RegExpRequestBodyConverter<>(new RegExpTypeAdapter<>(type));
    }
}
