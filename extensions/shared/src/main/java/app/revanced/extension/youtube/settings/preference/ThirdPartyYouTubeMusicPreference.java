package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Arrays;

import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings({"unused", "deprecation"})
public class ThirdPartyYouTubeMusicPreference extends Preference implements Preference.OnPreferenceClickListener {

    private static final StringSetting settings = Settings.THIRD_PARTY_YOUTUBE_MUSIC_PACKAGE_NAME;
    private static final String[] mEntries = ResourceUtils.getStringArray("revanced_third_party_youtube_music_label");
    private static final String[] mEntryValues = ResourceUtils.getStringArray("revanced_third_party_youtube_music_package_name");

    @SuppressLint("StaticFieldLeak")
    private static EditText mEditText;
    private static String packageName;
    private static int mClickedDialogEntryIndex;

    private final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            packageName = s.toString();
            mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);
        }
    };

    private void init() {
        setSelectable(true);
        setOnPreferenceClickListener(this);
    }

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThirdPartyYouTubeMusicPreference(Context context) {
        super(context);
        init();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        packageName = settings.get();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);

        final Context context = getContext();
        AlertDialog.Builder builder = Utils.getEditTextDialogBuilder(context);

        TableLayout table = new TableLayout(context);
        table.setOrientation(LinearLayout.HORIZONTAL);
        table.setPadding(15, 0, 15, 0);

        TableRow row = new TableRow(context);

        mEditText = new EditText(context);
        mEditText.setHint(settings.defaultValue);
        mEditText.setText(packageName);
        mEditText.addTextChangedListener(textWatcher);
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9);
        mEditText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mEditText);

        table.addView(row);
        builder.setView(table);

        builder.setTitle(str("revanced_third_party_youtube_music_dialog_title"));
        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, (dialog, which) -> {
            mClickedDialogEntryIndex = which;
            mEditText.setText(mEntryValues[which]);
        });
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            final String packageName = mEditText.getText().toString().trim();
            settings.save(packageName);
            checkPackageIsValid(context, packageName);
            dialog.dismiss();
        });
        builder.setNeutralButton(str("revanced_extended_settings_reset"), (dialog, which) -> settings.resetToDefault());
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();

        return true;
    }

    private static void checkPackageIsValid(Context context, String packageName) {
        if (packageName.isEmpty()) {
            settings.resetToDefault();
            return;
        }

        String appName = "";
        if (mClickedDialogEntryIndex >= 0) {
            appName = mEntries[mClickedDialogEntryIndex];
        }

        showToastOrOpenWebsites(context, appName, packageName);
    }

    private static void showToastOrOpenWebsites(Context context, String appName, String packageName) {
        if (ExtendedUtils.isPackageEnabled(packageName)) {
            return;
        }

        Utils.showToastShort(str("revanced_third_party_youtube_music_not_installed_warning", appName.isEmpty() ? packageName : appName));
    }

}