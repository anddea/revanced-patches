package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

import app.morphe.extension.shared.settings.preference.CustomDialogListPreference;
import app.morphe.extension.youtube.patches.video.CustomPlaybackSpeedPatch;

/**
 * A custom ListPreference that uses a styled custom dialog with a custom checkmark indicator.
 * Custom video speeds used by {@link CustomPlaybackSpeedPatch}.
 */
@SuppressWarnings({"unused", "deprecation"})
public final class CustomVideoSpeedListPreference extends CustomDialogListPreference {
    {
        setEntries(CustomPlaybackSpeedPatch.getEntries());
        setEntryValues(CustomPlaybackSpeedPatch.getEntryValues());
    }

    public CustomVideoSpeedListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomVideoSpeedListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomVideoSpeedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoSpeedListPreference(Context context) {
        super(context);
    }

}
