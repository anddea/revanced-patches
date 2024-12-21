package app.revanced.extension.music.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.PackageUtils;

public class ExtendedUtils extends PackageUtils {

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
}