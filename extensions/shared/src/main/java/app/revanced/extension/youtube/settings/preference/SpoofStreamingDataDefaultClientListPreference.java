package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.ResourceUtils.getStringArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import app.revanced.extension.shared.patches.client.AppClient.ClientType;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public class SpoofStreamingDataDefaultClientListPreference extends ListPreference {

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        // Because this listener may run before the ReVanced settings fragment updates Settings,
        // this could show the prior config and not the current.
        //
        // Push this call to the end of the main run queue,
        // so all other listeners are done and Settings is up to date.
        Utils.runOnMainThread(this::updateUI);
    };

    public SpoofStreamingDataDefaultClientListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofStreamingDataDefaultClientListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofStreamingDataDefaultClientListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofStreamingDataDefaultClientListPreference(Context context) {
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
        final boolean spoofStreamingDataAndroidOnly = Settings.SPOOF_STREAMING_DATA_ANDROID_ONLY.get();
        final String entryKey = spoofStreamingDataAndroidOnly
                ? "revanced_spoof_streaming_data_type_android_entries"
                : "revanced_spoof_streaming_data_type_android_ios_entries";
        final String entryValueKey = spoofStreamingDataAndroidOnly
                ? "revanced_spoof_streaming_data_type_android_entry_values"
                : "revanced_spoof_streaming_data_type_android_ios_entry_values";
        final String[] mEntries = getStringArray(entryKey);
        final String[] mEntryValues = getStringArray(entryValueKey);
        setEntries(mEntries);
        setEntryValues(mEntryValues);

        final EnumSetting<ClientType> clientType = Settings.SPOOF_STREAMING_DATA_TYPE;
        final boolean isAndroid = clientType.get().name().startsWith("ANDROID");
        if (spoofStreamingDataAndroidOnly && !isAndroid) {
            clientType.resetToDefault();
        }

        setEnabled(Settings.SPOOF_STREAMING_DATA.get());
    }
}
