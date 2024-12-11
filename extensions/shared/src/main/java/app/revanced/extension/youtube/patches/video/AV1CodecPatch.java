package app.revanced.extension.youtube.patches.video;

import static app.revanced.extension.shared.utils.StringRef.str;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class AV1CodecPatch {
    private static final int LITERAL_VALUE_AV01 = 1635135811;
    private static final int LITERAL_VALUE_DOLBY_VISION = 1685485123;
    private static final String VP9_CODEC = "video/x-vnd.on2.vp9";
    private static long lastTimeResponse = 0;

    /**
     * Replace the SW AV01 codec to VP9 codec.
     * May not be valid on some clients.
     *
     * @param original hardcoded value - "video/av01"
     */
    public static String replaceCodec(String original) {
        return Settings.REPLACE_AV1_CODEC.get() ? VP9_CODEC : original;
    }

    /**
     * Replace the SW AV01 codec request with a Dolby Vision codec request.
     * This request is invalid, so it falls back to codecs other than AV01.
     * <p>
     * Limitation: Fallback process causes about 15-20 seconds of buffering.
     *
     * @param literalValue literal value of the codec
     */
    public static int rejectResponse(int literalValue) {
        if (!Settings.REJECT_AV1_CODEC.get())
            return literalValue;

        Logger.printDebug(() -> "Response: " + literalValue);

        if (literalValue != LITERAL_VALUE_AV01)
            return literalValue;

        final long currentTime = System.currentTimeMillis();

        // Ignore the invoke within 20 seconds.
        if (currentTime - lastTimeResponse > 20000) {
            lastTimeResponse = currentTime;
            Utils.showToastShort(str("revanced_reject_av1_codec_toast"));
        }

        return LITERAL_VALUE_DOLBY_VISION;
    }
}
