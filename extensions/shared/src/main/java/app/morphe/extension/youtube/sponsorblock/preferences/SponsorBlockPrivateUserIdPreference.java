/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.sponsorblock.preferences;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.shared.settings.preference.ResettableEditTextPreference;
import app.morphe.extension.youtube.sponsorblock.SponsorBlockSettings;
import app.morphe.extension.shared.ui.CustomDialog;

/**
 * EditTextPreference whose dialog adds a Copy action and validates the user ID before saving.
 */
@SuppressWarnings({"unused", "deprecation"})
public class SponsorBlockPrivateUserIdPreference extends ResettableEditTextPreference {

    public SponsorBlockPrivateUserIdPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        installChangeListener();
    }

    public SponsorBlockPrivateUserIdPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        installChangeListener();
    }

    public SponsorBlockPrivateUserIdPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        installChangeListener();
    }

    public SponsorBlockPrivateUserIdPreference(Context context) {
        super(context);
        installChangeListener();
    }

    private void installChangeListener() {
        setOnPreferenceChangeListener((p, newValue) -> {
            String newUUID = newValue.toString();
            if (!SponsorBlockSettings.isValidSBUserId(newUUID)) {
                Utils.showToastLong(getInvalidUserIdMessage());
                return false;
            }
            return true;
        });
    }

    protected String getInvalidUserIdMessage() {
        return str("revanced_sb_general_uuid_invalid");
    }

    protected String getCopyButtonText() {
        return str("revanced_settings_import_copy");
    }

    @Override
    protected void showDialog(@Nullable Bundle state) {
        try {
            Context context = getContext();
            EditText editText = getEditText();

            String initialValue = getText() != null ? getText() : "";
            editText.setText(initialValue);
            editText.setSelection(initialValue.length());

            Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                    context,
                    getTitle() != null ? getTitle().toString() : "",
                    null,
                    editText,
                    null,
                    () -> {
                        String newValue = editText.getText().toString();
                        if (callChangeListener(newValue)) {
                            setText(newValue);
                        }
                    },
                    () -> {},
                    getCopyButtonText(),
                    () -> {
                        try {
                            Utils.setClipboard(getEditText().getText().toString());
                        } catch (Exception ex) {
                            Logger.printException(() -> "Copy private user ID failure", ex);
                        }
                    },
                    true
            );

            dialogPair.first.setCancelable(true);
            dialogPair.first.show();
        } catch (Exception ex) {
            Logger.printException(() -> "showDialog failure", ex);
        }
    }
}
