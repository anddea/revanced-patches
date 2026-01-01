package com.dragons.aurora.playstoreapiv2;

public interface DeviceInfoProvider {

    String getAuthUserAgentString();
    int getSdkVersion();
    int getPlayServicesVersion();
}
