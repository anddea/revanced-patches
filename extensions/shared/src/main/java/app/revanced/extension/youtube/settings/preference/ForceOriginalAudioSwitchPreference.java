package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch;

@SuppressWarnings({"deprecation", "unused"})
public class ForceOriginalAudioSwitchPreference extends SwitchPreference {
    {
        if (SpoofStreamingDataPatch.notSpoofingToAndroid()) {
            String summaryOn = str("revanced_disable_auto_audio_tracks_summary_on");
            String summaryOff = str("revanced_disable_auto_audio_tracks_summary_off");
            setSummaryOn(summaryOn);
            setSummaryOff(summaryOff);
        } else { // Show why force audio is not available.
            String summary = str("revanced_disable_auto_audio_tracks_not_available");
            setSummaryOn(summary);
            setSummaryOff(summary);
        }
    }

    public ForceOriginalAudioSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ForceOriginalAudioSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ForceOriginalAudioSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ForceOriginalAudioSwitchPreference(Context context) {
        super(context);
    }
}
