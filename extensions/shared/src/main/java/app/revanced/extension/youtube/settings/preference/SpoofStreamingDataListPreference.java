package app.revanced.extension.youtube.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch;
import app.revanced.extension.shared.settings.preference.CustomDialogListPreference;

/**
 * A custom ListPreference that uses a styled custom dialog with a custom checkmark indicator.
 * Default client used by {@link SpoofStreamingDataPatch}.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class SpoofStreamingDataListPreference extends CustomDialogListPreference {
    {
        setEntries(SpoofStreamingDataPatch.getEntries());
        setEntryValues(SpoofStreamingDataPatch.getEntryValues());
    }

    public SpoofStreamingDataListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofStreamingDataListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofStreamingDataListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofStreamingDataListPreference(Context context) {
        super(context);
    }

}
