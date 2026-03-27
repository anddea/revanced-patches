package app.morphe.extension.shared.innertube.utils;

import java.io.File;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class J2V8Support {
    private static final boolean DEVICE_SUPPORT_J2V8;

    static {
        boolean j2v8Support = false;
        try {
            String libraryDir = Utils.getContext()
                    .getApplicationContext()
                    .getApplicationInfo()
                    .nativeLibraryDir;
            File j2v8File = new File(libraryDir + "/libj2v8.so");
            j2v8Support = j2v8File.exists();
        } catch (Exception ex) {
            Logger.printException(() -> "J2V8 native library not found", ex);
        }
        DEVICE_SUPPORT_J2V8 = j2v8Support;

        Logger.printDebug(() -> DEVICE_SUPPORT_J2V8
                ? "Device supports J2V8"
                : "Device does not support J2V8");
    }

    public static boolean supportJ2V8() {
        return DEVICE_SUPPORT_J2V8;
    }
}
