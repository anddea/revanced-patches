package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.patches.utils.PatchStatus.SPOOF_APP_VERSION_TARGET_DEFAULT_VALUE;
import static app.revanced.extension.youtube.patches.utils.PatchStatus.SpoofAppVersionDefaultString;
import static app.revanced.extension.youtube.utils.ExtendedUtils.IS_19_29_OR_GREATER;
import static app.revanced.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

@SuppressWarnings({"deprecation", "unused"})
public class OverridePlaylistDownloadButtonPreference extends SwitchPreference {
    {
        boolean playlistDownloadButtonMayNotShown = IS_19_29_OR_GREATER
                && !SPOOF_APP_VERSION_TARGET_DEFAULT_VALUE.equals(SpoofAppVersionDefaultString())
                && !isSpoofingToLessThan("19.29.00");

        String summaryOn = playlistDownloadButtonMayNotShown
                ? "revanced_override_playlist_download_button_summary_on_disclaimer"
                : "revanced_override_playlist_download_button_summary_on";

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
