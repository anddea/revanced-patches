package app.morphe.extension.shared.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PackageUtils extends Utils {

    public static String getAppLabel() {
        final PackageInfo packageInfo = getPackageInfo();
        if (packageInfo != null) {
            final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (applicationInfo != null && applicationInfo.loadLabel(getPackageManager()) instanceof String applicationLabel) {
                return applicationLabel;
            }
        }
        return "";
    }

    public static String getAppVersionName() {
        final PackageInfo packageInfo = getPackageInfo();
        if (packageInfo != null) {
            return packageInfo.versionName;
        } else {
            return "";
        }
    }

    @Nullable
    public static Integer getTargetSDKVersion(@NonNull String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo != null) {
            return applicationInfo.targetSdkVersion;
        }

        return null;
    }

    public static boolean hasSystemFeature(String feature) {
        return getPackageManager().hasSystemFeature(feature);
    }

    public static boolean isPackageEnabled(@NonNull String packageName) {
        ApplicationInfo applicationInfo = getApplicationInfo(packageName);
        if (applicationInfo != null) {
            return applicationInfo.enabled;
        }

        return false;
    }

    public static boolean isTablet() {
        return getSmallestScreenWidthDp() >= 600;
    }

    public static boolean isVersionOrGreater(String version) {
        return getAppVersionName().compareTo(version) > 0;
    }

    public static int getSmallestScreenWidthDp() {
        return getResources(false).getConfiguration().smallestScreenWidthDp;
    }

    // utils
    @Nullable
    private static ApplicationInfo getApplicationInfo(@NonNull String packageName) {
        try {
            return getContext().getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.printDebug(() -> "getApplicationInfo) App is not installed: " + packageName);
        }
        return null;
    }

    @Nullable
    private static PackageInfo getPackageInfo() {
        return getPackageInfo(getContext().getPackageName());
    }

    @Nullable
    public static PackageInfo getPackageInfo(String packageName) {
        final PackageManager packageManager = getPackageManager();
        try {
            return isSDKAbove(33)
                    ? packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    : packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.printDebug(() -> "getPackageInfo) App is not installed: " + packageName);
        }
        return null;
    }

    @NonNull
    private static PackageManager getPackageManager() {
        return getContext().getPackageManager();
    }

    public static boolean isVersionToLessThan(@NonNull String compareVersion, @NonNull String targetVersion) {
        try {
            final int compareVersionNumber = Integer.parseInt(compareVersion.replaceAll("\\.", ""));
            final int targetVersionNumber = Integer.parseInt(targetVersion.replaceAll("\\.", ""));
            return compareVersionNumber < targetVersionNumber;
        } catch (NumberFormatException ex) {
            Logger.printException(() -> "Failed to compare version: " + compareVersion + ", " + targetVersion, ex);
        }
        return false;
    }
}
