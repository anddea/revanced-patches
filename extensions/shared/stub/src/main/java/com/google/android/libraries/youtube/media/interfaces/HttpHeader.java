package com.google.android.libraries.youtube.media.interfaces;

public record HttpHeader(String key, String value) {

    @Override
    public String toString() {
        return "HttpHeader{key=" + this.key +
                ",value=" + this.value +
                "}";
    }
}