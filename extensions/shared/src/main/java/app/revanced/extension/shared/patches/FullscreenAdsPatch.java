package app.revanced.extension.shared.patches;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.app.Dialog;
import android.view.View;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class FullscreenAdsPatch {
    private static final boolean HIDE_FULLSCREEN_ADS =
            BaseSettings.HIDE_FULLSCREEN_ADS.get();
    private static final ByteArrayFilterGroup exception =
            new ByteArrayFilterGroup(
                    null,
                    "post_image_lightbox.eml" // Community post image in fullscreen
            );

    private static boolean isFullscreenAds = false;

    public static void checkDialog(byte[] bytes, int type) {
        if (!HIDE_FULLSCREEN_ADS) {
            return;
        }
        final DialogType dialogType = DialogType.getDialogType(type);
        final String dialogName = dialogType.name();

        // The dialog type of a fullscreen dialog is always {@code DialogType.FULLSCREEN}
        if (dialogType != DialogType.FULLSCREEN) {
            Logger.printDebug(() -> "Ignoring dialogType " + dialogName);
            isFullscreenAds = false;
            return;
        }

        // Image in community post in fullscreen is not filtered
        final boolean isException = bytes != null &&
                exception.check(bytes).isFiltered();

        if (isException) {
            Logger.printDebug(() -> "Ignoring exception");
        }
        isFullscreenAds = !isException;
    }

    public static void dismissDialog(Object customDialog) {
        if (!isFullscreenAds) {
            return;
        }
        if (customDialog instanceof Dialog dialog) {
            dialog.hide();
            // Perhaps this is not necessary.
            dialog.dismiss();
            if (BaseSettings.ENABLE_DEBUG_LOGGING.get()) {
                Utils.showToastShort(str("revanced_fullscreen_ads_closed_toast"));
            }
        } else {
            Logger.printDebug(() -> "customDialog type: " + customDialog.getClass().getName());
        }
    }

    public static void hideFullscreenAds(View view) {
        hideViewBy0dpUnderCondition(
                HIDE_FULLSCREEN_ADS,
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
            for (DialogType dialogType : values())
                if (type == dialogType.type) return dialogType;

            return DialogType.NULL;
        }
    }

}