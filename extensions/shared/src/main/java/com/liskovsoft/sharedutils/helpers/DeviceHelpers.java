package com.liskovsoft.sharedutils.helpers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.CookieManager;

public final class DeviceHelpers {
    private static int sMaxHeapMemoryMB = -1;

    /**
     * @return whether the device has support for WebView, see
     * <a href="https://stackoverflow.com/a/69626735">https://stackoverflow.com/a/69626735</a>
     */
    public static boolean isWebViewSupported() {
        try {
            CookieManager.getInstance();
            return !isWebViewBroken();
        } catch (final Throwable ignored) {
            return false;
        }
    }

    // This value is always false, but is left in for better tracking of the MediaServiceCore module.
    @SuppressLint("ObsoleteSdkInt")
    private static boolean isWebViewBroken() {
        return Build.VERSION.SDK_INT == 19 && isTCL(); // "TCL TV - Harman"
    }

    public static boolean isTCL() {
        return Build.MANUFACTURER.toLowerCase().contains("tcl") || Build.BRAND.toLowerCase().contains("tcl");
    }

    public static int getMaxHeapMemoryMB() {
        if (sMaxHeapMemoryMB == -1) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            sMaxHeapMemoryMB = (int) (maxMemory / (1024 * 1024)); // Growth Limit
        }

        return sMaxHeapMemoryMB;
    }

    public static int getAllocatedHeapMemoryMB() {
        long allocatedMemory = Runtime.getRuntime().totalMemory();
        return (int) (allocatedMemory / (1024 * 1024));
    }

    public static boolean isMemoryCritical() {
        return getAllocatedHeapMemoryMB() > getMaxHeapMemoryMB() * 0.5;
    }
}
