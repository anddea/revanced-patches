package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"unused", "deprecation"})
public class ImportExportPreference extends EditTextPreference implements Preference.OnPreferenceClickListener {

    private String existingSettings;

    private void init() {
        setSelectable(true);

        EditText editText = getEditText();
        editText.setTextIsSelectable(true);
        if (Utils.isSDKAbove(26)) {
            editText.setAutofillHints((String) null);
        }
        editText.setInputType(editText.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8); // Use a smaller font to reduce text wrap.

        setOnPreferenceClickListener(this);
    }

    public ImportExportPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ImportExportPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ImportExportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ImportExportPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        try {
            // Must set text before preparing dialog, otherwise text is non selectable if this preference is later reopened.
            existingSettings = Setting.exportToJson(null);
            getEditText().setText(existingSettings);
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
        return true;
    }

    @Override
    protected void showDialog(Bundle state) {
        try {
            Context context = getContext();
            EditText editText = getEditText();

            // Create a custom dialog with the EditText.
            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                    context,
                    str("revanced_preference_screen_import_export_title"), // Title.
                    null,     // No message (EditText replaces it).
                    editText, // Pass the EditText.
                    str("revanced_settings_import"), // OK button text.
                    () -> importSettings(context, editText.getText().toString()), // OK button action.
                    () -> {
                    }, // Cancel button action (dismiss only).
                    str("revanced_settings_import_copy"), // Neutral button (Copy) text.
                    () -> {
                        // Neutral button (Copy) action. Show the user the settings in JSON format.
                        Utils.setClipboard(editText.getText().toString(), str("revanced_share_copy_settings_success"));
                    },
                    true // Dismiss dialog when onNeutralClick.
            );

            // Show the dialog.
            dialogPair.first.show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }

    private void importSettings(Context context, String replacementSettings) {
        try {
            if (replacementSettings.equals(existingSettings)) {
                return;
            }
            AbstractPreferenceFragment.settingImportInProgress = true;
            final boolean rebootNeeded = Setting.importFromJSON(context, replacementSettings);
            if (rebootNeeded) {
                AbstractPreferenceFragment.showRestartDialog(getContext());
            }
        } catch (Exception ex) {
            Logger.printException(() -> "importSettings failure", ex);
        } finally {
            AbstractPreferenceFragment.settingImportInProgress = false;
        }
    }

}