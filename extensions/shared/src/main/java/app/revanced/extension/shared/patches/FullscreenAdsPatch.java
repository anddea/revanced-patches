package app.revanced.extension.shared.patches;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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

    /**
     * Whether the last built dialog contains an ad
     */
    private static boolean isFullscreenAds = false;

    /**
     * {@link Dialog#dismiss()} can be used after {@link Dialog#show()}
     * When {@link Dialog#show()} is invoked, byte buffer register may not exist
     * (byte buffer register has already been assigned to another value)
     * <p>
     * Therefore, make sure that the dialog contains the ads at the beginning of the Method
     *
     * @param bytes proto buffer array
     * @param type  dialog type (similar to {@link Enum#ordinal()})
     */
    public static void checkDialog(byte[] bytes, int type) {
        if (!HIDE_FULLSCREEN_ADS) {
            return;
        }
        final DialogType dialogType = DialogType.getDialogType(type);
        final String dialogName = dialogType.name();

        // The dialog type of a fullscreen dialog is always {@code DialogType.FULLSCREEN}
        if (dialogType != DialogType.FULLSCREEN) {
            Logger.printDebug(() -> "Ignoring dialogType: " + dialogName);
            isFullscreenAds = false;
            return;
        }

        // Whether dialog is community post image (not ads)
        final boolean isException = bytes != null &&
                exception.check(bytes).isFiltered();

        if (isException) {
            Logger.printDebug(() -> "Ignoring exception");
        }
        isFullscreenAds = !isException;
    }

    /**
     * Called after {@link #checkDialog(byte[], int)}
     *
     * @param customDialog Custom dialog which bound by litho
     *                     Can be cast as {@link Dialog} or {@link DialogInterface}
     */
    public static void dismissDialog(Object customDialog) {
        if (isFullscreenAds && customDialog instanceof Dialog dialog) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.height = 0;
                params.width = 0;

                // Change the size of dialog to 0.
                window.setAttributes(params);

                // Disable dialog's background dim.
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // Hide DecorView.
                View decorView = window.getDecorView();
                decorView.setVisibility(View.GONE);

                // Dismiss dialog.
                dialog.dismiss();

                if (BaseSettings.DEBUG_TOAST_ON_ERROR.get()) {
                    Utils.showToastShort(str("revanced_fullscreen_ads_closed_toast"));
                }
            }
        } else {
            Logger.printDebug(() -> "customDialog type: " + customDialog.getClass().getName());
        }
    }

    /**
     * Injection point.
     * Invoke only in old clients.
     */
    public static void hideFullscreenAds(View view) {
        hideViewBy0dpUnderCondition(
                HIDE_FULLSCREEN_ADS,
                view
        );
    }

    /**
     * YouTube and YouTube Music do not have Enum class for DialogType,
     * but they have structures similar to Enum
     * It can also be replaced by {@link Enum#ordinal()}
     */
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