package app.morphe.extension.shared.patches;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.preference.PreferenceScreen;

@SuppressWarnings("unused")
public class BaseSettingsMenuPatch {

    /**
     * Rest of the implementation added by patch.
     */
    @SuppressLint("LongLogTag")
    public static void removePreference(PreferenceScreen mPreferenceScreen, String key) {
        Log.d("Extended: SettingsMenuPatch", "key: " + key);
    }
}