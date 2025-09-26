package com.liskovsoft.googlecommon.common.converters.regexp.converter;

import android.util.Log;
import com.liskovsoft.googlecommon.common.converters.regexp.typeadapter.RegExpTypeAdapter;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.internal.EverythingIsNonNull;

public class RegExpRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final String TAG = RegExpRequestBodyConverter.class.getSimpleName();
    private static final MediaType MEDIA_TYPE = MediaType.get("text/plain; charset=UTF-8");

    public RegExpRequestBodyConverter(RegExpTypeAdapter<T> adapter) {
    }

    @EverythingIsNonNull
    @Override
    public RequestBody convert(T value) {
        Log.d(TAG, value.toString());
        return RequestBody.create(MEDIA_TYPE, value.toString());
    }
}
