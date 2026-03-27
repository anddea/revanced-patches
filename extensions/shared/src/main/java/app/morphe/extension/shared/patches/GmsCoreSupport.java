package app.morphe.extension.shared.patches;

import static app.morphe.extension.shared.settings.BaseSettings.GMS_SHOW_DIALOG;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.isSDKAbove;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Pair;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"deprecation", "unused"})
public class GmsCoreSupport {
    private static final String PACKAGE_NAME_YOUTUBE = "com.google.android.youtube";
    private static final String PACKAGE_NAME_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    private static final String GMS_CORE_PACKAGE_NAME
            = getGmsCoreVendorGroupId() + ".android.gms";
    private static final Uri GMS_CORE_PROVIDER
            = Uri.parse("content://" + getGmsCoreVendorGroupId() + ".android.gsf.gservices/prefix");
    private static final String GMS_CORE_ORIGINAL_VENDOR_GROUP_ID
            = "com.google";
    private static final String DONT_KILL_MY_APP_LINK
            = "https://dontkillmyapp.com";

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
                                                      String positiveButtonTextRef,
                                                      DialogInterface.OnClickListener onPositiveClickListener,
                                                      boolean showNegativeButton) {
        // Use a delay to allow the activity to finish initializing.
        // Otherwise, if device is in dark mode the dialog is shown with wrong color scheme.
        Utils.runOnMainThreadDelayed(() -> {
            if (BaseThemeUtils.isSupportModernDialog) {
                // Create the custom dialog.
                Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                        context,
                        // Title.
                        str("gms_core_dialog_title"),
                        // Message.
                        str(dialogMessageRef),
                        // No EditText.
                        null,
                        // OK button text.
                        str(positiveButtonTextRef),
                        // OK button action
                        () -> onPositiveClickListener.onClick(null, 0),
                        // onCancelClick: We don't want a "Cancel" button.
                        null,
                        // neutralButtonText
                        showNegativeButton ? str("gms_core_dialog_dismiss_text") : null,
                        // onNeutralClick
                        showNegativeButton ? () -> GMS_SHOW_DIALOG.save(false) : null,

                        // Dismiss dialog when onNeutralClick.
                        true
                );

                Dialog dialog = dialogPair.first;

                // Do not set cancelable to false, to allow using back button to skip the action,
                // just in case the battery change can never be satisfied.
                dialog.setCancelable(true);

                // Show the dialog
                Utils.showDialog(context, dialog);
            } else {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle(str("gms_core_dialog_title"))
                        .setMessage(str(dialogMessageRef))
                        .setPositiveButton(str(positiveButtonTextRef), onPositiveClickListener);

                if (showNegativeButton) {
                    dialogBuilder.setNegativeButton(str("gms_core_dialog_dismiss_text"), (dialog, which) -> GMS_SHOW_DIALOG.save(false));
                }

                dialogBuilder.setCancelable(true);
                dialogBuilder.show();
            }
        }, 100);
    }

    /**
     * Injection point.
     */
    public static void checkGmsCore(Activity mActivity) {
        try {
            // The user is using LineageOS for microG and the original GmsCore.
            // GmsCore is a system app, so no check is required.
            if (GMS_CORE_ORIGINAL_VENDOR_GROUP_ID.equals(getGmsCoreVendorGroupId())) {
                return;
            }
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
                return;
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
                        (dialog, id) -> open(mActivity, DONT_KILL_MY_APP_LINK),
                        false);
                return;
            }

            // Check if GmsCore is whitelisted from battery optimizations.
            if (isAndroidAutomotive(mActivity)) {
                // Ignore Android Automotive devices (Google built-in),
                // as there is no way to disable battery optimizations.
                Logger.printDebug(() -> "Device is Android Automotive");
            } else if (batteryOptimizationsEnabled(mActivity)) {
                Logger.printInfo(() -> "GmsCore is not whitelisted from battery optimizations");
                if (GMS_SHOW_DIALOG.get()) {
                    showBatteryOptimizationDialog(mActivity,
                            "gms_core_dialog_not_whitelisted_using_battery_optimizations_message",
                            "gms_core_dialog_continue_text",
                            (dialog, id) -> openGmsCoreDisableBatteryOptimizationsIntent(mActivity),
                            true);
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

    private static boolean isAndroidAutomotive(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
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
