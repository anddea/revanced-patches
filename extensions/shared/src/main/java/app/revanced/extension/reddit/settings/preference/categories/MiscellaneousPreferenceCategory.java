package app.revanced.extension.reddit.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.revanced.extension.reddit.settings.Settings;
import app.revanced.extension.reddit.settings.SettingsStatus;
import app.revanced.extension.reddit.settings.preference.TogglePreference;

import static app.revanced.extension.shared.utils.StringRef.dstr;
import static app.revanced.extension.shared.utils.StringRef.str;

@SuppressWarnings("deprecation")
public class MiscellaneousPreferenceCategory extends ConditionalPreferenceCategory {
    public MiscellaneousPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(dstr("revanced_misc_category"));
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.miscellaneousCategoryEnabled();
    }

    @Override
    public void addPreferences(Context context) {
        if (SettingsStatus.openLinksDirectlyEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.OPEN_LINKS_DIRECTLY
            ));
        }
        if (SettingsStatus.openLinksExternallyEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.OPEN_LINKS_EXTERNALLY
            ));
        }
        if (SettingsStatus.sanitizeUrlQueryEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.SANITIZE_URL_QUERY
            ));
        }
    }
}
