package app.morphe.extension.reddit.settings.preference;

import android.content.Context;
import android.preference.SwitchPreference;

import app.morphe.extension.shared.settings.BooleanSetting;

import static app.morphe.extension.shared.utils.StringRef.dstr;

@SuppressWarnings("deprecation")
public class TogglePreference extends SwitchPreference {
    public TogglePreference(Context context, BooleanSetting setting) {
        super(context);
        this.setTitle(dstr(setting.key + "_title"));
        this.setSummary(dstr(setting.key + "_summary"));
        this.setKey(setting.key);
        this.setChecked(setting.get());
    }
}
