package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import app.revanced.extension.shared.patches.client.AppClient.ClientType;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public class SpoofStreamingDataSideEffectsPreference extends Preference {

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        // Because this listener may run before the ReVanced settings fragment updates Settings,
        // this could show the prior config and not the current.
        //
        // Push this call to the end of the main run queue,
        // so all other listeners are done and Settings is up to date.
        Utils.runOnMainThread(this::updateUI);
    };

    public SpoofStreamingDataSideEffectsPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofStreamingDataSideEffectsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofStreamingDataSideEffectsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofStreamingDataSideEffectsPreference(Context context) {
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
        final ClientType clientType = Settings.SPOOF_STREAMING_DATA_TYPE.get();

        final String summaryTextKey;
        if (clientType == ClientType.IOS && Settings.SPOOF_STREAMING_DATA_IOS_SKIP_LIVESTREAM_PLAYBACK.get()) {
            summaryTextKey = "revanced_spoof_streaming_data_side_effects_ios_skip_livestream_playback";
        } else {
            summaryTextKey = "revanced_spoof_streaming_data_side_effects_" + clientType.name().toLowerCase();
        }

        setSummary(str(summaryTextKey));
        setEnabled(Settings.SPOOF_STREAMING_DATA.get());
        setSelectable(false);
    }
}