package app.revanced.extension.shared.utils;

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

    public static int getSmallestScreenWidthDp() {
        return getResources(false).getConfiguration().smallestScreenWidthDp;
    }

    // utils
    @Nullable
    private static ApplicationInfo getApplicationInfo(@NonNull String packageName) {
        try {
            return getContext().getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.printException(() -> "Failed to get application Info!" + e);
        }
        return null;
    }

    @Nullable
    private static PackageInfo getPackageInfo() {
        try {
            final PackageManager packageManager = getPackageManager();
            final String packageName = getContext().getPackageName();
            return isSDKAbove(33)
                    ? packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                    : packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Logger.printException(() -> "Failed to get package Info!" + e);
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
