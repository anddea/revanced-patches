package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.LinearLayout;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings({"deprecation", "unused"})
public class AboutFakeBufferingPreference extends Preference implements Preference.OnPreferenceClickListener {

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
        setEnabled(Settings.SPOOF_STREAMING_DATA.get() &&
                Settings.SPOOF_STREAMING_DATA_USE_JS.get() &&
                Settings.SPOOF_STREAMING_DATA_DEFAULT_CLIENT.get().getRequireJS());
    }

    public AboutFakeBufferingPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public AboutFakeBufferingPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public AboutFakeBufferingPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AboutFakeBufferingPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Context context = getContext();
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                // Title.
                str("revanced_spoof_streaming_data_use_js_bypass_fake_buffering_dialog_title"),
                // Message.
                str("revanced_spoof_streaming_data_use_js_bypass_fake_buffering_dialog_message"),
                // No EditText.
                null,
                // OK button text.
                null,
                // OK button action (dismiss only).
                () -> {
                },
                // No Cancel button action.
                null,
                // Neutral button text.
                str("revanced_spoof_streaming_data_use_js_bypass_fake_buffering_dialog_open_text"),
                // Neutral button action.
                () -> {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://iter.ca/post/yt-adblock/"));
                    context.startActivity(i);
                },
                // Dismiss dialog when onNeutralClick.
                true
        );
        dialogPair.first.show();
        return true;
    }
}