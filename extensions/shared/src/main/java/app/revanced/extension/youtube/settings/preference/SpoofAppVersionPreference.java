package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.dipToPixels;
import static app.revanced.extension.youtube.utils.ExtendedUtils.updateRadioGroup;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;

import androidx.annotation.NonNull;

import java.util.Arrays;

import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings({"unused", "deprecation"})
public class SpoofAppVersionPreference extends ListPreference {
    private final StringSetting settings = Settings.SPOOF_APP_VERSION_TARGET;
    private final String[] mEntries = ResourceUtils.getEntry(settings);
    private final String[] mEntryValues = ResourceUtils.getEntryValue(settings);

    private EditText mEditText;
    private RadioGroup mRadioGroup;
    private RadioGroup.OnCheckedChangeListener onCheckedChangeListener;
    @NonNull
    private String spoofAppVersion = "";
    private int mClickedDialogEntryIndex;

    private final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            String newValue = s.toString();
            if (!spoofAppVersion.equals(newValue)) {
                spoofAppVersion = newValue;
                mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(newValue);
                updateRadioGroup(mRadioGroup, onCheckedChangeListener, mEntries, mClickedDialogEntryIndex);
            }
        }
    };

    public SpoofAppVersionPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SpoofAppVersionPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SpoofAppVersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpoofAppVersionPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        spoofAppVersion = settings.get();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(spoofAppVersion);

        final Context context = getContext();

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Add behavior selection radio buttons.
        mRadioGroup = new RadioGroup(context);
        mRadioGroup.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < mEntries.length; i++) {
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(mEntries[i]);
            radioButton.setId(i);
            radioButton.setChecked(i == mClickedDialogEntryIndex);
            mRadioGroup.addView(radioButton);
        }
        onCheckedChangeListener = (group, checkedId) -> {
            String newValue = mEntryValues[checkedId];
            mClickedDialogEntryIndex = checkedId;
            mEditText.setText(newValue);
            mEditText.setSelection(newValue.length());
        };
        mRadioGroup.setOnCheckedChangeListener(onCheckedChangeListener);
        mRadioGroup.setPadding(dipToPixels(10), 0, 0, 0);
        contentLayout.addView(mRadioGroup);

        TableLayout table = new TableLayout(context);
        table.setOrientation(LinearLayout.HORIZONTAL);
        table.setPadding(15, 0, 15, 0);

        TableRow row = new TableRow(context);

        mEditText = new EditText(context);
        mEditText.setHint(settings.defaultValue);
        mEditText.setText(spoofAppVersion);
        mEditText.setSelection(spoofAppVersion.length());
        mEditText.addTextChangedListener(textWatcher);
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9);
        mEditText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mEditText);

        table.addView(row);
        contentLayout.addView(table);

        // Create ScrollView to wrap the content layout.
        ScrollView contentScrollView = new ScrollView(context);
        contentScrollView.setVerticalScrollBarEnabled(false); // Disable vertical scrollbar.
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); // Disable overscroll effect.
        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        contentScrollView.setLayoutParams(scrollViewParams);
        contentScrollView.addView(contentLayout);

        // Create the custom dialog.
        // OK button action.
        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                context,
                str(settings.key + "_title"), // Title.
                null, // No message (replaced by contentLayout).
                null, // No EditText.
                null, // OK button text.
                // OK button action.
                () -> {
                    String newValue = mEditText.getText().toString().trim();
                    if (callChangeListener(newValue)) {
                        setValue(newValue);
                    } else {
                        settings.save(newValue);
                    }
                },
                () -> {
                }, // Cancel button action (dismiss only).
                str("revanced_extended_settings_reset"), // Neutral button text.
                // Neutral button action.
                () -> {
                    final String newValue = settings.defaultValue;
                    mEditText.setText(newValue);
                    mEditText.setSelection(newValue.length());
                },
                false  // Dismiss dialog when onNeutralClick.
        );

        // Add the ScrollView to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentScrollView, dialogMainLayout.getChildCount() - 1);
        // Show the dialog.
        dialogPair.first.show();
    }
}