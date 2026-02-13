package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.morphe.extension.shared.patches.spoof.SpoofStreamingDataPatch;

@SuppressWarnings({"deprecation", "unused"})
public class HideAudioFlyoutMenuPreference extends SwitchPreference {
    {
        if (SpoofStreamingDataPatch.multiAudioTrackAvailable()) {
            String summaryOn = str("revanced_hide_player_flyout_menu_audio_track_summary_on");
            String summaryOff = str("revanced_hide_player_flyout_menu_audio_track_summary_off");
            setSummaryOn(summaryOn);
            setSummaryOff(summaryOff);
        } else { // Audio menu is not available if spoofing to Android client type.
            String summary = str("revanced_hide_player_flyout_menu_audio_track_not_available");
            setSummaryOn(summary);
            setSummaryOff(summary);
        }
    }

    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HideAudioFlyoutMenuPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HideAudioFlyoutMenuPreference(Context context) {
        super(context);
    }
}
