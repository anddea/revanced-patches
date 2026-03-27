package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.util.AttributeSet;

@SuppressWarnings("unused")
public final class SpoofStreamingDataSideEffectsPreference extends SpoofVideoStreamsSideEffectsPreference {
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
}
