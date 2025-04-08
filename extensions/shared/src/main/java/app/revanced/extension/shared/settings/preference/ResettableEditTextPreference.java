package app.revanced.extension.shared.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.Objects;

import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public class ResettableEditTextPreference extends EditTextPreference {

    /**
     * Setting to reset.
     */
    @Nullable
    private Setting<?> setting;

    public ResettableEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ResettableEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ResettableEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResettableEditTextPreference(Context context) {
        super(context);
    }

    public void setSetting(@Nullable Setting<?> setting) {
        this.setting = setting;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        Utils.setEditTextDialogTheme(builder);
        super.onPrepareDialogBuilder(builder);

        final CharSequence title = getTitle();
        if (title != null) {
            builder.setTitle(getTitle());
        }
        if (setting == null) {
            String key = getKey();
            if (key != null) {
                setting = Setting.getSettingFromPath(key);
            }
        }
        if (setting != null) {
            builder.setNeutralButton(str("revanced_extended_settings_reset"), null);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        if (!(getDialog() instanceof AlertDialog alertDialog)) {
            return;
        }

        // Override the button click listener to prevent dismissing the dialog.
        Button button = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (button == null) {
            return;
        }
        button.setOnClickListener(v -> {
            try {
                String defaultStringValue = Objects.requireNonNull(setting).defaultValue.toString();
                EditText editText = getEditText();
                editText.setText(defaultStringValue);
                editText.setSelection(defaultStringValue.length()); // move cursor to end of text
            } catch (Exception ex) {
                Logger.printException(() -> "reset failure", ex);
            }
        });
    }
}
