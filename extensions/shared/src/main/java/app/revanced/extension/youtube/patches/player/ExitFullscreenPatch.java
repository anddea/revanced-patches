package app.revanced.extension.youtube.patches.player;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public class ExitFullscreenPatch {

    public enum FullscreenMode {
        DISABLED,
        PORTRAIT,
        LANDSCAPE,
        PORTRAIT_LANDSCAPE,
    }

    /**
     * Injection point.
     */
    public static void endOfVideoReached() {
        try {
            FullscreenMode mode = Settings.EXIT_FULLSCREEN.get();
            if (mode == FullscreenMode.DISABLED) {
                return;
            }

            if (PlayerType.getCurrent() == PlayerType.WATCH_WHILE_FULLSCREEN) {
                if (mode != FullscreenMode.PORTRAIT_LANDSCAPE) {
                    if (Utils.isLandscapeOrientation()) {
                        if (mode == FullscreenMode.PORTRAIT) {
                            return;
                        }
                    } else if (mode == FullscreenMode.LANDSCAPE) {
                        return;
                    }
                }

                Utils.runOnMainThread(VideoUtils::exitFullscreenMode);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "endOfVideoReached failure", ex);
        }
    }
}
