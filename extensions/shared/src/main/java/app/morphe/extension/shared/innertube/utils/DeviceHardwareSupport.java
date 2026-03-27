package app.morphe.extension.shared.innertube.utils;

import static app.morphe.extension.shared.utils.Utils.isSDKAbove;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import app.morphe.extension.shared.utils.Logger;

public class DeviceHardwareSupport {
    private static final boolean DEVICE_HAS_HARDWARE_DECODING_AV1;

    static {
        boolean av1found = false;
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        final boolean deviceIsAndroidTenOrLater = isSDKAbove(29);

        for (MediaCodecInfo codecInfo : codecList.getCodecInfos()) {
            final boolean isHardwareAccelerated = deviceIsAndroidTenOrLater
                    ? codecInfo.isHardwareAccelerated()
                    : !codecInfo.getName().startsWith("OMX.google"); // Software decoder.
            if (isHardwareAccelerated && !codecInfo.isEncoder()) {
                for (String type : codecInfo.getSupportedTypes()) {
                    if (type.equalsIgnoreCase("video/av01")) {
                        av1found = true;
                        break;
                    }
                }
            }
        }

        DEVICE_HAS_HARDWARE_DECODING_AV1 = av1found;

        Logger.printDebug(() -> DEVICE_HAS_HARDWARE_DECODING_AV1
                ? "Device supports AV1 hardware decoding"
                : "Device does not support AV1 hardware decoding");
    }

    public static boolean hasAV1Decoder() {
        return DEVICE_HAS_HARDWARE_DECODING_AV1;
    }
}
