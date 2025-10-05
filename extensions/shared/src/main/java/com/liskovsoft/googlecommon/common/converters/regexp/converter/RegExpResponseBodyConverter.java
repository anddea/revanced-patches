package com.liskovsoft.googlecommon.common.converters.regexp.converter;

import com.liskovsoft.googlecommon.common.converters.regexp.typeadapter.RegExpTypeAdapter;

import okhttp3.ResponseBody;
import retrofit2.Converter;

final class RegExpResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final RegExpTypeAdapter<T> mAdapter;

    RegExpResponseBodyConverter(RegExpTypeAdapter<T> adapter) {
        mAdapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) {
        try (value) {
            return mAdapter.read(value.byteStream());
        }
    }
}
