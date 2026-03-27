package app.morphe.extension.reddit.settings.preference;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.Preference;

import static app.morphe.extension.shared.utils.StringRef.dstr;

@SuppressWarnings("deprecation")
public class LinkPreference extends Preference {

    public LinkPreference(Context context, String titleKey, String url) {
        super(context);

        this.setTitle(dstr(titleKey));
        this.setSummary(url);

        this.setOnPreferenceClickListener(pref -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                pref.getContext().startActivity(i);
            } catch (Exception ignored) {
                // Prevent crash if no browser is installed
            }
            return true;
        });
    }
}
