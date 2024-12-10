package app.revanced.extension.shared.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PackageUtils extends Utils {
    private static String applicationLabel = "";
    private static int smallestScreenWidthDp = 0;
    private static String versionName = "";

    public static String getApplicationLabel() {
        return applicationLabel;
    }

    public static String getVersionName() {
        return versionName;
    }

    public static boolean isPackageEnabled(@NonNull String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        return false;
    }

    public static boolean isTablet() {
        return smallestScreenWidthDp >= 600;
    }

    public static void setApplicationLabel() {
        final PackageInfo packageInfo = getPackageInfo();
        if (packageInfo != null) {
            final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            if (applicationInfo != null) {
                applicationLabel = (String) applicationInfo.loadLabel(getPackageManager());
            }
        }
    }

    public static void setSmallestScreenWidthDp() {
        smallestScreenWidthDp = context.getResources().getConfiguration().smallestScreenWidthDp;
    }

    public static void setVersionName() {
        final PackageInfo packageInfo = getPackageInfo();
        if (packageInfo != null) {
            versionName = packageInfo.versionName;
        }
    }

    public static int getSmallestScreenWidthDp() {
        return smallestScreenWidthDp;
    }

    // utils
    @Nullable
    private static PackageInfo getPackageInfo() {
        try {
            final PackageManager packageManager = getPackageManager();
            final String packageName = context.getPackageName();
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
        return context.getPackageManager();
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
