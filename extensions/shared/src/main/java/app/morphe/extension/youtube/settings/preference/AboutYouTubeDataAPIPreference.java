package app.morphe.extension.youtube.settings.preference;

import android.app.Activity;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import app.morphe.extension.shared.settings.preference.YouTubeDataAPIDialogBuilder;

@SuppressWarnings({"unused", "deprecation"})
public class AboutYouTubeDataAPIPreference extends Preference implements Preference.OnPreferenceClickListener {

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
    }

    public AboutYouTubeDataAPIPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AboutYouTubeDataAPIPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AboutYouTubeDataAPIPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AboutYouTubeDataAPIPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (getContext() instanceof Activity mActivity) {
            YouTubeDataAPIDialogBuilder.showDialog(mActivity);
        }

        return true;
    }
}