package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.utils.ExtendedUtils.launchWebViewActivity;

import android.app.Dialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;

import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"FieldCanBeLocal", "deprecation", "unused"})
public class GetCookiesPreference extends Preference implements Preference.OnPreferenceClickListener {
    // Me at the zoo
    private final String YOUTUBE_EMBEDDED_PLAYER_URL =
            "https://www.youtube.com/embed/jNQXAC9IVRw?autoplay=1&mute=1&cc_load_policy=1";
    private final String YOUTUBE_SIGN_IN_URL =
            "https://www.youtube.com/signin";

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
        setEnabled(Settings.SET_TRANSCRIPT_COOKIES.get());
    }

    public GetCookiesPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public GetCookiesPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public GetCookiesPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GetCookiesPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Context context = getContext();
        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                context,
                // Title.
                str("revanced_transcript_cookies_dialog_title"),
                // Message.
                str("revanced_transcript_cookies_dialog_message"),
                // No EditText.
                null,
                // OK button text.
                str("revanced_transcript_cookies_dialog_auth_text"),
                // OK button action.
                () -> launchWebViewActivity(
                        context,
                        false,
                        false,
                        false,
                        YOUTUBE_SIGN_IN_URL
                ),
                // Cancel button action (dismiss only).
                null,
                // Neutral button text.
                str("revanced_transcript_cookies_dialog_no_auth_text"),
                // Neutral button action.
                () -> launchWebViewActivity(
                        context,
                        true,
                        true,
                        true,
                        YOUTUBE_EMBEDDED_PLAYER_URL
                ),
                // Dismiss dialog when onNeutralClick.
                true
        );
        dialogPair.first.show();
        return true;
    }
}