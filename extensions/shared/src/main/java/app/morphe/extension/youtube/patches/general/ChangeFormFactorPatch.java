package app.morphe.extension.youtube.patches.general;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_AUTOMOTIVE;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_WATCH;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.util.Objects;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.PackageUtils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.PlayerType;
import app.morphe.extension.youtube.shared.RootView;

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

    private static final int smallestScreenWidthDp = PackageUtils.getSmallestScreenWidthDp();
    private static int clientFormFactorOrdinal = -1;

    private static int getClientFormFactorOrdinal() {
        if (clientFormFactorOrdinal == -1) {
            if (IS_WATCH) {
                clientFormFactorOrdinal = 4; // WEARABLE_FORM_FACTOR
            } else if (IS_AUTOMOTIVE) {
                clientFormFactorOrdinal = 3; // AUTOMOTIVE_FORM_FACTOR
            } else {
                if (smallestScreenWidthDp >= 600) {
                    clientFormFactorOrdinal = 2; // LARGE_FORM_FACTOR
                } else if (smallestScreenWidthDp > 0) {
                    clientFormFactorOrdinal = 1; // SMALL_FORM_FACTOR
                } else {
                    clientFormFactorOrdinal = 0; // UNKNOWN_FORM_FACTOR
                }
            }
        }

        return clientFormFactorOrdinal;
    }

    /**
     * Toolbar buttons (including the YouTube logo) and navigation bar buttons depend on the
     * '<a href="https://www.youtube.com/youtubei/v1/guide">'/guide' endpoint</a>' requests.
     * <p>
     * Therefore, the patch works if the 'clientFormFactor' value is spoofed only in '/guide' endpoint requests.
     *
     * @return clientFormFactor (ordinal).
     */
    public static int getFormFactor() {
        int original = getClientFormFactorOrdinal();

        return FORM_FACTOR_TYPE == null || USING_AUTOMOTIVE_TYPE
                // When 'USING_AUTOMOTIVE_TYPE' is true, the 'Shorts' button in the navigation bar is replaced with the 'Explore' button.
                // To prevent this, the original clientFormFactorOrdinal is used when 'USING_AUTOMOTIVE_TYPE' is true.
                ? original
                : FORM_FACTOR_TYPE;
    }

    /**
     * Injection point.
     */
    public static int getFormFactor(int original) {
        if (FORM_FACTOR_TYPE == null) return original;

        if (USING_AUTOMOTIVE_TYPE) {
            // Do not change if the player is opening or is opened,
            // otherwise the video description cannot be opened.
            PlayerType current = PlayerType.getCurrent();
            if (current.isMaximizedOrFullscreenOrSliding()) {
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
}