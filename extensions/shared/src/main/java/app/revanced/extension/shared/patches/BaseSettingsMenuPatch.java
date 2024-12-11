package app.revanced.extension.shared.patches;

import android.util.Log;

import androidx.preference.PreferenceScreen;

@SuppressWarnings("unused")
public class BaseSettingsMenuPatch {

    /**
     * Rest of the implementation added by patch.
     */
    public static void removePreference(PreferenceScreen mPreferenceScreen, String key) {
        Log.d("Extended: SettingsMenuPatch", "key: " + key);
    }
}