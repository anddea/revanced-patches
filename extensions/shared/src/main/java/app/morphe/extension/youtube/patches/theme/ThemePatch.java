/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2524
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.theme;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.view.View;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public final class ThemePatch {
    public static final String DEFAULT_DARK_THEME = "stock";
    public static final String DEFAULT_LIGHT_THEME = "white";
    public static final String DEFAULT_DARK_THEME_CUSTOM_COLOR = "#FF000000";
    public static final String DEFAULT_LIGHT_THEME_CUSTOM_COLOR = "#FFFFFFFF";
    public static final String DEFAULT_NOTIFICATION_DOT_COLOR = "#FFFF0000";

    private static final String RUNTIME_LIGHT_THEME_COLOR = "morphe_runtime_light_theme_color";
    private static final String SPLASH_THEME_DARK_PREFIX = "morphe_theme_splash_dark_";
    private static final String SPLASH_THEME_LIGHT_PREFIX = "morphe_theme_splash_light_";
    private static final String SPLASH_THEME_NO_ICON_SUFFIX = "_no_icon";
    private static final int PRECOMPILED_THEME_MCC = 801;
    private static final int RUNTIME_THEME_MCC = 802;
    private static final int RUNTIME_THEME_CHANGE_FOREGROUND_MNC = 1;
    private static final int RUNTIME_THEME_DARK_BACKGROUND_MNC = 2;
    private static final int RUNTIME_THEME_LIGHT_BACKGROUND_MNC = 3;
    private static final String[] PRECOMPILED_DARK_THEME_KEYS = {
            "stock", "amoled_black", "material_you_neutral", "material_you_primary",
            "material_you_secondary", "material_you_tertiary", "modern_youtube",
            "classic_youtube", "catppuccin_mocha", "dark_pink", "dark_blue", "dark_green",
            "dark_yellow", "dark_orange", "dark_red",
    };
    private static final String[] PRECOMPILED_LIGHT_THEME_KEYS = {
            "white", "material_you_neutral", "material_you_primary", "material_you_secondary",
            "material_you_tertiary", "catppuccin_latte", "light_pink", "light_blue",
            "light_green", "light_yellow", "light_orange", "light_red", "pale_blue",
            "pale_green", "pale_yellow",
    };
    /** Offset between the full, dark-background-only, and light-background-only variants. */
    private static final int PRECOMPILED_THEME_PAIR_COUNT =
            PRECOMPILED_DARK_THEME_KEYS.length * PRECOMPILED_LIGHT_THEME_KEYS.length;
    private static final int STOCK_DARK_THEME_MAIN_COLOR_INDEX = 5;
    private static final int[] STOCK_DARK_THEME_COLORS = {
            0xFF282828, 0xFF212121, 0xF2212121, 0xFA212121, 0xFF181818,
            0xFF0F0F0F, 0xFF030303, 0xFF131313, 0xFF303030,
            0xFF000000, 0xFF424242, 0xFF212121, 0x99282828, 0xCC000000,
            0x99000000,
    };
    private static final int STOCK_DARK_THEME_STATUS_BAR_COLOR_INDEX = 7;

    /**
     * Applies both palettes before YouTube inflates its first layout, then persists the next
     * starting-window theme after the host reports its forced appearance. Android 8–10 select
     * precompiled resource configurations, while Android 11+ replaces the stable resources.
     */
    public static void setTheme(Activity activity) {
        setTheme((Context) activity);
        persistSplashScreenThemeWhenResolved(activity);
    }

    /**
     * Installs the resource overlay from Application.onCreate. The overlay is ready before the
     * Activity inflates its first layout; Android 12+ may already have created the starting window,
     * so the Activity overload also selects a stable splash theme with the chosen preset color.
     */
    public static void setTheme(Context context) {
        BaseThemeUtils.setChangeForegroundColor(Settings.THEME_COLOR_CHANGE_FOREGROUND.get());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            try {
                PrecompiledResourcePalette.install(context);
            } catch (Exception ex) {
                Logger.printException(() -> "Failed to apply precompiled YouTube theme", ex);
            }
            return;
        }

        try {
            int[] darkColors = getSelectedDarkColors(context);
            int lightColor = getSelectedLightColor(context);
            int lightOpacity70Color = getSelectedLightColorWithOpacity70(context);
            BaseThemeUtils.setThemeDarkColor(darkColors[STOCK_DARK_THEME_MAIN_COLOR_INDEX]);
            BaseThemeUtils.setThemeLightColor(lightColor);
            RuntimeResourceOverlay.install(context, darkColors, lightColor, lightOpacity70Color);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to install runtime YouTube theme", ex);
        }
    }

    /**
     * Applies the selected palette to the separate Activity used by RVX settings. Runtime resource
     * loaders belong to a Resources instance, so a settings Activity created after YouTube's main
     * Activity must receive the loader before its theme and views are inflated.
     */
    public static void applyToSettingsActivity(Activity activity) {
        if (ResourceUtils.getIdentifier(RUNTIME_LIGHT_THEME_COLOR, ResourceType.COLOR, activity) == 0) {
            // The Theme patch was not included, so the runtime resource does not exist.
            return;
        }
        setTheme((Context) activity);
    }

    /**
     * Selects the palette for YouTube's resolved appearance. YouTube can force light or dark mode
     * independently of Android's night configuration, so resource foregrounds must follow the
     * host theme result instead of a {@code night} qualifier.
     *
     * @param value YouTube's resolved light/dark appearance enum.
     */
    public static void onAppThemeResolved(Enum<?> value) {
        BaseThemeUtils.updateLightDarkModeStatus(value);
        final boolean dark = BaseThemeUtils.isDarkModeEnabled();
        if (Settings.THEME_LAST_USED_DARK_MODE.get() != dark) {
            Settings.THEME_LAST_USED_DARK_MODE.save(dark);
        }
        Context context = Utils.getContext();
        if (context == null) return;

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                PrecompiledResourcePalette.install(context);
            } else {
                RuntimeResourceOverlay.select(context);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to select resolved YouTube theme", ex);
        }
    }

    /**
     * Persists the selected preset as Android's starting-window theme for the next launch. The
     * style contains a concrete color because Android resolves it outside the app process, before
     * the runtime resource overlay exists. The API 31 reference is isolated so this class remains
     * loadable on older Android versions.
     */
    private static void persistSplashScreenThemeWhenResolved(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }

        // Android draws the current starting window before the process begins. Persist the next
        // one only after YouTube reports its forced appearance, which may differ from device mode.
        View decor = activity.getWindow().getDecorView();
        decor.post(new Runnable() {
            private int remainingFrames = 120;

            @Override
            public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (BaseThemeUtils.isAppThemeResolved()) {
                    applySplashScreenTheme(activity);
                } else if (--remainingFrames > 0) {
                    decor.postOnAnimation(this);
                }
            }
        });
    }

    private static void applySplashScreenTheme(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        try {
            final boolean dark = BaseThemeUtils.isDarkModeEnabled();
            final String themePrefix = dark
                    ? SPLASH_THEME_DARK_PREFIX
                    : SPLASH_THEME_LIGHT_PREFIX;
            final String fallbackTheme = dark ? "stock" : "white";
            final String selectedTheme = dark
                    ? Settings.DARK_THEME.get()
                    : Settings.LIGHT_THEME.get();
            final boolean useCustomSplashAnimation =
                    !"original".equals(Settings.CUSTOM_BRANDING_ICON.get())
                            && !isSplashAnimationDisabled();
            final String splashIconSuffix = useCustomSplashAnimation
                    ? SPLASH_THEME_NO_ICON_SUFFIX
                    : "";
            String themeName = themePrefix + selectedTheme + splashIconSuffix;
            int themeId = ResourceUtils.getIdentifier(themeName, ResourceType.STYLE, activity);
            if (themeId == 0) {
                themeName = themePrefix + fallbackTheme + splashIconSuffix;
                themeId = ResourceUtils.getIdentifier(themeName, ResourceType.STYLE, activity);
            }
            if (themeId == 0) {
                final String missingThemeName = themeName;
                Logger.printDebug(() -> "Splash theme not found: " + missingThemeName);
                return;
            }
            SplashScreenBridge.apply(activity, themeId);
        } catch (Exception ex) {
            Logger.printException(() -> "Failed to apply selected splash theme", ex);
        }
    }

    /** Keeps Android 12 splash-screen classes out of the verifier path on older devices. */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final class SplashScreenBridge {
        private SplashScreenBridge() {
        }

        private static void apply(Activity activity, int themeId) {
            activity.getSplashScreen().setSplashScreenTheme(themeId);
        }
    }

    /** Returns whether YouTube's original dark resource palette should be left unchanged. */
    public static boolean isStockDarkTheme() {
        return "stock".equals(Settings.DARK_THEME.get());
    }

    private static int[] getSelectedDarkColors(Context context) {
        if (isStockDarkTheme()) {
            int[] colors = STOCK_DARK_THEME_COLORS.clone();
            if (isDisableTranslucentStatusBar()) {
                int statusBarColor = colors[STOCK_DARK_THEME_MAIN_COLOR_INDEX];
                colors[STOCK_DARK_THEME_STATUS_BAR_COLOR_INDEX] = Color.argb(
                        0xFF,
                        Color.red(statusBarColor),
                        Color.green(statusBarColor),
                        Color.blue(statusBarColor)
                );
            }
            return colors;
        }

        int[] colors = new int[STOCK_DARK_THEME_COLORS.length];
        int selectedColor = getSelectedDarkColor(context);
        for (int i = 0; i < colors.length; i++) {
            colors[i] = Color.argb(
                    isDisableTranslucentStatusBar()
                            && i == STOCK_DARK_THEME_STATUS_BAR_COLOR_INDEX
                            ? 0xFF
                            : Color.alpha(STOCK_DARK_THEME_COLORS[i]),
                    Color.red(selectedColor),
                    Color.green(selectedColor),
                    Color.blue(selectedColor)
            );
        }
        return colors;
    }

    private static int getSelectedDarkColor(Context context) {
        final int patchedColor = STOCK_DARK_THEME_COLORS[STOCK_DARK_THEME_MAIN_COLOR_INDEX];
        return switch (Settings.DARK_THEME.get()) {
            case "amoled_black" -> Color.BLACK;
            case "material_you_neutral" -> getSystemColor(context, "system_neutral1_900", patchedColor);
            case "material_you_primary" -> getSystemColor(context, "system_accent1_800", patchedColor);
            case "material_you_secondary" -> getSystemColor(context, "system_accent2_800", patchedColor);
            case "material_you_tertiary" -> getSystemColor(context, "system_accent3_800", patchedColor);
            case "modern_youtube" -> 0xFF0F0F0F;
            case "classic_youtube" -> 0xFF212121;
            case "catppuccin_mocha" -> 0xFF181825;
            case "dark_pink" -> 0xFF290025;
            case "dark_blue" -> 0xFF001029;
            case "dark_green" -> 0xFF002905;
            case "dark_yellow" -> 0xFF282900;
            case "dark_orange" -> 0xFF291800;
            case "dark_red" -> 0xFF290000;
            case "custom" -> ResourceUtils.getColor(Settings.DARK_THEME_CUSTOM_COLOR.get(), patchedColor);
            default -> patchedColor;
        };
    }

    private static int getSelectedLightColor(Context context) {
        final int patchedColor = ResourceUtils.getColor(RUNTIME_LIGHT_THEME_COLOR);
        return switch (Settings.LIGHT_THEME.get()) {
            case "white" -> Color.WHITE;
            case "material_you_neutral" -> getSystemColor(context, "system_neutral1_100", patchedColor);
            case "material_you_primary" -> getSystemColor(context, "system_accent1_200", patchedColor);
            case "material_you_secondary" -> getSystemColor(context, "system_accent2_200", patchedColor);
            case "material_you_tertiary" -> getSystemColor(context, "system_accent3_200", patchedColor);
            case "catppuccin_latte" -> 0xFFE6E9EF;
            case "light_pink" -> 0xFFFCCFF3;
            case "light_blue" -> 0xFFD1E0FF;
            case "light_green" -> 0xFFCCFFCC;
            case "light_yellow" -> 0xFFFDFFCC;
            case "light_orange" -> 0xFFFFE6CC;
            case "light_red" -> 0xFFFFD6D6;
            case "pale_blue" -> 0xFFD4FFF8;
            case "pale_green" -> 0xFFD1FFCC;
            case "pale_yellow" -> 0xFFFFE9AA;
            case "custom" -> ResourceUtils.getColor(Settings.LIGHT_THEME_CUSTOM_COLOR.get(), patchedColor);
            default -> patchedColor;
        };
    }

    /** Keeps the translucent light overlay while applying the selected light-theme color. */
    private static int getSelectedLightColorWithOpacity70(Context context) {
        int color = getSelectedLightColor(context);
        return Color.argb(0xB3, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Applies the selected theme only to YouTube's status-bar fallback. The shared translucent
     * resources remain stock because the player also uses them while controls are visible.
     */
    public static int getStatusBarColor(int color) {
        int selectedColor = BaseThemeUtils.isDarkModeEnabled() && isStockDarkTheme()
                ? color
                : BaseThemeUtils.getAppBackgroundColor();
        return Color.argb(
                isDisableTranslucentStatusBar() ? 0xFF : Color.alpha(color),
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
        );
    }

    /**
     * Injection point for a new-content indicator declared as a view stub. Material You uses the
     * selected system palette instead of the app's red indicator.
     */
    public static void onNewContentIndicator(ViewStub stub) {
        try {
            stub.setOnInflateListener((inflatedStub, view) -> keepIndicatorColor(view));
        } catch (Exception ex) {
            Logger.printException(() -> "onNewContentIndicator failure", ex);
        }
    }

    /**
     * Injection point for a new-content indicator declared as a view of its own instead of a stub.
     */
    public static void onNewContentIndicator(View indicator) {
        try {
            keepIndicatorColor(indicator);
        } catch (Exception ex) {
            Logger.printException(() -> "onNewContentIndicator failure", ex);
        }
    }

    private static void keepIndicatorColor(View view) {
        keepIndicatorColor(view, getIndicatorColor(view.getContext()));
    }

    private static void keepIndicatorColor(View view, Integer color) {
        if (color == null) {
            return;
        }

        setIndicatorColor(view, color);

        // YouTube applies its own background after the indicator is shown. Applying the selected
        // color before every draw prevents an intermediate frame with the app's red color.
        ViewTreeObserver.OnPreDrawListener listener = () -> {
            setIndicatorColor(view, color);
            return true;
        };
        view.getViewTreeObserver().addOnPreDrawListener(listener);

        // The observer belongs to the window, so remove the listener when a discarded indicator
        // is detached instead of keeping it alive for the lifetime of the window.
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View attached) {
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View detached) {
                detached.getViewTreeObserver().removeOnPreDrawListener(listener);
            }
        });
    }

    private static void setIndicatorColor(View view, int color) {
        Drawable background = view.getBackground();

        if (background instanceof GradientDrawable shape) {
            ColorStateList fill = shape.getColor();
            if (fill == null || fill.getDefaultColor() != color) {
                ((GradientDrawable) shape.mutate()).setColor(color);
            }
        }

        if (view instanceof TextView count && isMaterialYouTheme()) {
            final int textColor = getIndicatorTextColor(view.getContext());
            if (count.getCurrentTextColor() != textColor) {
                count.setTextColor(textColor);
            }
        }
    }

    /**
     * Returns the dynamic Material You color or the configured color for other themes.
     */
    @Nullable
    public static Integer getIndicatorColor(Context context) {
        try {
            final boolean dark = isDarkTheme();
            final String selectedTheme = dark
                    ? Settings.DARK_THEME.get()
                    : Settings.LIGHT_THEME.get();
            if (selectedTheme.startsWith("material_you_")) {
                return getMaterialYouIndicatorColor(context, dark);
            }

            return ResourceUtils.getColor(
                    Settings.NOTIFICATION_DOT_COLOR.get(),
                    Color.parseColor(DEFAULT_NOTIFICATION_DOT_COLOR));
        } catch (Exception ex) {
            Logger.printException(() -> "getIndicatorColor failure", ex);
            return null;
        }
    }

    private static Integer getMaterialYouIndicatorColor(Context context, boolean dark) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return null;
        }

        return context.getColor(dark
                ? android.R.color.system_accent1_100
                : android.R.color.system_accent1_200);
    }

    private static boolean isMaterialYouTheme() {
        final String selectedTheme = isDarkTheme()
                ? Settings.DARK_THEME.get()
                : Settings.LIGHT_THEME.get();
        return selectedTheme.startsWith("material_you_");
    }

    /** Returns text that remains readable on the selected Material You indicator color. */
    public static int getIndicatorTextColor(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.getColor(android.R.color.system_neutral1_900);
        }

        // Never reached: an indicator color exists only on Android 12 and newer.
        return Color.BLACK;
    }

    private static boolean isDisableTranslucentStatusBar() {
        return Settings.DISABLE_TRANSLUCENT_STATUS_BAR.get();
    }

    /**
     * Returns YouTube's appearance, falling back to the last resolved value during process start.
     * The device mode is not authoritative because YouTube has an independent appearance setting.
     */
    private static boolean isDarkTheme() {
        return BaseThemeUtils.isAppThemeResolved()
                ? BaseThemeUtils.isDarkModeEnabled()
                : Settings.THEME_LAST_USED_DARK_MODE.get();
    }

    @SuppressLint("DiscouragedApi")
    private static int getSystemColor(Context context, String name, int fallback) {
        int identifier = Resources.getSystem().getIdentifier(name, "color", "android");
        if (identifier == 0) return fallback;
        try {
            return context.getResources().getColor(identifier, context.getTheme());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Selects concrete resource variants using synthetic MCC/MNC qualifiers. These fields are
     * private to this process's resource configuration and avoid unsupported AssetManager APIs.
     */
    private static final class PrecompiledResourcePalette {
        private PrecompiledResourcePalette() {
        }

        static synchronized void install(Context context) {
            Resources resources = context.getResources();
            int darkIndex = indexFor(PRECOMPILED_DARK_THEME_KEYS, Settings.DARK_THEME.get());
            int lightIndex = indexFor(PRECOMPILED_LIGHT_THEME_KEYS, Settings.LIGHT_THEME.get());
            int themedMnc = darkIndex * PRECOMPILED_LIGHT_THEME_KEYS.length + lightIndex + 1;
            if (!Settings.THEME_COLOR_CHANGE_FOREGROUND.get()) {
                themedMnc += PRECOMPILED_THEME_PAIR_COUNT *
                        (isDarkTheme() ? 1 : 2);
            }
            apply(resources, themedMnc);

            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null) {
                Resources applicationResources = applicationContext.getResources();
                if (applicationResources != resources) {
                    apply(applicationResources, themedMnc);
                }
            }

            // Refresh extension and Litho colors from the now-selected concrete resources.
            BaseThemeUtils.setThemeColor();
        }

        private static void apply(Resources resources, int themedMnc) {
            Configuration current = resources.getConfiguration();
            if (current.mcc == ThemePatch.PRECOMPILED_THEME_MCC && current.mnc == themedMnc) return;

            Configuration themed = new Configuration(current);
            themed.mcc = ThemePatch.PRECOMPILED_THEME_MCC;
            themed.mnc = themedMnc;
            resources.updateConfiguration(themed, resources.getDisplayMetrics());
        }

        private static int indexFor(String[] keys, String selection) {
            for (int i = 0; i < keys.length; i++) {
                if (keys[i].equals(selection)) {
                    return i;
                }
            }
            return 0;
        }
    }

    /** API 30 resource-loader references stay isolated from Android 8–10 class verification. */
    private static final class RuntimeResourceOverlay {
        private static final byte[][] DARK_PLACEHOLDERS = {
                {0x50, 0x34, 0x12, (byte) 0xFF}, {0x51, 0x34, 0x12, (byte) 0xFF},
                {0x52, 0x34, 0x12, (byte) 0xFF}, {0x53, 0x34, 0x12, (byte) 0xFF},
                {0x54, 0x34, 0x12, (byte) 0xFF}, {0x55, 0x34, 0x12, (byte) 0xFF},
                {0x56, 0x34, 0x12, (byte) 0xFF}, {0x57, 0x34, 0x12, (byte) 0xFF},
                {0x58, 0x34, 0x12, (byte) 0xFF}, {0x59, 0x34, 0x12, (byte) 0xFF},
                {0x5A, 0x34, 0x12, (byte) 0xFF}, {0x5B, 0x34, 0x12, (byte) 0xFF},
                {0x5C, 0x34, 0x12, (byte) 0xFF}, {0x5D, 0x34, 0x12, (byte) 0xFF},
                {0x5E, 0x34, 0x12, (byte) 0xFF},
        };
        private static final byte[] LIGHT_PLACEHOLDER =
                {0x21, 0x43, 0x65, (byte) 0xFF};
        private static final byte[] LIGHT_OPACITY70_PLACEHOLDER =
                {0x32, 0x54, 0x76, (byte) 0xFF};
        /**
         * Gzip/Base64 resources.arsc generated with aapt2 from the stable runtime color IDs.
         * Synthetic MNC variants represent both changed palettes, the dark background only, and
         * the light background only. The last two keep the opposite foreground palette stock.
         */
        private static final String COMPRESSED_TABLE =
                "H4sIAAAAAAAAA+3dTU9cZRgG4Hdm2kq1CpUNi8aAdkHahJzSoaVJjQ0mbq2K3x/jABMgDAwZBhNW9ge4cGNidyZuXDbxD5h068KlS1eu1aUbfAZeykwqGQmNUXJd5OGd+/CemZMTZnnnlNOF9PPNUiqlFHMpfnrEwUe5PF56EPs+j5eLqZXW01RajrUVv5upEameNtJSaseR1Vin0k682k6dmIX4+2kyXtpfvygdHuveu2as53r2VXteD8WMxIx1z4+ZjCmnV9K1vE7n9Xpeq3mdifXs3h1vxrQffVZRSeni4YenT3s+66WY12K2Yr6JeRjza8z52HcnZiPmq5iHMb/HvFBO6fWYezHfxoyPr7famyuNWnt7o7O63qgt1dtrtc5KI14uNOuLa8XgLdfSlSuDttRam/XF1c7OrZnjbJ4d/OHTg7dcH7ylmqrVo7dsdeqd7a3aQr0ds7i23G5tbyztbUhXrx592nq902iv1pu15XZjpzY7U6TLlwdcR21zu904zpsWxTF234rdA+9+cXD3bxRpauqfXO/BCbPHPeHG3/xzNVeXVzr5jMVWs9V+/Iof23LwfjeLVC6PpPnb3e/mmXRx+GTffQAAAAAAAAAAAAAAAAAA4MmqpHTnpFMqz6fvXt7v+3c7xV/fjoMnvK5dAAAAAAAAAAAAAAAAAADgP6PbAe4+I3w8pkj7feK7af/Z3psx92K+TPvP9H4Q80PMTzG/xPzWPbnUfc54ea9LPJTSpbvV53e7uZTzGzmXc34z50rOb+V8Juf5nM/m/HbO53J+J+encn4356Gc38v5fM7v5/x0zh/k/EzOH+Z8IeePcn42549zfi7nT3Ieznni1cZeHsl5ev6z3aN62i9Weh7erqcNAAAAAAAAAAAAAAAAAAD/W3ra/2ZPu6ynDQAAAAAAAAAAAAAAAAAAp4Ce9sl62t172NvTjvj90T3tip42AAAAAAAAAAAAAAAAAACcAk+6pz05OdnX056YmOjraUf+o7enHfnP3p722NhYX097eHi4r6ddqVT6etqjo6N9Pe2iKPp62t2Kc29Pe25urq+nfXB9Fw6v/35vTzuWH3t72rHcH/Q87b8A8KKu29g3AQA=";

        private static ResourcesLoader loader;

        private RuntimeResourceOverlay() {
        }

        @RequiresApi(api = Build.VERSION_CODES.R)
        static synchronized void install(
                Context context, int[] darkColors, int lightColor, int lightOpacity70Color)
                throws IOException {
            if (loader == null) {
                byte[] table = inflateTable();
                for (int i = 0; i < DARK_PLACEHOLDERS.length; i++) {
                    replacePlaceholderColor(table, DARK_PLACEHOLDERS[i], darkColors[i]);
                }
                replacePlaceholderColor(table, LIGHT_PLACEHOLDER, lightColor);
                replacePlaceholderColor(table, LIGHT_OPACITY70_PLACEHOLDER, lightOpacity70Color);
                File tableFile = new File(context.getCacheDir(), "morphe-youtube-theme.arsc");
                try (FileOutputStream output = new FileOutputStream(tableFile, false)) {
                    output.write(table);
                }

                ResourcesProvider provider;
                try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                        tableFile, ParcelFileDescriptor.MODE_READ_ONLY)) {
                    provider = ResourcesProvider.loadFromTable(descriptor, null);
                }
                loader = new ResourcesLoader();
                loader.addProvider(provider);
            }

            applySelection(context);
            Resources contextResources = context.getResources();
            contextResources.addLoaders(loader);
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null && applicationContext.getResources() != contextResources) {
                applicationContext.getResources().addLoaders(loader);
            }
            BaseThemeUtils.setThemeColor();
        }

        /** Selects a table configuration after YouTube reports its own forced appearance. */
        static synchronized void select(Context context) {
            applySelection(context);
            if (loader != null) {
                BaseThemeUtils.setThemeColor();
            }
        }

        private static void applySelection(Context context) {
            int themedMnc = Settings.THEME_COLOR_CHANGE_FOREGROUND.get()
                    ? RUNTIME_THEME_CHANGE_FOREGROUND_MNC
                    : isDarkTheme()
                            ? RUNTIME_THEME_DARK_BACKGROUND_MNC
                            : RUNTIME_THEME_LIGHT_BACKGROUND_MNC;
            Resources contextResources = context.getResources();
            apply(contextResources, themedMnc);
            Context applicationContext = context.getApplicationContext();
            if (applicationContext != null && applicationContext.getResources() != contextResources) {
                apply(applicationContext.getResources(), themedMnc);
            }
        }

        private static void apply(Resources resources, int themedMnc) {
            Configuration current = resources.getConfiguration();
            if (current.mcc == RUNTIME_THEME_MCC && current.mnc == themedMnc) return;

            Configuration themed = new Configuration(current);
            themed.mcc = RUNTIME_THEME_MCC;
            themed.mnc = themedMnc;
            resources.updateConfiguration(themed, resources.getDisplayMetrics());
        }

        private static byte[] inflateTable() throws IOException {
            byte[] compressed = Base64.decode(COMPRESSED_TABLE, Base64.DEFAULT);
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed));
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        }

        private static void replacePlaceholderColor(byte[] table, byte[] placeholder, int color)
                throws IOException {
            boolean found = false;
            for (int i = 0; i <= table.length - placeholder.length; i++) {
                if (table[i] == placeholder[0]
                        && table[i + 1] == placeholder[1]
                        && table[i + 2] == placeholder[2]
                        && table[i + 3] == placeholder[3]) {
                    table[i] = (byte) color;
                    table[i + 1] = (byte) (color >>> 8);
                    table[i + 2] = (byte) (color >>> 16);
                    table[i + 3] = (byte) (color >>> 24);
                    found = true;
                    i += placeholder.length - 1;
                }
            }
            if (!found) {
                throw new IOException("Runtime theme placeholder not found");
            }
        }

    }

    public enum SplashScreenAnimationStyle {
        // 0 int style exists in target app as a fall through default, but its value is repurposed to be disabled.
        DISABLED(0),
        FPS_60_ONE_SECOND(1),
        FPS_60_TWO_SECOND(2),
        FPS_60_FIVE_SECOND(3),
        FPS_60_BLACK_AND_WHITE(4),
        FPS_30_ONE_SECOND(5),
        FPS_30_TWO_SECOND(6),
        FPS_30_FIVE_SECOND(7),
        FPS_30_BLACK_AND_WHITE(8);
        // There exists a 10th JSON style used as the switch statement default,
        // but visually it is identical to 60fps one second.

        @Nullable
        static SplashScreenAnimationStyle styleFromOrdinal(int style) {
            // Alternatively can return using values()[style]
            for (SplashScreenAnimationStyle value : values()) {
                if (value.style == style) {
                    return value;
                }
            }

            return null;
        }

        final int style;

        SplashScreenAnimationStyle(int style) {
            this.style = style;
        }
    }

    /**
     * Injection point for the splash screen visibility flag.
     */
    public static boolean showSplashScreen(boolean original) {
        return !shouldDisableHostSplashAnimation() && original;
    }

    /**
     * Injection point for the splash screen animation resource selection.
     */
    public static int showSplashScreen(int i, int i2) {
        if (!shouldDisableHostSplashAnimation() || i != i2) {
            return i;
        }
        return i - 1;
    }

    /**
     * Injection point for the splash screen animation style.
     */
    public static int getLoadingScreenType(int original) {
        SplashScreenAnimationStyle style = Settings.SPLASH_SCREEN_ANIMATION_STYLE.get();

        if (shouldDisableHostSplashAnimation()) {
            return original;
        }

        final int replacement = style.style;
        if (original != replacement) {
            Logger.printDebug(() -> "Overriding splash screen style from: "
                    + SplashScreenAnimationStyle.styleFromOrdinal(original) + " to: " + style);
        }

        return replacement;
    }

    /** Returns whether the selected splash animation style disables the animation completely. */
    private static boolean isSplashAnimationDisabled() {
        return Settings.SPLASH_SCREEN_ANIMATION_STYLE.get() == SplashScreenAnimationStyle.DISABLED;
    }

    /**
     * Suppresses YouTube's host splash when custom branding supplies its own splash overlay.
     * The style preference remains the independent control that disables both implementations.
     */
    private static boolean shouldDisableHostSplashAnimation() {
        return isSplashAnimationDisabled()
                || !"original".equals(Settings.CUSTOM_BRANDING_ICON.get());
    }
}
