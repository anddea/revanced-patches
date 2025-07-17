package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.dipToPixels;
import static app.revanced.extension.youtube.utils.ExtendedUtils.updateRadioGroup;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import app.revanced.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings({"unused", "deprecation"})
public class ExternalDownloaderVideoPreference extends ListPreference {

    private static final StringSetting settings = Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME_VIDEO;
    private static final String[] mEntries = ResourceUtils.getStringArray("revanced_external_downloader_video_label");
    private static final String[] mEntryValues = ResourceUtils.getStringArray("revanced_external_downloader_video_package_name");
    private static final String[] mWebsiteEntries = ResourceUtils.getStringArray("revanced_external_downloader_video_website");

    @SuppressLint("StaticFieldLeak")
    private static EditText mEditText;
    @SuppressLint("StaticFieldLeak")
    private static RadioGroup mRadioGroup;
    @SuppressLint("StaticFieldLeak")
    private static RadioGroup.OnCheckedChangeListener onCheckedChangeListener;
    @NonNull
    private static String packageName = "";
    private static int mClickedDialogEntryIndex;

    private final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            String newPackageName = s.toString();
            if (!packageName.equals(newPackageName)) {
                packageName = newPackageName;
                mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(newPackageName);
                updateRadioGroup(mRadioGroup, onCheckedChangeListener, mEntries, mClickedDialogEntryIndex);
            }
        }
    };

    public ExternalDownloaderVideoPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ExternalDownloaderVideoPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExternalDownloaderVideoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExternalDownloaderVideoPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        packageName = settings.get();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);

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
        mEditText.setText(packageName);
        mEditText.setSelection(packageName.length());
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
        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                context,
                str("revanced_external_downloader_dialog_title"), // Title.
                null, // No message (replaced by contentLayout).
                null, // No EditText.
                null, // OK button text.
                () -> {
                    // OK button action.
                    final String newValue = mEditText.getText().toString().trim();
                    if (callChangeListener(newValue)) {
                        setValue(newValue);
                    } else {
                        settings.save(newValue);
                    }
                    checkPackageIsValid(context, newValue);
                },
                () -> {
                }, // Cancel button action (dismiss only).
                str("revanced_extended_settings_reset"), // Neutral button text.
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

    @Override
    public void setSummary(CharSequence summary) {
        // Ignore calls to set the summary.
        // Summary is always the description of the category.
        //
        // This is required otherwise the ReVanced preference fragment
        // sets all ListPreference summaries to show the current selection.
    }

    private static boolean checkPackageIsValid(Context context, String packageName) {
        String appName = "";
        String website = "";

        if (mClickedDialogEntryIndex >= 0) {
            appName = mEntries[mClickedDialogEntryIndex];
            website = mWebsiteEntries[mClickedDialogEntryIndex];
        }

        return showToastOrOpenWebsites(context, appName, packageName, website);
    }

    private static boolean showToastOrOpenWebsites(Context context, String appName, String packageName, String website) {
        if (ExtendedUtils.isPackageEnabled(packageName))
            return true;

        if (website.isEmpty()) {
            Utils.showToastShort(str("revanced_external_downloader_not_installed_warning", packageName));
            return false;
        }

        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                context,
                // Title.
                str("revanced_external_downloader_not_installed_dialog_title"),
                // Message.
                str("revanced_external_downloader_not_installed_dialog_message", appName, appName),
                // No EditText.
                null,
                // OK button text.
                null,
                // OK button action.
                () -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                    context.startActivity(i);
                },
                // Cancel button action (dismiss only).
                () -> {
                },
                // Neutral button text.
                null,
                // Neutral button action.
                null,
                // Dismiss dialog when onNeutralClick.
                false
        );

        dialogPair.first.show();

        return false;
    }

    public static boolean checkPackageIsDisabled() {
        final Context context = Utils.getActivity();
        packageName = settings.get();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);
        return !checkPackageIsValid(context, packageName);
    }

    public static String getExternalDownloaderPackageName() {
        String downloaderPackageName = settings.get().trim();

        if (downloaderPackageName.isEmpty()) {
            settings.resetToDefault();
            downloaderPackageName = settings.defaultValue;
        }

        return downloaderPackageName;
    }
}