package com.dragons.aurora.playstoreapiv2;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class HttpClientAdapter {

    abstract public byte[] get(String url, Map<String, String> params, Map<String, String> headers) throws IOException;
    abstract public byte[] post(String url, Map<String, String> params, Map<String, String> headers) throws IOException;
    abstract public byte[] post(String url, byte[] body, Map<String, String> headers) throws IOException;
    abstract public String buildUrl(String url, Map<String, String> params);

    public byte[] get(String url, Map<String, String> params) throws IOException {
        return get(url, params, new HashMap<>());
    }

    public byte[] get(String url) throws IOException {
        return get(url, new HashMap<>());
    }

    static public Map<String, List<String>> expand(Map<String, String> params) {
        Map<String, List<String>> result = new HashMap<String, List<String>>();
        for (String name: params.keySet()) {
            result.put(name, Collections.singletonList(params.get(name)));
        }
        return result;
    }
}
