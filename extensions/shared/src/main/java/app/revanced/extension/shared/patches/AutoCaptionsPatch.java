package app.revanced.extension.shared.patches;

import app.revanced.extension.shared.settings.BaseSettings;

@SuppressWarnings("unused")
public final class AutoCaptionsPatch {

    private static boolean captionsButtonStatus;

    public static boolean disableAutoCaptions() {
        return BaseSettings.DISABLE_AUTO_CAPTIONS.get() &&
                !captionsButtonStatus;
    }

    public static void setCaptionsButtonStatus(boolean status) {
        captionsButtonStatus = status;
    }
}