package app.revanced.extension.youtube.patches.general;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.BooleanUtils;

import java.util.Objects;

import app.revanced.extension.shared.utils.PackageUtils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class LayoutSwitchPatch {

    public enum FormFactor {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL(null, null, null),
        SMALL_FORM_FACTOR(1, null, TRUE),
        SMALL_FORM_FACTOR_WIDTH_DP(1, 480, TRUE),
        LARGE_FORM_FACTOR(2, null, FALSE),
        LARGE_FORM_FACTOR_WIDTH_DP(2, 600, FALSE);

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

    private static final FormFactor FORM_FACTOR = Settings.CHANGE_LAYOUT.get();

    public static int getFormFactor(int original) {
        Integer formFactorType = FORM_FACTOR.formFactorType;
        return formFactorType == null
                ? original
                : formFactorType;
    }

    public static int getWidthDp(int original) {
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

}
