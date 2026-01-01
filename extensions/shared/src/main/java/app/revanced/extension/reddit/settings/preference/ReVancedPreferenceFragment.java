package app.revanced.extension.reddit.settings.preference;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import androidx.annotation.NonNull;

import app.revanced.extension.shared.utils.Utils;
import org.jetbrains.annotations.NotNull;

import app.revanced.extension.reddit.settings.preference.categories.AdsPreferenceCategory;
import app.revanced.extension.reddit.settings.preference.categories.LayoutPreferenceCategory;
import app.revanced.extension.reddit.settings.preference.categories.MiscellaneousPreferenceCategory;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment;

import static app.revanced.extension.shared.utils.StringRef.dstr;

/**
 * Preference fragment for ReVanced settings
 */
@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends AbstractPreferenceFragment {

    @Override
    protected void syncSettingWithPreference(@NonNull @NotNull Preference pref,
                                             @NonNull @NotNull Setting<?> setting,
                                             boolean applySettingToPreference) {
        super.syncSettingWithPreference(pref, setting, applySettingToPreference);
    }

    @Override
    protected void initialize() {
        final Context context = getContext();

        // Currently no resources can be compiled for Reddit (fails with aapt error).
        // So all Reddit Strings are hard coded in integrations.
        restartDialogTitle = Utils.getContext().getString(dstr("revanced_restart_dialog_title"));
        restartDialogMessage = Utils.getContext().getString(dstr("revanced_restart_dialog_message"));
        restartDialogButtonText = Utils.getContext().getString(dstr("revanced_restart_dialog_button"));

        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(preferenceScreen);

        // Custom categories reference app specific Settings class.
        new AdsPreferenceCategory(context, preferenceScreen);
        new LayoutPreferenceCategory(context, preferenceScreen);
        new MiscellaneousPreferenceCategory(context, preferenceScreen);
    }
}
