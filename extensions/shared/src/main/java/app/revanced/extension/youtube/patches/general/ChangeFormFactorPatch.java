package app.revanced.extension.youtube.patches.general;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.revanced.extension.youtube.shared.NavigationBar.NavigationButton;

import android.view.View;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.util.Objects;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.PackageUtils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.PlayerType;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public class ChangeFormFactorPatch {

    public enum FormFactor {
        /**
         * Unmodified, and same as un-patched.
         */
        DEFAULT(null, null, null),
        /**
         * <pre>
         * Some changes include:
         * - Explore tab is present.
         * - watch history is missing.
         * - feed thumbnails fade in.
         */
        UNKNOWN(0, null, null),
        SMALL(1, null, TRUE),
        SMALL_WIDTH_DP(1, 480, TRUE),
        LARGE(2, null, FALSE),
        LARGE_WIDTH_DP(2, 600, FALSE),
        /**
         * Cars with 'Google built-in'.
         * Layout seems identical to {@link #UNKNOWN}
         * even when using an Android Automotive device.
         */
        AUTOMOTIVE(3, null, null),
        WEARABLE(4, null, null);

        @Nullable
        final Integer formFactorType;

        @Nullable
        final Integer widthDp;

        @Nullable
        final Boolean setMinimumDp;


        FormFactor(@Nullable Integer formFactorType, @Nullable Integer widthDp, @Nullable Boolean setMinimumDp) {
            this.formFactorType = formFactorType;
            this.widthDp = widthDp;
            this.setMinimumDp = setMinimumDp;
        }

        private boolean setMinimumDp() {
            return BooleanUtils.isTrue(setMinimumDp);
        }
    }

    private static final FormFactor FORM_FACTOR = Settings.CHANGE_FORM_FACTOR.get();
    @Nullable
    private static final Integer FORM_FACTOR_TYPE = FORM_FACTOR.formFactorType;
    private static final boolean USING_AUTOMOTIVE_TYPE = Objects.requireNonNull(
            FormFactor.AUTOMOTIVE.formFactorType).equals(FORM_FACTOR_TYPE);

    /**
     * Injection point.
     */
    public static int getFormFactor(int original) {
        if (FORM_FACTOR_TYPE == null) return original;

        if (USING_AUTOMOTIVE_TYPE) {
            // Do not change if the player is opening or is opened,
            // otherwise the video description cannot be opened.
            PlayerType current = PlayerType.getCurrent();
            if (current.isMaximizedOrFullscreen() || current == PlayerType.WATCH_WHILE_SLIDING_MINIMIZED_MAXIMIZED) {
                Logger.printDebug(() -> "Using original form factor for player");
                return original;
            }
            if (!RootView.isSearchBarActive()) {
                // Automotive type shows error 400 when opening a channel page and using some explore tab.
                // This is a bug in unpatched YouTube that occurs on actual Android Automotive devices.
                // Work around the issue by using the original form factor if not in search and the
                // navigation back button is present.
                if (RootView.isBackButtonVisible()) {
                    Logger.printDebug(() -> "Using original form factor, as back button is visible without search present");
                    return original;
                }

                // Do not change library tab otherwise watch history is hidden.
                // Do this check last since the current navigation button is required.
                if (NavigationButton.getSelectedNavigationButton() == NavigationButton.LIBRARY) {
                    return original;
                }
            }
        }

        return FORM_FACTOR_TYPE;
    }

    /**
     * Injection point.
     */
    public static int getWidthDp(int original) {
        if (FORM_FACTOR_TYPE == null) return original;
        Integer widthDp = FORM_FACTOR.widthDp;
        if (widthDp == null) {
            return original;
        }
        final int smallestScreenWidthDp = PackageUtils.getSmallestScreenWidthDp();
        if (smallestScreenWidthDp == 0) {
            return original;
        }
        return FORM_FACTOR.setMinimumDp()
                ? Math.min(smallestScreenWidthDp, widthDp)
                : Math.max(smallestScreenWidthDp, widthDp);
    }

    public static boolean phoneLayoutEnabled() {
        return Objects.equals(FORM_FACTOR.formFactorType, 1);
    }

    public static boolean tabletLayoutEnabled() {
        return Objects.equals(FORM_FACTOR.formFactorType, 2);
    }

    /**
     * Injection point.
     */
    public static void navigationTabCreated(NavigationButton button, View tabView) {
        // On first startup of the app the navigation buttons are fetched and updated.
        // If the user immediately opens the 'You' or opens a video, then the call to
        // update the navigtation buttons will use the non automotive form factor
        // and the explore tab is missing.
        // Fixing this is not so simple because of the concurrent calls for the player and You tab.
        // For now, always hide the explore tab.
        if (USING_AUTOMOTIVE_TYPE && button == NavigationButton.EXPLORE) {
            tabView.setVisibility(View.GONE);
        }
    }
}