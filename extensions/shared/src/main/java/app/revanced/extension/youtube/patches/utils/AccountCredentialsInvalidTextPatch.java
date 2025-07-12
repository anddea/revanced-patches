package app.revanced.extension.youtube.patches.utils;

import static app.revanced.extension.shared.utils.StringRef.sf;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings("unused")
public class AccountCredentialsInvalidTextPatch extends Utils {

    /**
     * Injection point.
     */
    public static String getOfflineNetworkErrorString(String original) {
        try {
            if (isNetworkConnected()) {
                Logger.printDebug(() -> "Network appears to be online, but app is showing offline error");
                return '\n' + sf("gms_core_offline_account_login_error").toString();
            }

            Logger.printDebug(() -> "Network is offline");
        } catch (Exception ex) {
            Logger.printException(() -> "getOfflineNetworkErrorString failure", ex);
        }

        return original;
    }
}
