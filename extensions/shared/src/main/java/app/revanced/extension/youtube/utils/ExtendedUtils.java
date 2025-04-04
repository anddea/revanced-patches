package app.revanced.extension.youtube.utils;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.Map;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.FloatSetting;
import app.revanced.extension.shared.settings.IntegerSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.PackageUtils;
import app.revanced.extension.youtube.settings.Settings;

public class ExtendedUtils extends PackageUtils {

    private static boolean isVersionOrGreater(String version) {
        return getAppVersionName().compareTo(version) >= 0;
    }

    @SuppressWarnings("unused")
    public static final boolean IS_19_17_OR_GREATER = isVersionOrGreater("19.17.00");
    public static final boolean IS_19_20_OR_GREATER = isVersionOrGreater("19.20.00");
    public static final boolean IS_19_21_OR_GREATER = isVersionOrGreater("19.21.00");
    public static final boolean IS_19_26_OR_GREATER = isVersionOrGreater("19.26.00");
    public static final boolean IS_19_28_OR_GREATER = isVersionOrGreater("19.28.00");
    public static final boolean IS_19_29_OR_GREATER = isVersionOrGreater("19.29.00");
    public static final boolean IS_19_34_OR_GREATER = isVersionOrGreater("19.34.00");
    public static final boolean IS_20_09_OR_GREATER = isVersionOrGreater("20.09.00");

    public static int validateValue(IntegerSetting settings, int min, int max, String message) {
        int value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_extended_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    public static float validateValue(FloatSetting settings, float min, float max, String message) {
        float value = settings.get();

        if (value < min || value > max) {
            showToastShort(str(message));
            showToastShort(str("revanced_extended_reset_to_default_toast"));
            settings.resetToDefault();
            value = settings.defaultValue;
        }

        return value;
    }

    public static boolean isFullscreenHidden() {
        return Settings.DISABLE_ENGAGEMENT_PANEL.get() || Settings.HIDE_QUICK_ACTIONS.get();
    }

    public static boolean isSpoofingToLessThan(@NonNull String versionName) {
        if (!Settings.SPOOF_APP_VERSION.get())
            return false;

        return isVersionToLessThan(Settings.SPOOF_APP_VERSION_TARGET.get(), versionName);
    }

    public static void setCommentPreviewSettings() {
        final boolean enabled = Settings.HIDE_PREVIEW_COMMENT.get();
        final boolean newMethod = Settings.HIDE_PREVIEW_COMMENT_TYPE.get();

        Settings.HIDE_PREVIEW_COMMENT_OLD_METHOD.save(enabled && !newMethod);
        Settings.HIDE_PREVIEW_COMMENT_NEW_METHOD.save(enabled && newMethod);
    }

    private static final Setting<?>[] additionalSettings = {
            Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
            Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
            Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
            Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
            Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
            Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
            Settings.SPOOF_APP_VERSION,
            Settings.SPOOF_APP_VERSION_TARGET
    };

    public static boolean anyMatchSetting(Setting<?> setting) {
        for (Setting<?> s : additionalSettings) {
            if (setting == s) return true;
        }
        return false;
    }

    public static void setPlayerFlyoutMenuAdditionalSettings() {
        Settings.HIDE_PLAYER_FLYOUT_MENU_ADDITIONAL_SETTINGS.save(isAdditionalSettingsEnabled());
    }

    private static boolean isAdditionalSettingsEnabled() {
        // In the old player flyout panels, the video quality icon and additional quality icon are the same
        // Therefore, additional Settings should not be blocked in old player flyout panels
        if (isSpoofingToLessThan("18.22.00"))
            return false;

        boolean additionalSettingsEnabled = true;
        final BooleanSetting[] additionalSettings = {
                Settings.HIDE_PLAYER_FLYOUT_MENU_AMBIENT,
                Settings.HIDE_PLAYER_FLYOUT_MENU_HELP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_LOOP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PIP,
                Settings.HIDE_PLAYER_FLYOUT_MENU_PREMIUM_CONTROLS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STABLE_VOLUME,
                Settings.HIDE_PLAYER_FLYOUT_MENU_STATS_FOR_NERDS,
                Settings.HIDE_PLAYER_FLYOUT_MENU_WATCH_IN_VR,
                Settings.HIDE_PLAYER_FLYOUT_MENU_YT_MUSIC,
        };
        for (BooleanSetting s : additionalSettings) {
            additionalSettingsEnabled &= s.get();
        }
        return additionalSettingsEnabled;
    }

    public static void showBottomSheetDialog(Context mContext, ScrollView mScrollView,
                                             Map<LinearLayout, Runnable> actionsMap) {
        runOnMainThreadDelayed(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setView(mScrollView);

            AlertDialog dialog = builder.create();
            dialog.show();

            actionsMap.forEach((view, action) ->
                    view.setOnClickListener(v -> {
                        action.run();
                        dialog.dismiss();
                    })
            );
            actionsMap.clear();

            Window window = dialog.getWindow();
            if (window == null) {
                return;
            }

            // round corners
            GradientDrawable dialogBackground = new GradientDrawable();
            dialogBackground.setCornerRadius(32);
            window.setBackgroundDrawable(dialogBackground);

            // fit screen width
            int dialogWidth = (int) (mContext.getResources().getDisplayMetrics().widthPixels * 0.95);
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);

            // move dialog to bottom
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.gravity = Gravity.BOTTOM;

            // adjust the vertical offset
            layoutParams.y = dpToPx(5);

            window.setAttributes(layoutParams);
        }, 250);
    }

    public static LinearLayout createItemLayout(Context mContext, String title, int iconId) {
        // Item Layout
        LinearLayout itemLayout = new LinearLayout(mContext);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setClickable(true);
        itemLayout.setFocusable(true);

        // Create a StateListDrawable for the background
        StateListDrawable background = new StateListDrawable();
        ColorDrawable pressedDrawable = new ColorDrawable(ThemeUtils.getPressedElementColor());
        ColorDrawable defaultDrawable = new ColorDrawable(ThemeUtils.getBackgroundColor());
        background.addState(new int[]{android.R.attr.state_pressed}, pressedDrawable);
        background.addState(new int[]{}, defaultDrawable);
        itemLayout.setBackground(background);

        // Icon
        ColorFilter cf = new PorterDuffColorFilter(ThemeUtils.getForegroundColor(), PorterDuff.Mode.SRC_ATOP);
        ImageView iconView = new ImageView(mContext);
        iconView.setImageResource(iconId);
        iconView.setColorFilter(cf);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(24));
        iconParams.setMarginEnd(dpToPx(16));
        iconView.setLayoutParams(iconParams);
        itemLayout.addView(iconView);

        // Text container
        LinearLayout textContainer = new LinearLayout(mContext);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = new TextView(mContext);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(ThemeUtils.getForegroundColor());
        textContainer.addView(titleView);

        itemLayout.addView(textContainer);

        return itemLayout;
    }

}