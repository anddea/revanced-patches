package app.revanced.extension.shared.patches;

import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.view.View;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class FullscreenAdsPatch {
    private static final boolean hideFullscreenAdsEnabled = BaseSettings.HIDE_FULLSCREEN_ADS.get();
    private static final ByteArrayFilterGroup exception =
            new ByteArrayFilterGroup(
                    null,
                    "post_image_lightbox.eml" // Community post image in fullscreen
            );

    public static boolean disableFullscreenAds(final byte[] bytes, int type) {
        if (!hideFullscreenAdsEnabled) {
            return false;
        }

        final DialogType dialogType = DialogType.getDialogType(type);
        final String dialogName = dialogType.name();

        // The dialog type of a fullscreen dialog is always {@code DialogType.FULLSCREEN}
        if (dialogType != DialogType.FULLSCREEN) {
            Logger.printDebug(() -> "Ignoring dialogType " + dialogName);
            return false;
        }

        // Image in community post in fullscreen is not filtered
        final boolean isException = bytes != null &&
                exception.check(bytes).isFiltered();

        if (isException) {
            Logger.printDebug(() -> "Ignoring exception");
        } else {
            Logger.printDebug(() -> "Blocked fullscreen ads");
        }

        return !isException;
    }

    public static void hideFullscreenAds(View view) {
        hideViewBy0dpUnderCondition(
                hideFullscreenAdsEnabled,
                view
        );
    }

    private enum DialogType {
        NULL(0),
        ALERT(1),
        FULLSCREEN(2),
        LAYOUT_FULLSCREEN(3);

        private final int type;

        DialogType(int type) {
            this.type = type;
        }

        private static DialogType getDialogType(int type) {
            for (DialogType val : values())
                if (type == val.type) return val;

            return DialogType.NULL;
        }
    }

}