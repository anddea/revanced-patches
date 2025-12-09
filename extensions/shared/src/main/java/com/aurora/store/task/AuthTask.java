package com.aurora.store.task;

import android.util.Pair;

import com.aurora.store.adapter.OkHttpClientAdapter;
import com.aurora.store.util.ApiBuilderUtil;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

public class AuthTask {
    protected GooglePlayAPI api;

    public AuthTask() {
        api = new GooglePlayAPI();
        api.setLocale(Locale.getDefault());
        api.setDeviceInfoProvider(ApiBuilderUtil.getDeviceInfoProvider());
        api.setClient(new OkHttpClientAdapter());
    }

    public Pair<String, String> getAuthToken(String email, String oauth_token) throws Exception {
        String aasToken = api.generateAASToken(email, oauth_token);
        if (StringUtils.isNotEmpty(aasToken)) {
            return new Pair<>(aasToken, api.generateYouTubeToken(email, aasToken));
        }
        return null;
    }

    public String refreshAuthToken(String email, String aasToken) throws Exception {
        return api.generateYouTubeToken(email, aasToken);
    }
}
