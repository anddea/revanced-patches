package app.morphe.extension.shared;

import app.morphe.extension.shared.utils.PackageUtils;

public class Utils extends app.morphe.extension.shared.utils.Utils {
    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static String getAppVersionName() {
        return PackageUtils.getAppVersionName();
    }
}
