package app.revanced.extension.music.patches.general;

import static app.revanced.extension.music.utils.ExtendedUtils.isSpoofingToLessThan;
import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import app.revanced.extension.music.settings.Settings;

/**
 * @noinspection ALL
 */
@SuppressWarnings("unused")
public class GeneralPatch {

    // region [Change start page] patch

    public static String changeStartPage(final String browseId) {
        if (!browseId.equals("FEmusic_home"))
            return browseId;

        return Settings.CHANGE_START_PAGE.get();
    }

    // endregion

    // region [Disable dislike redirection] patch

    public static boolean disableDislikeRedirection() {
        return Settings.DISABLE_DISLIKE_REDIRECTION.get();
    }

    // endregion

    // region [Enable landscape mode] patch

    public static boolean enableLandScapeMode(boolean original) {
        return Settings.ENABLE_LANDSCAPE_MODE.get() || original;
    }

    // endregion

    // region [Hide layout components] patch

    public static int hideCastButton(int original) {
        return Settings.HIDE_CAST_BUTTON.get() ? View.GONE : original;
    }

    public static void hideCastButton(View view) {
        hideViewBy0dpUnderCondition(Settings.HIDE_CAST_BUTTON.get(), view);
    }

    public static void hideCategoryBar(View view) {
        hideViewBy0dpUnderCondition(Settings.HIDE_CATEGORY_BAR.get(), view);
    }

    public static boolean hideFloatingButton() {
        return Settings.HIDE_FLOATING_BUTTON.get();
    }

    public static boolean hideTapToUpdateButton() {
        return Settings.HIDE_TAP_TO_UPDATE_BUTTON.get();
    }

    public static boolean hideHistoryButton(boolean original) {
        return !Settings.HIDE_HISTORY_BUTTON.get() && original;
    }

    public static void hideNotificationButton(View view) {
        if (view.getParent() instanceof ViewGroup viewGroup) {
            hideViewBy0dpUnderCondition(Settings.HIDE_NOTIFICATION_BUTTON, viewGroup);
        }
    }

    public static boolean hideSoundSearchButton(boolean original) {
        if (!Settings.SETTINGS_INITIALIZED.get()) {
            return original;
        }
        return !Settings.HIDE_SOUND_SEARCH_BUTTON.get();
    }

    public static void hideVoiceSearchButton(ImageView view, int visibility) {
        final int finalVisibility = Settings.HIDE_VOICE_SEARCH_BUTTON.get()
                ? View.GONE
                : visibility;

        view.setVisibility(finalVisibility);
    }

    public static void hideTasteBuilder(View view) {
        view.setVisibility(View.GONE);
    }


    // endregion

    // region [Hide overlay filter] patch

    public static void disableDimBehind(Window window) {
        if (window != null) {
            // Disable AlertDialog's background dim.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    // endregion

    // region [Remove viewer discretion dialog] patch

    /**
     * Injection point.
     * <p>
     * The {@link AlertDialog#getButton(int)} method must be used after {@link AlertDialog#show()} is called.
     * Otherwise {@link AlertDialog#getButton(int)} method will always return null.
     * https://stackoverflow.com/a/4604145
     * <p>
     * That's why {@link AlertDialog#show()} is absolutely necessary.
     * Instead, use two tricks to hide Alertdialog.
     * <p>
     * 1. Change the size of AlertDialog to 0.
     * 2. Disable AlertDialog's background dim.
     * <p>
     * This way, AlertDialog will be completely hidden,
     * and {@link AlertDialog#getButton(int)} method can be used without issue.
     */
    public static void confirmDialog(final AlertDialog dialog) {
        if (!Settings.REMOVE_VIEWER_DISCRETION_DIALOG.get()) {
            return;
        }

        // This method is called after AlertDialog#show(),
        // So we need to hide the AlertDialog before pressing the possitive button.
        final Window window = dialog.getWindow();
        final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (window != null && button != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.height = 0;
            params.width = 0;

            // Change the size of AlertDialog to 0.
            window.setAttributes(params);

            // Disable AlertDialog's background dim.
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

            button.callOnClick();
        }
    }

    // endregion

    // region [Restore old style library shelf] patch

    public static String restoreOldStyleLibraryShelf(final String browseId) {
        final boolean oldStyleLibraryShelfEnabled =
                Settings.RESTORE_OLD_STYLE_LIBRARY_SHELF.get() || isSpoofingToLessThan("5.38.00");
        return oldStyleLibraryShelfEnabled && browseId.equals("FEmusic_library_landing")
                ? "FEmusic_liked"
                : browseId;
    }

    // endregion

    // region [Spoof app version] patch

    public static String getVersionOverride(String version) {
        if (!Settings.SPOOF_APP_VERSION.get())
            return version;

        return Settings.SPOOF_APP_VERSION_TARGET.get();
    }

    // endregion

}
