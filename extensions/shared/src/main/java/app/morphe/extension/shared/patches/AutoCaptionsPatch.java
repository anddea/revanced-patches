/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.patches;

import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class AutoCaptionsPatch {

    /**
     * Automatic-caption behavior available in YouTube 20.26 and later.
     */
    public enum AutoCaptionsStyle {
        BOTH_ENABLED,
        BOTH_DISABLED,
        WITH_VOLUME_ONLY,
        WITHOUT_VOLUME_ONLY
    }

    private static boolean captionsButtonStatus;

    public static boolean disableAutoCaptions() {
        return BaseSettings.DISABLE_AUTO_CAPTIONS.get() &&
                !captionsButtonStatus;
    }

    /**
     * Injection point for YouTube 20.26+ automatic captions while volume is enabled.
     *
     * @return whether automatic captions should be disabled
     */
    public static boolean disableAutoCaptionsByStyle() {
        AutoCaptionsStyle style = Settings.AUTO_CAPTIONS_STYLE.get();
        boolean disableWithVolume = style == AutoCaptionsStyle.BOTH_DISABLED ||
                style == AutoCaptionsStyle.WITHOUT_VOLUME_ONLY;

        return disableWithVolume && !captionsButtonStatus;
    }

    /**
     * Injection point for YouTube's automatic captions while the device is muted.
     *
     * @return whether muted-volume automatic captions should remain enabled
     */
    public static boolean disableMuteAutoCaptions() {
        AutoCaptionsStyle style = Settings.AUTO_CAPTIONS_STYLE.get();
        return style == AutoCaptionsStyle.BOTH_ENABLED ||
                style == AutoCaptionsStyle.WITHOUT_VOLUME_ONLY;
    }

    public static void setCaptionsButtonStatus(boolean status) {
        captionsButtonStatus = status;
    }
}
