package app.morphe.extension.shared.settings.preference;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import app.morphe.extension.shared.settings.Setting;
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
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 7); // Use a smaller font to reduce text wrap.

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
            // Must set text before showing dialog,
            // otherwise text is non-selectable if this preference is later reopened.
            existingSettings = Setting.exportToJson(getContext());
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
                    // Title.
                    str("revanced_preference_screen_import_export_title"),
                    // Message.
                    null,
                    // EditText.
                    editText,
                    // OK button text.
                    str("revanced_settings_import"),
                    // OK button action.
                    () -> importSettings(context, editText.getText().toString()),
                    // Cancel button action.
                    () -> {
                    },
                    // Neutral button text.
                    str("revanced_settings_import_copy"),
                    // Neutral button action.
                    () -> Utils.setClipboard(editText.getText().toString()),
                    // Dismiss dialog when onNeutralClick.
                    true
            );

            // If there are no settings yet, then show the on screen keyboard and bring focus to
            // the edit text. This makes it easier to paste saved settings after a reinstall.
            dialogPair.first.setOnShowListener(dialogInterface -> {
                if (existingSettings.isEmpty()) {
                    editText.postDelayed(() -> {
                        editText.requestFocus();

                        if (editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)
                                instanceof InputMethodManager inputMethodManager) {
                            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }, 100);
                }
            });

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
                AbstractPreferenceFragment.showRestartDialog(context);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "importSettings failure", ex);
        } finally {
            AbstractPreferenceFragment.settingImportInProgress = false;
        }
    }

}