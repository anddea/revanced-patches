/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.general;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_20_31_OR_GREATER;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_AUTOMOTIVE;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_WATCH;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.util.List;
import java.util.Objects;

import app.morphe.extension.shared.settings.Setting;
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
    private static final boolean TABLET_LAYOUT_IN_PLAYER =
            IS_20_31_OR_GREATER
                    && FORM_FACTOR != FormFactor.LARGE
                    && FORM_FACTOR != FormFactor.LARGE_WIDTH_DP
                    && Settings.TABLET_LAYOUT_IN_PLAYER.get();

    public static final class TabletLayoutInPlayerAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.CHANGE_FORM_FACTOR.get() != FormFactor.LARGE
                    && Settings.CHANGE_FORM_FACTOR.get() != FormFactor.LARGE_WIDTH_DP;
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.CHANGE_FORM_FACTOR);
        }
    }

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
        if (TABLET_LAYOUT_IN_PLAYER) {
            // Keep the selected form factor for the guide endpoint so navigation remains unchanged.
            return Objects.requireNonNull(FormFactor.LARGE.formFactorType);
        }

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

    /**
     * If the form factor is spoofed as a tablet, {@code shelfRenderer} is used instead of
     * {@code itemSectionRenderer}. Sometimes YouTube still parses it as an item section; skip
     * parsing when that renderer index is invalid to avoid a crash.
     */
    public static boolean checkItemSectionRenderer(List<?> list, int listIndex) {
        return list != null && !list.isEmpty() && listIndex >= 0 && listIndex < list.size();
    }
}
