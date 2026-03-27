package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.youtube.utils.ExtendedUtils.IS_19_29_OR_GREATER;
import static app.morphe.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.morphe.extension.youtube.patches.utils.PatchStatus;

@SuppressWarnings({"deprecation", "unused"})
public class OverridePlaylistDownloadButtonPreference extends SwitchPreference {
    {
        String summaryOn = "revanced_override_playlist_download_button_summary_on";
        if (IS_19_29_OR_GREATER) {
            if (!PatchStatus.SpoofAppVersion()) {
                summaryOn = "revanced_override_playlist_download_button_summary_on_disclaimer_1";
            } else if (!isSpoofingToLessThan("19.29.00")) {
                summaryOn = "revanced_override_playlist_download_button_summary_on_disclaimer_2";
            }
        }

        setSummaryOn(str(summaryOn));
    }

    public OverridePlaylistDownloadButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public OverridePlaylistDownloadButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OverridePlaylistDownloadButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OverridePlaylistDownloadButtonPreference(Context context) {
        super(context);
    }
}
