package app.revanced.extension.reddit.patches;

import app.revanced.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public class ScreenshotPopupPatch {

    public static boolean disableScreenshotPopup() {
        return Settings.DISABLE_SCREENSHOT_POPUP.get();
    }
}
