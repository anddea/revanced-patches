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
 * Inspired by Morphe.
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.patches;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.settings.preference.IconListPreference;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;

/**
 * Enables the launcher alias selected in the custom branding settings.
 *
 * <p>The patch creates one alias for every app-name/icon combination. Android keeps the alias
 * labels and icons in the manifest, so changing either setting only requires switching which
 * alias is enabled. The application resources themselves remain untouched.</p>
 */
@SuppressWarnings({"deprecation", "unused"})
public final class CustomBrandingPatch {
    private static final String ICON_VALUES_RESOURCE = "morphe_custom_branding_icon_entry_values";
    private static final String NAME_ENTRIES_RESOURCE = "morphe_custom_branding_name_entries";
    private static final String NAME_VALUES_RESOURCE = "morphe_custom_branding_name_entry_values";
    private static final String DEFAULT_ICON_RESOURCE = "morphe_custom_branding_default_icon";
    private static final String DEFAULT_NAME_INDEX_RESOURCE = "morphe_custom_branding_default_name_index";
    private static final String MAIN_ACTIVITY_RESOURCE = "morphe_custom_branding_main_activity";
    private static final String ORIGINAL_LAUNCHER_RESOURCE = "morphe_custom_branding_original_launcher";
    private static final String NOTIFICATION_ICON_PREFIX = "morphe_notification_icon_";
    private static final String HEADER_RESOURCE_PREFIX = "morphe_custom_branding_header_";
    private static final String SPLASH_RESOURCE_PREFIX = "morphe_custom_branding_splash_";
    private static final String SPLASH_OVERLAY_TAG = "morphe_custom_branding_splash_overlay";
    private static final String SPLASH_SCREEN_STYLE_OPTION =
            "android.activity.splashScreenStyle";
    private static final int SPLASH_SCREEN_STYLE_SOLID_COLOR = 0;
    private static final String SPLASH_ANIMATION_STYLE_KEY = "morphe_splash_screen_animation_style";
    private static final String SPLASH_ANIMATION_STYLE_DEFAULT = "FPS_60_ONE_SECOND";
    private static final String SPLASH_ANIMATION_STYLE_DISABLED = "DISABLED";
    private static final String DISABLE_CAIRO_SPLASH_ANIMATION_KEY =
            "revanced_disable_cairo_splash_animation";
    private static final String SPLASH_ANIMATION_STYLE_60_BLACK_AND_WHITE = "FPS_60_BLACK_AND_WHITE";
    private static final String SPLASH_ANIMATION_STYLE_30_BLACK_AND_WHITE = "FPS_30_BLACK_AND_WHITE";
    private static final String CUSTOM_ICON_ALIAS = "custom";
    private static final String LAUNCHER_RESOURCE_PREFIX = "morphe_launcher_";
    private static final float RVX_SETTINGS_ICON_SCALE = 0.6f;
    private static final float MUSIC_CUSTOM_RVX_SETTINGS_ICON_HORIZONTAL_OFFSET_DP = -3.0f;
    private static final int NAME_ALIAS_COUNT = 5;
    private static final ColorFilter MONOCHROME_SPLASH_FILTER = new ColorMatrixColorFilter(
            new ColorMatrix(new float[]{
                    0.299f, 0.587f, 0.114f, 0, 0,
                    0.299f, 0.587f, 0.114f, 0, 0,
                    0.299f, 0.587f, 0.114f, 0, 0,
                    0, 0, 0, 1, 0,
            }));

    private CustomBrandingPatch() {
    }

    /**
     * Splashless entry point used only for non-stock launcher aliases.
     *
     * <p>It immediately forwards the launch intent to the real host activity. Keeping the host
     * activity out of this class hierarchy avoids breaking applications that initialize their
     * dependency graph using the exact activity type.</p>
     */
    public static class SplashlessLauncherActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String mainActivityName = CustomBrandingPatch.getString(
                    this, MAIN_ACTIVITY_RESOURCE, "");
            if (mainActivityName.isEmpty()) {
                finish();
                return;
            }

            try {
                Intent targetIntent = new Intent(getIntent());
                targetIntent.setComponent(new ComponentName(this, mainActivityName));
                int targetFlags = targetIntent.getFlags()
                        & ~Intent.FLAG_ACTIVITY_NEW_TASK
                        & ~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
                targetIntent.setFlags(targetFlags | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // ActivityOptions exposes this publicly only on newer Android releases, but
                    // the Bundle contract exists from API 31. Suppress the forwarded activity's
                    // icon as well as the launcher's icon to prevent an intermittent second flash.
                    Bundle options = new Bundle();
                    options.putInt(SPLASH_SCREEN_STYLE_OPTION, SPLASH_SCREEN_STYLE_SOLID_COLOR);
                    startActivity(targetIntent, options);
                } else {
                    startActivity(targetIntent);
                }
            } finally {
                finish();
                overridePendingTransition(0, 0);
            }
        }
    }

    /** Default used before a branding resource configuration is available. */
    @NonNull
    public static String getDefaultIconStyle() {
        Context context = Utils.getContext();
        return context == null
                ? "original"
                : getString(context, DEFAULT_ICON_RESOURCE, "original");
    }

    /** Default used before a branding resource configuration is available. */
    public static int getDefaultAppNameIndex() {
        Context context = Utils.getContext();
        return context == null
                ? 1
                : parseInt(getString(context, DEFAULT_NAME_INDEX_RESOURCE, "1"));
    }

    /** Entry point injected into the main activity's {@code onCreate}. */
    public static void setBranding(Activity activity) {
        try {
            Context context = activity != null ? activity : Utils.getContext();
            if (context == null) return;

            String[] iconValues = getStringArray(context, ICON_VALUES_RESOURCE);
            String[] nameValues = getStringArray(context, NAME_VALUES_RESOURCE);
            if (iconValues.length == 0 || nameValues.length == 0) return;

            String defaultIcon = getString(context, DEFAULT_ICON_RESOURCE, "original");
            int defaultNameIndex = parseInt(
                    getString(context, DEFAULT_NAME_INDEX_RESOURCE, "1"));

            // A value equal to a Setting's default is intentionally removed from
            // SharedPreferences. Do not materialize the generated resource default here: doing
            // so would turn a deliberate selection of the stock icon/name into the old patch
            // default on the next launch.
            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            int selectedNameIndex = SharedYouTubeSettings.CUSTOM_BRANDING_NAME.get();

            Set<String> validIcons = new HashSet<>(Arrays.asList(iconValues));
            if (!validIcons.contains(selectedIcon)) {
                selectedIcon = defaultIcon;
                if (!validIcons.contains(selectedIcon)) selectedIcon = iconValues[0];
                SharedYouTubeSettings.CUSTOM_BRANDING_ICON.save(selectedIcon);
            }
            if (selectedNameIndex < 1 || selectedNameIndex > nameValues.length) {
                selectedNameIndex = defaultNameIndex;
                if (selectedNameIndex < 1 || selectedNameIndex > nameValues.length) {
                    selectedNameIndex = 1;
                }
                SharedYouTubeSettings.CUSTOM_BRANDING_NAME.save(selectedNameIndex);
            }

            applyApplicationLabel(context, selectedNameIndex);

            String packageName = context.getPackageName();
            PackageManager packageManager = context.getPackageManager();
            List<ComponentName> aliases = new ArrayList<>();
            ComponentName selectedComponent = null;

            String originalLauncherName = getString(context, ORIGINAL_LAUNCHER_RESOURCE, "");
            IconListPreference.setOriginalLauncherIconName(originalLauncherName);

            // Custom aliases remain in the manifest even when their patch options are omitted.
            // Always manage those hidden aliases so an alias enabled by an earlier installation
            // cannot remain active alongside the newly selected launcher component.
            List<String> aliasIconValues = new ArrayList<>(Arrays.asList(iconValues));
            if (!aliasIconValues.contains(CUSTOM_ICON_ALIAS)) {
                aliasIconValues.add(CUSTOM_ICON_ALIAS);
            }
            for (String iconValue : aliasIconValues) {
                for (int nameIndex = 1; nameIndex <= NAME_ALIAS_COUNT; nameIndex++) {
                    ComponentName component = new ComponentName(
                            packageName,
                            packageName + ".morphe_" + iconValue + "_" + nameIndex);
                    aliases.add(component);
                    if (iconValue.equals(selectedIcon) && nameIndex == selectedNameIndex) {
                        selectedComponent = component;
                    }
                }
            }

            if (selectedComponent == null) {
                selectedComponent = aliases.get(0);
            }

            // Retire the old launcher before publishing the new one, then let
            // PackageManager finish the final enable operation normally.
            for (ComponentName alias : aliases) {
                if (!alias.equals(selectedComponent)) {
                    packageManager.setComponentEnabledSetting(
                            alias,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            PackageManager.DONT_KILL_APP);
                }
            }
            packageManager.setComponentEnabledSetting(
                    selectedComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0);
        } catch (Exception ignored) {
            // Root-mounted installations do not apply manifest aliases. Keep the app launchable.
        }
    }

    /**
     * Resolves the selected name for code paths that ask Android for the application name.
     * Launcher aliases still use their manifest labels because Android does not expose a runtime
     * API to change an installed component's label.
     */
    @NonNull
    public static String getApplicationName(String original) {
        String fallback = original == null ? "" : original;
        try {
            Context context = Utils.getContext();
            if (context == null) return fallback;

            String[] nameEntries = getStringArray(context, NAME_ENTRIES_RESOURCE);
            if (nameEntries.length == 0) return fallback;

            int selectedNameIndex = SharedYouTubeSettings.CUSTOM_BRANDING_NAME.get();
            if (selectedNameIndex < 1 || selectedNameIndex > nameEntries.length) {
                selectedNameIndex = getDefaultAppNameIndex();
            }

            String label = selectedNameIndex >= 1 && selectedNameIndex <= nameEntries.length
                    ? nameEntries[selectedNameIndex - 1]
                    : fallback;
            return label.isEmpty() ? fallback : label;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void applyApplicationLabel(Context context, int selectedNameIndex) {
        String fallback = getString(
                context, "morphe_custom_branding_original_app_name", "YouTube");
        String[] nameEntries = getStringArray(context, NAME_ENTRIES_RESOURCE);
        String label = selectedNameIndex >= 1 && selectedNameIndex <= nameEntries.length
                ? nameEntries[selectedNameIndex - 1]
                : fallback;

        if (!label.isEmpty()) {
            context.getApplicationInfo().nonLocalizedLabel = label;
        }
    }

    /** Replaces the stock YouTube wordmark with the selected project's themed wordmark. */
    public static Drawable getHeaderDrawable(
            Context context, int originalAttribute, Drawable original) {
        try {
            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            if ("original".equals(selectedIcon) || "youtube".equals(selectedIcon)) {
                return original;
            }

            int premiumAttribute = ResourceUtils.getIdentifier(
                    "ytPremiumWordmarkHeader", ResourceType.ATTR, context);
            int generalAttribute = ResourceUtils.getIdentifier(
                    "ytWordmarkHeader", ResourceType.ATTR, context);
            if (originalAttribute != premiumAttribute && originalAttribute != generalAttribute) {
                return original;
            }
            String headerResource = originalAttribute == premiumAttribute
                    ? "yt_premium_wordmark_header"
                    : "yt_wordmark_header";
            String theme = BaseThemeUtils.isDarkModeEnabled() ? "dark" : "light";
            int identifier = ResourceUtils.getIdentifier(
                    HEADER_RESOURCE_PREFIX + selectedIcon + "_" + headerResource + "_" + theme,
                    ResourceType.DRAWABLE,
                    context);
            return identifier == 0 ? original : context.getResources().getDrawable(identifier);
        } catch (Exception ignored) {
            return original;
        }
    }

    /**
     * Header wrapper for injected methods that have no free local register to preserve their
     * {@link Context}. The application context is sufficient for resolving themed drawables.
     */
    public static Drawable getHeaderDrawable(int originalAttribute, Drawable original) {
        Context context = Utils.getContext();
        return context == null ? original : getHeaderDrawable(context, originalAttribute, original);
    }

    /** Returns the selected YouTube Music header while preserving the stock resource as fallback. */
    public static int getMusicHeaderDrawableId(int original) {
        try {
            Context context = Utils.getContext();
            if (context == null) return original;

            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            if ("original".equals(selectedIcon) || "youtube_music".equals(selectedIcon)) {
                int stock = ResourceUtils.getIdentifier(
                        "action_bar_logo", ResourceType.DRAWABLE, context);
                return stock == 0 ? original : stock;
            }

            int identifier = ResourceUtils.getIdentifier(
                    HEADER_RESOURCE_PREFIX + selectedIcon + "_action_bar_logo",
                    ResourceType.DRAWABLE,
                    context);
            return identifier == 0 ? original : identifier;
        } catch (Exception ignored) {
            return original;
        }
    }

    private static int getRvxSettingsIconDrawableId(Context context) {
        String resourceName = "morphe_rvx_settings_icon_fallback";
        if (SharedYouTubeSettings.CUSTOM_BRANDING_APPLY_TO_RVX_SETTINGS.get()) {
            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            if (!selectedIcon.isEmpty()) {
                resourceName = "morphe_rvx_settings_icon_" + selectedIcon;
            }
        }

        int identifier = ResourceUtils.getIdentifier(
                resourceName, ResourceType.DRAWABLE, context);
        if (identifier == 0 && !"morphe_rvx_settings_icon_fallback".equals(resourceName)) {
            identifier = ResourceUtils.getIdentifier(
                    "morphe_rvx_settings_icon_fallback", ResourceType.DRAWABLE, context);
        }
        return identifier;
    }

    /**
     * Drawable inflated by the Music settings XML so its ordinary Preference can select an icon
     * without calling version-specific AndroidX Preference methods.
     */
    public static final class RvxSettingsIconDrawable extends Drawable {
        private Drawable delegate;

        /**
         * Keeps launcher fallbacks aligned with Music's legacy preference icon slot. Dedicated
         * settings artwork retains its authored bounds and does not receive implicit scaling.
         */
        private void updateDelegateBounds(@NonNull android.graphics.Rect bounds) {
            if (delegate == null) return;

            if (delegate instanceof RvxSettingsIconFallbackDrawable) {
                Context context = Utils.getContext();
                if (context == null) {
                    delegate.setBounds(bounds);
                    return;
                }

                int horizontalOffset = Math.round(
                        MUSIC_CUSTOM_RVX_SETTINGS_ICON_HORIZONTAL_OFFSET_DP
                                * context.getResources().getDisplayMetrics().density);
                delegate.setBounds(
                        bounds.left + horizontalOffset,
                        bounds.top,
                        bounds.right + horizontalOffset,
                        bounds.bottom);
                return;
            }

            delegate.setBounds(bounds);
        }

        private Drawable getDelegate() {
            if (delegate != null) return delegate;

            Context context = Utils.getContext();
            if (context == null) return null;
            int identifier = getRvxSettingsIconDrawableId(context);
            if (identifier == 0) return null;

            delegate = context.getResources().getDrawable(identifier);
            updateDelegateBounds(getBounds());
            delegate.setState(getState());
            delegate.setLevel(getLevel());
            return delegate;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Drawable drawable = getDelegate();
            if (drawable == null) return;

            updateDelegateBounds(getBounds());
            drawable.draw(canvas);
        }

        @Override
        protected void onBoundsChange(@NonNull android.graphics.Rect bounds) {
            getDelegate();
            updateDelegateBounds(bounds);
        }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.setState(state);
        }

        @Override
        protected boolean onLevelChange(int level) {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.setLevel(level);
        }

        @Override
        public boolean isStateful() {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.isStateful();
        }

        @Override
        public int getIntrinsicWidth() {
            Drawable drawable = getDelegate();
            return drawable == null ? -1 : drawable.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            Drawable drawable = getDelegate();
            return drawable == null ? -1 : drawable.getIntrinsicHeight();
        }

        @Override
        public void setAlpha(int alpha) {
            Drawable drawable = getDelegate();
            if (drawable != null) drawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            Drawable drawable = getDelegate();
            if (drawable != null) drawable.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            Drawable drawable = getDelegate();
            return drawable == null ? PixelFormat.TRANSPARENT : drawable.getOpacity();
        }
    }

    private static int getLauncherIconDrawableIdentifier(Context context) {
        String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
        if (selectedIcon.isEmpty()) selectedIcon = "original";

        String resourceName = "original".equals(selectedIcon)
                ? getString(context, ORIGINAL_LAUNCHER_RESOURCE, "")
                : LAUNCHER_RESOURCE_PREFIX + selectedIcon;
        int identifier = getLauncherResourceIdentifier(context, resourceName);
        if (identifier != 0 || "original".equals(selectedIcon)) return identifier;

        // An icon without bundled launcher layers can still use the stock launcher as a safe
        // fallback instead of leaving the RVX settings row empty.
        return getLauncherResourceIdentifier(
                context, getString(context, ORIGINAL_LAUNCHER_RESOURCE, ""));
    }

    private static int getLauncherResourceIdentifier(Context context, String resourceName) {
        if (resourceName.isEmpty()) return 0;

        int identifier = ResourceUtils.getIdentifier(
                resourceName, ResourceType.MIPMAP, context);
        if (identifier == 0) {
            identifier = ResourceUtils.getIdentifier(
                    resourceName, ResourceType.DRAWABLE, context);
        }
        return identifier;
    }

    /**
     * Draws a launcher icon as a smaller RVX settings icon when no dedicated settings artwork is
     * available. The launcher aliases and manifest continue to use the unscaled resource.
     */
    public static final class RvxSettingsIconFallbackDrawable extends Drawable {
        private Drawable delegate;

        private int getSettingsIconSize() {
            Context context = Utils.getContext();
            if (context == null) return -1;

            return Math.round(48.0f * context.getResources().getDisplayMetrics().density);
        }

        private Drawable getDelegate() {
            if (delegate != null) return delegate;

            Context context = Utils.getContext();
            if (context == null) return null;

            int identifier = getLauncherIconDrawableIdentifier(context);
            if (identifier == 0) return null;

            try {
                delegate = context.getResources().getDrawable(identifier);
                delegate.setBounds(getBounds());
                delegate.setState(getState());
                delegate.setLevel(getLevel());
                return delegate;
            } catch (Exception ignored) {
                return null;
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Drawable drawable = getDelegate();
            if (drawable == null) return;

            android.graphics.Rect bounds = getBounds();
            if (bounds.isEmpty()) return;

            int scaledWidth = Math.round(bounds.width() * RVX_SETTINGS_ICON_SCALE);
            int scaledHeight = Math.round(bounds.height() * RVX_SETTINGS_ICON_SCALE);
            int left = bounds.left + (bounds.width() - scaledWidth) / 2;
            int top = bounds.top + (bounds.height() - scaledHeight) / 2;

            drawable.setBounds(left, top, left + scaledWidth, top + scaledHeight);
            drawable.draw(canvas);
        }

        @Override
        protected void onBoundsChange(@NonNull android.graphics.Rect bounds) {
            Drawable drawable = getDelegate();
            if (drawable != null) drawable.setBounds(bounds);
        }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.setState(state);
        }

        @Override
        protected boolean onLevelChange(int level) {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.setLevel(level);
        }

        @Override
        public boolean isStateful() {
            Drawable drawable = getDelegate();
            return drawable != null && drawable.isStateful();
        }

        @Override
        public int getIntrinsicWidth() {
            return getSettingsIconSize();
        }

        @Override
        public int getIntrinsicHeight() {
            return getSettingsIconSize();
        }

        @Override
        public void setAlpha(int alpha) {
            Drawable drawable = getDelegate();
            if (drawable != null) drawable.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            Drawable drawable = getDelegate();
            if (drawable != null) drawable.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            Drawable drawable = getDelegate();
            return drawable == null ? PixelFormat.TRANSPARENT : drawable.getOpacity();
        }
    }

    /**
     * Shows the selected splash in the activity's first rendered frame.
     *
     * <p>The host splash continues running behind this independent overlay so its completion
     * callbacks and offline startup behavior remain unchanged.</p>
     */
    public static void applySplashAnimation(Activity activity) {
        applySplashAnimation(activity, false);
    }

    /**
     * Applies the YouTube splash animation style to the custom branding overlay.
     *
     * <p>The style preference belongs to YouTube's splash patch and is intentionally not used by
     * YouTube Music. Music keeps its custom branding animation regardless of that preference.</p>
     */
    public static void applyYouTubeSplashAnimation(Activity activity) {
        applySplashAnimation(activity, true);
    }

    private static void applySplashAnimation(Activity activity, boolean useYouTubeSplashStyle) {
        if (activity == null
                || activity.isFinishing()
                || activity.isDestroyed()
                || (useYouTubeSplashStyle && isSplashAnimationDisabled())
                || (!useYouTubeSplashStyle && isMusicSplashAnimationDisabled())) {
            return;
        }

        try {
            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            if ("original".equals(selectedIcon)) return;

            if (getSplashResourceIdentifier(activity, selectedIcon) == 0) return;

            Runnable showOverlay = () -> {
                int identifier = getSplashResourceIdentifier(activity, selectedIcon);
                if (identifier == 0) return;

                Drawable drawable = activity.getResources().getDrawable(identifier);
                showSplashOverlay(activity, drawable, selectedIcon, useYouTubeSplashStyle);
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    // The transferred system splash is above the activity decor. Add our overlay
                    // during its exit callback, then remove it in the same handoff so no stock icon
                    // or empty frame can appear between the two splash implementations.
                    SplashScreenBridge.installExitHandoff(activity, showOverlay);
                } catch (Exception ignored) {
                    // The posted overlay below remains the fallback if no splash was transferred.
                }
            }

            // Warm launches may not receive a system splash exit callback. Posting also lets
            // YouTube publish its forced appearance before the initial background is resolved.
            activity.getWindow().getDecorView().post(showOverlay);
        } catch (Exception ignored) {
            // Keep the stock host splash if a generated resource is unavailable.
        }
    }

    /** Keeps Android 12 splash-screen classes out of the verifier path on older devices. */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private static final class SplashScreenBridge {
        private SplashScreenBridge() {
        }

        private static void installExitHandoff(Activity activity, Runnable showOverlay) {
            activity.getSplashScreen().setOnExitAnimationListener(splashScreenView -> {
                try {
                    showOverlay.run();
                } finally {
                    splashScreenView.remove();
                }
            });
        }
    }

    /**
     * Resolves a generated theme-specific splash when one exists, falling back to the base splash.
     * The lookup uses YouTube's resolved appearance rather than Android's night qualifier because
     * YouTube can force light or dark mode independently of the device configuration.
     */
    private static int getSplashResourceIdentifier(Context context, String selectedIcon) {
        String theme = BaseThemeUtils.isDarkModeEnabled() ? "dark" : "light";
        int themedIdentifier = ResourceUtils.getIdentifier(
                SPLASH_RESOURCE_PREFIX + selectedIcon + "_" + theme,
                ResourceType.DRAWABLE,
                context);
        if (themedIdentifier != 0) return themedIdentifier;

        return ResourceUtils.getIdentifier(
                SPLASH_RESOURCE_PREFIX + selectedIcon,
                ResourceType.DRAWABLE,
                context);
    }

    private static void showSplashOverlay(
            Activity activity,
            Drawable drawable,
            String selectedIcon,
            boolean useYouTubeSplashStyle) {
        try {
            ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
            if (decor.findViewWithTag(SPLASH_OVERLAY_TAG) != null) return;

            FrameLayout overlay = new FrameLayout(activity);
            overlay.setTag(SPLASH_OVERLAY_TAG);
            overlay.setClickable(true);
            int[] backgroundColor = {BaseThemeUtils.getAppBackgroundColor()};
            overlay.setBackgroundColor(backgroundColor[0]);

            ImageView image = new ImageView(activity);
            image.setImageDrawable(drawable);
            float splashScale = SharedYouTubeSettings.CUSTOM_BRANDING_SPLASH_ANIMATION_SIZE.get()
                    / 100.0f;
            image.setScaleX(splashScale);
            image.setScaleY(splashScale);
            if (useYouTubeSplashStyle && isSplashAnimationMonochrome()) {
                image.setColorFilter(MONOCHROME_SPLASH_FILTER);
            }
            image.setScaleType(ImageView.ScaleType.CENTER);
            overlay.addView(image, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER));
            decor.addView(overlay, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            // YouTube can publish its forced appearance after the first Activity callback. Keep
            // the splash aligned with that state instead of falling back to the device dark mode.
            Runnable syncBackground = new Runnable() {
                @Override
                public void run() {
                    if (overlay.getParent() == null) return;

                    int resolvedColor = BaseThemeUtils.getAppBackgroundColor();
                    if (backgroundColor[0] != resolvedColor) {
                        backgroundColor[0] = resolvedColor;
                        overlay.setBackgroundColor(resolvedColor);
                    }
                    overlay.postOnAnimation(this);
                }
            };
            overlay.postOnAnimation(syncBackground);

            Runnable removeOverlay = () -> {
                overlay.removeCallbacks(syncBackground);
                if (overlay.getParent() instanceof ViewGroup parent) {
                    parent.removeView(overlay);
                }
            };

            if (drawable instanceof AnimatedVectorDrawable animatedVector) {
                // Custom AVD durations are not known at compile time. Keep the overlay visible
                // until Android reports completion, with a timeout for malformed animations.
                animatedVector.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable ignored) {
                        overlay.post(removeOverlay);
                    }
                });
                animatedVector.start();
                overlay.postDelayed(removeOverlay, 5000L);
            } else {
                long duration = selectedIcon.startsWith("revancify") ? 1500L : 1000L;
                if (drawable instanceof Animatable) {
                    ((Animatable) drawable).start();
                } else {
                    AnimationSet animation = new AnimationSet(true);
                    animation.setDuration(duration);
                    animation.addAnimation(new AlphaAnimation(0.0f, 1.0f));
                    animation.addAnimation(new ScaleAnimation(
                            0.8f,
                            1.0f,
                            0.8f,
                            1.0f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f));
                    image.startAnimation(animation);
                }
                // Resources without AVD callbacks release offline startup after a fixed duration.
                overlay.postDelayed(removeOverlay, duration);
            }
        } catch (Exception ignored) {
            // Keep the stock splash if the activity window cannot host the overlay.
        }
    }

    /** Returns the selected in-app icon for notification small-icon hooks. */
    public static int getSmallIcon(int original) {
        try {
            Context context = Utils.getContext();
            if (context == null) return original;

            String selectedIcon = SharedYouTubeSettings.CUSTOM_BRANDING_ICON.get();
            if ("original".equals(selectedIcon)) return original;

            int identifier = ResourceUtils.getIdentifier(
                    NOTIFICATION_ICON_PREFIX + selectedIcon,
                    ResourceType.DRAWABLE,
                    context);
            return identifier == 0 ? original : identifier;
        } catch (Exception ignored) {
            return original;
        }
    }

    /** Removes the stock notification tint when a custom monochrome icon is selected. */
    public static int getColor(int original) {
        return getSmallIcon(original) == original ? original : Color.TRANSPARENT;
    }

    private static String[] getStringArray(Context context, String name) {
        try {
            int identifier = ResourceUtils.getIdentifier(name, ResourceType.ARRAY, context);
            return identifier == 0 ? new String[0] : context.getResources().getStringArray(identifier);
        } catch (Exception ignored) {
            return new String[0];
        }
    }

    @NonNull
    private static String getString(Context context, String name, @NonNull String fallback) {
        try {
            int identifier = ResourceUtils.getIdentifier(name, ResourceType.STRING, context);
            return identifier == 0 ? fallback : context.getResources().getString(identifier);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /**
     * Reads the existing YouTube splash-style preference without loading YouTube-only settings.
     * The branding runtime is shared with YouTube Music, where that settings class is absent.
     */
    private static String getSplashAnimationStyle() {
        try {
            return Setting.preferences.getString(
                    SPLASH_ANIMATION_STYLE_KEY, SPLASH_ANIMATION_STYLE_DEFAULT);
        } catch (Exception ignored) {
            return SPLASH_ANIMATION_STYLE_DEFAULT;
        }
    }

    private static boolean isSplashAnimationDisabled() {
        return SPLASH_ANIMATION_STYLE_DISABLED.equalsIgnoreCase(getSplashAnimationStyle());
    }

    private static boolean isSplashAnimationMonochrome() {
        String style = getSplashAnimationStyle();
        return SPLASH_ANIMATION_STYLE_60_BLACK_AND_WHITE.equalsIgnoreCase(style)
                || SPLASH_ANIMATION_STYLE_30_BLACK_AND_WHITE.equalsIgnoreCase(style);
    }

    /** Returns whether Music's existing Cairo animation toggle also disables the custom overlay. */
    private static boolean isMusicSplashAnimationDisabled() {
        try {
            return Setting.preferences.getBoolean(DISABLE_CAIRO_SPLASH_ANIMATION_KEY, false);
        } catch (Exception ignored) {
            return false;
        }
    }
}
