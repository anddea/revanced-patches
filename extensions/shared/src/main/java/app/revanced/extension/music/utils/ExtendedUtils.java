package app.revanced.extension.music.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.PackageUtils;
import app.revanced.extension.shared.utils.ResourceUtils;

public class ExtendedUtils extends PackageUtils {
    private static final String SETTINGS_CLASS_DESCRIPTOR = "com.google.android.apps.youtube.music.settings.SettingsCompatActivity";
    private static final String SETTINGS_ATTRIBUTION_FRAGMENT_KEY = ":android:show_fragment";
    private static final String SETTINGS_ATTRIBUTION_FRAGMENT_VALUE = "com.google.android.apps.youtube.music.settings.fragment.SettingsHeadersFragment";
    private static final String SETTINGS_ATTRIBUTION_HEADER_KEY = ":android:no_headers";
    private static final int SETTINGS_ATTRIBUTION_HEADER_VALUE = 1;

    private static final String SHORTCUT_ACTION = "com.google.android.youtube.music.action.shortcut";
    private static final String SHORTCUT_CLASS_DESCRIPTOR = "com.google.android.apps.youtube.music.activities.InternalMusicActivity";
    private static final String SHORTCUT_TYPE = "com.google.android.youtube.music.action.shortcut_type";
    private static final String SHORTCUT_ID_SEARCH = "Eh4IBRDTnQEYmgMiEwiZn+H0r5WLAxVV5OcDHcHRBmPqpd25AQA=";
    private static final int SHORTCUT_TYPE_SEARCH = 1;

    @SuppressWarnings("unused")
    public static boolean isSpoofingToLessThan(@NonNull String versionName) {
        if (!Settings.SPOOF_APP_VERSION.get())
            return false;

        return isVersionToLessThan(Settings.SPOOF_APP_VERSION_TARGET.get(), versionName);
    }

    @SuppressWarnings("deprecation")
    public static AlertDialog.Builder getDialogBuilder(@NonNull Context context) {
        return new AlertDialog.Builder(context, isSDKAbove(22)
                ? android.R.style.Theme_DeviceDefault_Dialog_Alert
                : AlertDialog.THEME_DEVICE_DEFAULT_DARK
        );
    }

    public static FrameLayout.LayoutParams getLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        int left_margin = dpToPx(20);
        int top_margin = dpToPx(10);
        int right_margin = dpToPx(20);
        int bottom_margin = dpToPx(4);
        params.setMargins(left_margin, top_margin, right_margin, bottom_margin);

        return params;
    }

    public static void openSearch() {
        Activity mActivity = ResourceUtils.getActivity();
        if (mActivity == null) {
            return;
        }
        Intent intent = new Intent();
        setSearchIntent(mActivity, intent);
        mActivity.startActivity(intent);
    }

    public static void openSetting() {
        Activity mActivity = ResourceUtils.getActivity();
        if (mActivity == null) {
            return;
        }
        Intent intent = new Intent();
        intent.setPackage(mActivity.getPackageName());
        intent.setClassName(mActivity, SETTINGS_CLASS_DESCRIPTOR);
        intent.putExtra(SETTINGS_ATTRIBUTION_FRAGMENT_KEY, SETTINGS_ATTRIBUTION_FRAGMENT_VALUE);
        intent.putExtra(SETTINGS_ATTRIBUTION_HEADER_KEY, SETTINGS_ATTRIBUTION_HEADER_VALUE);
        mActivity.startActivity(intent);
    }

    public static void setSearchIntent(Activity mActivity, Intent intent) {
        intent.setAction(SHORTCUT_ACTION);
        intent.setClassName(mActivity, SHORTCUT_CLASS_DESCRIPTOR);
        intent.setPackage(mActivity.getPackageName());
        intent.putExtra(SHORTCUT_TYPE, SHORTCUT_TYPE_SEARCH);
        intent.putExtra(SHORTCUT_ACTION, SHORTCUT_ID_SEARCH);
    }
}