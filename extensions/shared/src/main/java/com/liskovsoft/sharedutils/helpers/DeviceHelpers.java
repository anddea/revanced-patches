package com.liskovsoft.sharedutils.helpers;

public final class DeviceHelpers {
    private static int sMaxHeapMemoryMB = -1;

    public static int getMaxHeapMemoryMB() {
        if (sMaxHeapMemoryMB == -1) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            sMaxHeapMemoryMB = (int) (maxMemory / (1024 * 1024)); // Growth Limit
        }

        return sMaxHeapMemoryMB;
    }
}
