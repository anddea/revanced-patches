package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.morphe.extension.shared.patches.spoof.SpoofStreamingDataPatch;

@SuppressWarnings({"deprecation", "unused"})
public class ForceOriginalAudioSwitchPreference extends SwitchPreference {
    {
        String summaryOn = SpoofStreamingDataPatch.multiAudioTrackAvailable()
                ? "revanced_disable_auto_audio_tracks_summary_on"
                : "revanced_disable_auto_audio_tracks_summary_on_disclaimer";
        setSummaryOn(str(summaryOn));
        setSummaryOff(str("revanced_disable_auto_audio_tracks_summary_off"));
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
