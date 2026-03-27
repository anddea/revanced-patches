package app.morphe.extension.shared.settings.preference;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

import android.content.Context;
import android.preference.SwitchPreference;
import android.text.Html;
import android.util.AttributeSet;

/**
 * Allows using basic html for the summary text.
 */
@SuppressWarnings({"unused", "deprecation"})
public class HtmlSwitchPreference extends SwitchPreference {
    {
        setSummaryOn(Html.fromHtml(getSummaryOn().toString(), FROM_HTML_MODE_COMPACT));
        setSummaryOff(Html.fromHtml(getSummaryOff().toString(), FROM_HTML_MODE_COMPACT));
    }

    public HtmlSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public HtmlSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public HtmlSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlSwitchPreference(Context context) {
        super(context);
    }
}