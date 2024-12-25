package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.sponsorblock.SponsorBlockSettings;

@SuppressWarnings({"unused", "deprecation"})
public class SponsorBlockImportExportPreference extends EditTextPreference implements Preference.OnPreferenceClickListener {

    private String existingSettings;

    @TargetApi(26)
    private void init() {
        setSelectable(true);

        EditText editText = getEditText();
        editText.setTextIsSelectable(true);
        editText.setAutofillHints((String) null);
        editText.setInputType(editText.getInputType()
                | InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8); // Use a smaller font to reduce text wrap.

        // If the user has a private user id, then include a subtext that mentions not to share it.
        String importExportSummary = SponsorBlockSettings.userHasSBPrivateId()
                ? str("revanced_sb_settings_ie_sum_warning")
                : str("revanced_sb_settings_ie_sum");
        setSummary(importExportSummary);

        setOnPreferenceClickListener(this);
    }

    public SponsorBlockImportExportPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SponsorBlockImportExportPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SponsorBlockImportExportPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SponsorBlockImportExportPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        try {
            // Must set text before preparing dialog, otherwise text is non selectable if this preference is later reopened.
            existingSettings = SponsorBlockSettings.exportDesktopSettings();
            getEditText().setText(existingSettings);
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
        return true;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        try {
            Utils.setEditTextDialogTheme(builder);
            super.onPrepareDialogBuilder(builder);
            // Show the user the settings in JSON format.
            builder.setTitle(getTitle());
            builder.setNeutralButton(str("revanced_sb_settings_copy"), (dialog, which) ->
                            Utils.setClipboard(getEditText().getText().toString(), str("revanced_sb_share_copy_settings_success")))
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            importSettings(getEditText().getText().toString()));
        } catch (Exception ex) {
            Logger.printException(() -> "onPrepareDialogBuilder failure", ex);
        }
    }

    private void importSettings(String replacementSettings) {
        try {
            if (replacementSettings.equals(existingSettings)) {
                return;
            }
            SponsorBlockSettings.importDesktopSettings(replacementSettings);
            SponsorBlockSettingsPreference.updateSegmentCategories();
            SponsorBlockSettingsPreference.fetchAndDisplayStats();
            SponsorBlockSettingsPreference.updateUI();
        } catch (Exception ex) {
            Logger.printException(() -> "importSettings failure", ex);
        }
    }

}