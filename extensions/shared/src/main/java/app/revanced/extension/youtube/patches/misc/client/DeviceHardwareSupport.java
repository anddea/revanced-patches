package app.revanced.extension.youtube.patches.misc.client;

import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;

public class DeviceHardwareSupport {
    private static final boolean DEVICE_HAS_HARDWARE_DECODING_VP9;
    private static final boolean DEVICE_HAS_HARDWARE_DECODING_AV1;

    static {
        boolean vp9found = false;
        boolean av1found = false;
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        final boolean deviceIsAndroidTenOrLater = isSDKAbove(29);

        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            final boolean isHardwareAccelerated = deviceIsAndroidTenOrLater
                    ? codecInfo.isHardwareAccelerated()
                    : !codecInfo.getName().startsWith("OMX.google"); // Software decoder.
            if (isHardwareAccelerated && !codecInfo.isEncoder()) {
                for (String type : codecInfo.getSupportedTypes()) {
                    if (type.equalsIgnoreCase("video/x-vnd.on2.vp9")) {
                        vp9found = true;
                    } else if (type.equalsIgnoreCase("video/av01")) {
                        av1found = true;
                    }
                }
            }
        }

        DEVICE_HAS_HARDWARE_DECODING_VP9 = vp9found;
        DEVICE_HAS_HARDWARE_DECODING_AV1 = av1found;

        Logger.printDebug(() -> DEVICE_HAS_HARDWARE_DECODING_AV1
                ? "Device supports AV1 hardware decoding\n"
                : "Device does not support AV1 hardware decoding\n"
                + (DEVICE_HAS_HARDWARE_DECODING_VP9
                ? "Device supports VP9 hardware decoding"
                : "Device does not support VP9 hardware decoding"));
    }

    public static boolean allowVP9() {
        return DEVICE_HAS_HARDWARE_DECODING_VP9 && !Settings.SPOOF_STREAMING_DATA_IOS_FORCE_AVC.get();
    }

    public static boolean allowAV1() {
        return allowVP9() && DEVICE_HAS_HARDWARE_DECODING_AV1;
    }
}
