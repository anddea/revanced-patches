package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.Utils.isSDKAbove;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public class OpenDefaultAppSettingsPreference extends Preference {
    {
        setOnPreferenceClickListener(pref -> {
            try {
                Context context = Utils.getActivity();
                final Uri uri = Uri.parse("package:" + context.getPackageName());
                final Intent intent = isSDKAbove(31)
                        ? new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, uri)
                        : new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
                context.startActivity(intent);
            } catch (Exception exception) {
                Logger.printException(() -> "OpenDefaultAppSettings Failed");
            }
            return false;
        });
    }

    public OpenDefaultAppSettingsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public OpenDefaultAppSettingsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OpenDefaultAppSettingsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenDefaultAppSettingsPreference(Context context) {
        super(context);
    }
}
