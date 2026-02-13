package app.morphe.extension.reddit.settings.preference.categories;

import android.content.Context;
import android.preference.PreferenceScreen;

import app.morphe.extension.reddit.settings.Settings;
import app.morphe.extension.reddit.settings.SettingsStatus;
import app.morphe.extension.reddit.settings.preference.TogglePreference;

import static app.morphe.extension.shared.utils.StringRef.dstr;
import static app.morphe.extension.shared.utils.StringRef.str;

@SuppressWarnings("deprecation")
public class LayoutPreferenceCategory extends ConditionalPreferenceCategory {
    public LayoutPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(dstr("revanced_layout_category"));
    }

    @Override
    public boolean getSettingsStatus() {
        return SettingsStatus.layoutCategoryEnabled();
    }

    @Override
    public void addPreferences(Context context) {
        if (SettingsStatus.screenshotPopupEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.DISABLE_SCREENSHOT_POPUP
            ));
        }
        if (SettingsStatus.navigationButtonsEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_CHAT_BUTTON
            ));
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_CREATE_BUTTON
            ));
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_DISCOVER_BUTTON
            ));
        }
        if (SettingsStatus.recentlyVisitedShelfEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_RECENTLY_VISITED_SHELF
            ));
        }
        if (SettingsStatus.recommendedCommunitiesShelfEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_RECOMMENDED_COMMUNITIES_SHELF
            ));
        }
        if (SettingsStatus.toolBarButtonEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_TOOLBAR_BUTTON
            ));
        }
        if (SettingsStatus.trendingTodayShelfEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.HIDE_TRENDING_TODAY_SHELF
            ));
        }
        if (SettingsStatus.subRedditDialogEnabled) {
            addPreference(new TogglePreference(
                    context,
                    Settings.REMOVE_NSFW_DIALOG
            ));
            addPreference(new TogglePreference(
                    context,
                    Settings.REMOVE_NOTIFICATION_DIALOG
            ));
        }
    }
}
