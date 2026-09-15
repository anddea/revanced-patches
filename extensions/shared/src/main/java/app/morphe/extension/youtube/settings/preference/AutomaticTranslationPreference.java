package app.morphe.extension.youtube.settings.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import app.morphe.extension.youtube.settings.Settings;

/** Only one provider should automatically translate a newly opened video. */
@SuppressWarnings({"deprecation", "unused"})
public class AutomaticTranslationPreference extends SwitchPreference {
    public AutomaticTranslationPreference(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onClick() {
        super.onClick();
        if (!isChecked()) return;
        if ("vot_auto_translate".equals(getKey())) Settings.GOOGLE_VOT_AUTO_TRANSLATE.save(false);
        else Settings.VOT_AUTO_TRANSLATE.save(false);
    }
}
