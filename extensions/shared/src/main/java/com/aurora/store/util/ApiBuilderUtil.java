package com.aurora.store.util;

import com.aurora.store.provider.NativeDeviceInfoProvider;
import com.dragons.aurora.playstoreapiv2.DeviceInfoProvider;

public class ApiBuilderUtil {
    public static DeviceInfoProvider getDeviceInfoProvider() {
        NativeDeviceInfoProvider deviceInfoProvider = new NativeDeviceInfoProvider();
        deviceInfoProvider.setGsfVersionProvider();
        return deviceInfoProvider;
    }
}
