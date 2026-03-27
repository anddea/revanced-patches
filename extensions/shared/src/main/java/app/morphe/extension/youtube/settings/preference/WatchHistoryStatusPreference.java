package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import app.morphe.extension.shared.patches.WatchHistoryPatch.WatchHistoryType;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public class WatchHistoryStatusPreference extends Preference {

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        // Because this listener may run before the ReVanced settings fragment updates Settings,
        // this could show the prior config and not the current.
        //
        // Push this call to the end of the main run queue,
        // so all other listeners are done and Settings is up to date.
        Utils.runOnMainThread(this::updateUI);
    };

    public WatchHistoryStatusPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public WatchHistoryStatusPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WatchHistoryStatusPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WatchHistoryStatusPreference(Context context) {
        super(context);
    }

    private void addChangeListener() {
        Setting.preferences.preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void removeChangeListener() {
        Setting.preferences.preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);
        updateUI();
        addChangeListener();
    }

    @Override
    protected void onPrepareForRemoval() {
        super.onPrepareForRemoval();
        removeChangeListener();
    }

    private void updateUI() {
        final WatchHistoryType watchHistoryType = Settings.WATCH_HISTORY_TYPE.get();
        final boolean blockWatchHistory = watchHistoryType == WatchHistoryType.BLOCK;
        final boolean replaceWatchHistory = watchHistoryType == WatchHistoryType.REPLACE;

        final String summaryTextKey;
        if (blockWatchHistory) {
            summaryTextKey = "revanced_watch_history_about_status_blocked";
        } else if (replaceWatchHistory) {
            summaryTextKey = "revanced_watch_history_about_status_replaced";
        } else {
            summaryTextKey = "revanced_watch_history_about_status_original";
        }

        setSummary(str(summaryTextKey));
    }
}