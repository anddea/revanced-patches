package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Allows tapping the Gemini about preference to open the Gemini website.
 */
@SuppressWarnings({"unused", "deprecation"})
public class AboutGeminiPreference extends Preference {
    {
        setOnPreferenceClickListener(pref -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://aistudio.google.com/apikey"));
            pref.getContext().startActivity(i);
            return false;
        });
    }

    public AboutGeminiPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public AboutGeminiPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AboutGeminiPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AboutGeminiPreference(Context context) {
        super(context);
    }
}
