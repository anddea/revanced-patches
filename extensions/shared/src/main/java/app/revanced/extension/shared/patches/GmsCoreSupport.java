package app.revanced.extension.shared.patches;

import static app.revanced.extension.shared.settings.BaseSettings.GMS_SHOW_DIALOG;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class GmsCoreSupport {
    private static final String PACKAGE_NAME_YOUTUBE = "com.google.android.youtube";
    private static final String PACKAGE_NAME_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    private static final String GMS_CORE_PACKAGE_NAME
            = getGmsCoreVendorGroupId() + ".android.gms";
    private static final Uri GMS_CORE_PROVIDER
            = Uri.parse("content://" + getGmsCoreVendorGroupId() + ".android.gsf.gservices/prefix");
    private static final String DONT_KILL_MY_APP_LINK
            = "https://dontkillmyapp.com";

    private static final String META_SPOOF_PACKAGE_NAME =
            GMS_CORE_PACKAGE_NAME + ".SPOOFED_PACKAGE_NAME";

    private static void open(Activity mActivity, String queryOrLink) {
        Intent intent;
        try {
            // Check if queryOrLink is a valid URL.
            new URL(queryOrLink);

            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(queryOrLink));
        } catch (MalformedURLException e) {
            intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY, queryOrLink);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mActivity.startActivity(intent);

        // Gracefully exit, otherwise the broken app will continue to run.
        System.exit(0);
    }

    private static void showBatteryOptimizationDialog(Activity context,
                                                      String dialogMessageRef,
                                                      String positiveButtonStringRef,
                                                      BooleanSetting setting,
                                                      DialogInterface.OnClickListener onPositiveClickListener,
                                                      boolean showNegativeButton) {
        // Use a delay to allow the activity to finish initializing.
        // Otherwise, if device is in dark mode the dialog is shown with wrong color scheme.
        Utils.runOnMainThreadDelayed(() -> {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(str("gms_core_dialog_title"))
                    .setMessage(str(dialogMessageRef))
                    .setPositiveButton(str(positiveButtonStringRef), onPositiveClickListener)
                    // Allow using back button to skip the action, just in case the check can never be satisfied.
                    .setCancelable(true);

            if (showNegativeButton) {
                dialogBuilder.setNegativeButton(str("gms_core_dialog_dismiss_text"), (dialog, which) -> setting.save(false));
            }

            dialogBuilder.show();
        }, 100);
    }

    /**
     * Injection point.
     */
    public static void checkGmsCore(Activity mActivity) {
        try {
            // Verify the user has not included GmsCore for a root installation.
            // GmsCore Support changes the package name, but with a mounted installation
            // all manifest changes are ignored and the original package name is used.
            if (StringUtils.equalsAny(mActivity.getPackageName(), PACKAGE_NAME_YOUTUBE, PACKAGE_NAME_YOUTUBE_MUSIC)) {
                Logger.printInfo(() -> "App is mounted with root, but GmsCore patch was included");
                // Cannot use localize text here, since the app will load
                // resources from the unpatched app and all patch strings are missing.
                Utils.showToastLong("The 'GmsCore support' patch breaks mount installations");

                // Do not exit. If the app exits before launch completes (and without
                // opening another activity), then on some devices such as Pixel phone Android 10
                // no toast will be shown and the app will continually be relaunched
                // with the appearance of a hung app.
            }

            // Verify GmsCore is installed.
            try {
                PackageManager manager = mActivity.getPackageManager();
                manager.getPackageInfo(GMS_CORE_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException exception) {
                Logger.printInfo(() -> "GmsCore was not found");
                // Cannot show a dialog and must show a toast,
                // because on some installations the app crashes before a dialog can be displayed.
                Utils.showToastLong(str("gms_core_toast_not_installed_message"));
                open(mActivity, getGmsCoreDownload());
                return;
            }

            if (contentProviderClientUnAvailable(mActivity)) {
                Logger.printInfo(() -> "GmsCore is not running in the background");

                showBatteryOptimizationDialog(mActivity,
                        "gms_core_dialog_not_whitelisted_not_allowed_in_background_message",
                        "gms_core_dialog_open_website_text",
                        null,
                        (dialog, id) -> open(mActivity, DONT_KILL_MY_APP_LINK),
                        false);  // Do not show the negative button
                return;
            }

            // Check if GmsCore is whitelisted from battery optimizations.
            if (batteryOptimizationsEnabled(mActivity)) {
                Logger.printInfo(() -> "GmsCore is not whitelisted from battery optimizations");
                if (GMS_SHOW_DIALOG.get()) {
                    showBatteryOptimizationDialog(mActivity,
                            "gms_core_dialog_not_whitelisted_using_battery_optimizations_message",
                            "gms_core_dialog_continue_text",
                            GMS_SHOW_DIALOG,
                            (dialog, id) -> openGmsCoreDisableBatteryOptimizationsIntent(mActivity),
                            true);  // Show the negative button
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "checkGmsCore failure", ex);
        }
    }

    /**
     * @return If GmsCore is not running in the background.
     */
    @SuppressWarnings("deprecation")
    private static boolean contentProviderClientUnAvailable(Context context) {
        // Check if GmsCore is running in the background.
        // Do this check before the battery optimization check.
        if (isSDKAbove(24)) {
            try (ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(GMS_CORE_PROVIDER)) {
                return client == null;
            }
        } else {
            ContentProviderClient client = null;
            try {
                //noinspection resource
                client = context.getContentResolver()
                        .acquireContentProviderClient(GMS_CORE_PROVIDER);
                return client == null;
            } finally {
                if (client != null) client.release();
            }
        }
    }

    @SuppressLint("BatteryLife") // Permission is part of GmsCore
    private static void openGmsCoreDisableBatteryOptimizationsIntent(Activity mActivity) {
        if (!isSDKAbove(23)) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.fromParts("package", GMS_CORE_PACKAGE_NAME, null));
        mActivity.startActivityForResult(intent, 0);
    }

    /**
     * @return If GmsCore is not whitelisted from battery optimizations.
     */
    private static boolean batteryOptimizationsEnabled(Context context) {
        if (isSDKAbove(23) && context.getSystemService(Context.POWER_SERVICE) instanceof PowerManager powerManager) {
            return !powerManager.isIgnoringBatteryOptimizations(GMS_CORE_PACKAGE_NAME);
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static String spoofPackageName(Context context) {
        // Package name of ReVanced.
        final String packageName = context.getPackageName();

        try {
            final PackageManager packageManager = context.getPackageManager();

            // Package name of YouTube or YouTube Music.
            String originalPackageName;

            try {
                originalPackageName = packageManager
                        .getPackageInfo(packageName, PackageManager.GET_META_DATA)
                        .applicationInfo
                        .metaData
                        .getString(META_SPOOF_PACKAGE_NAME);
            } catch (PackageManager.NameNotFoundException exception) {
                Logger.printDebug(() -> "Failed to parsing metadata");
                return packageName;
            }

            if (StringUtils.isBlank(originalPackageName)) {
                Logger.printDebug(() -> "Failed to parsing spoofed package name");
                return packageName;
            }

            try {
                packageManager.getPackageInfo(originalPackageName, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException exception) {
                Logger.printDebug(() -> "Original app '" + originalPackageName + "' was not found");
                return packageName;
            }

            Logger.printDebug(() -> "Package name of '" + packageName + "' spoofed to '" + originalPackageName + "'");

            return originalPackageName;
        } catch (Exception ex) {
            Logger.printException(() -> "spoofPackageName failure", ex);
        }

        return packageName;
    }

    private static String getGmsCoreDownload() {
        final String vendorGroupId = getGmsCoreVendorGroupId();
        return switch (vendorGroupId) {
            case "app.revanced" -> "https://github.com/revanced/gmscore/releases/latest";
            case "com.mgoogle" -> "https://github.com/inotia00/VancedMicroG/releases/latest";
            default -> vendorGroupId + ".android.gms";
        };
    }

    // Modified by a patch. Do not touch.
    private static String getGmsCoreVendorGroupId() {
        return "app.revanced";
    }
}