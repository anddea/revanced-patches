package com.liskovsoft.sharedutils.helpers;

import android.os.Build;
import android.webkit.CookieManager;

public final class DeviceHelpers {
    /**
     * @return whether the device has support for WebView, see
     * <a href="https://stackoverflow.com/a/69626735">https://stackoverflow.com/a/69626735</a>
     */
    public static boolean isWebViewSupported() {
        try {
            CookieManager.getInstance();
            return true;
        } catch (final Throwable ignored) {
            return false;
        }
    }

    public static boolean isTCL() {
        return Build.MANUFACTURER.toLowerCase().contains("tcl") || Build.BRAND.toLowerCase().contains("tcl");
    }
}
