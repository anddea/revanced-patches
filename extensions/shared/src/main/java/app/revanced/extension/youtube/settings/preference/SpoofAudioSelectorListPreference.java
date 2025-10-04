package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.content.Context;
import android.util.AttributeSet;

import app.revanced.extension.shared.innertube.client.YouTubeClient.ClientType;
import app.revanced.extension.shared.patches.PatchStatus;
import app.revanced.extension.shared.settings.preference.SortedListPreference;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public class SpoofAudioSelectorListPreference extends SortedListPreference {

    private final boolean available;

    {
        final ClientType defaultClient =
                Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get();
        final boolean forcedOriginalAudioTrackEnabled =
                PatchStatus.SpoofStreamingData() && Settings.SPOOF_STREAMING_DATA.get() &&
                        defaultClient.getSupportsCookies() &&
                        defaultClient.getSupportsMultiAudioTracks() &&
                        Settings.DISABLE_AUTO_AUDIO_TRACKS.get();
        final boolean disabledByClient = defaultClient == ClientType.ANDROID_CREATOR ||
                defaultClient == ClientType.ANDROID_VR_AUTH ||
                defaultClient == ClientType.IPADOS;

        if (forcedOriginalAudioTrackEnabled || disabledByClient) {
            available = false;
            String summary = disabledByClient
                    ? str("revanced_spoof_streaming_data_no_auth_language_not_available_by_client", defaultClient.getFriendlyName())
                    : str("revanced_spoof_streaming_data_no_auth_language_not_available");
            super.setEnabled(false);
            super.setSummary(summary);
        } else {
            available = true;
        }
    }

    public SpoofAudioSelectorListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofAudioSelectorListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofAudioSelectorListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofAudioSelectorListPreference(Context context) {
        super(context);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!available) {
            return;
        }

        super.setEnabled(enabled);
    }

    @Override
    public void setSummary(CharSequence summary) {
        if (!available) {
            return;
        }

        super.setSummary(summary);
    }
}

