package app.morphe.extension.reddit.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.reddit.settings.Settings;
import app.morphe.extension.reddit.settings.SettingsStatus;
import app.morphe.extension.reddit.settings.preference.TogglePreference;

import static app.morphe.extension.shared.utils.StringRef.dstr;
import static app.morphe.extension.shared.utils.StringRef.str;

@SuppressWarnings("deprecation")
public class AdsPreferenceCategory extends ConditionalPreferenceCategory {
    public AdsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(dstr("revanced_ads_category"));
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.adsCategoryEnabled();
    }

    @Override
    public void addPreferences(Context context) {
        addPreference(new TogglePreference(
                context,
                Settings.HIDE_COMMENT_ADS
        ));
        addPreference(new TogglePreference(
                context,
                Settings.HIDE_OLD_POST_ADS
        ));
        addPreference(new TogglePreference(
                context,
                Settings.HIDE_NEW_POST_ADS
        ));
    }
}
