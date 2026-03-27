package app.morphe.extension.shared.settings.preference;

import static app.morphe.extension.shared.utils.Utils.setHtml;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Allows using basic html for the summary text.
 */
@SuppressWarnings({"unused", "deprecation"})
public class HtmlPreference extends Preference {
    {
        setSummary(setHtml(getSummary().toString()));
    }

    public HtmlPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public HtmlPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HtmlPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlPreference(Context context) {
        super(context);
    }
}