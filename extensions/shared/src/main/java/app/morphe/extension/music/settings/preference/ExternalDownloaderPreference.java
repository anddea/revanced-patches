package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.Arrays;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.utils.ExtendedUtils;
import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings("StringOperationCanBeSimplified")
public class ExternalDownloaderPreference {

    private static final StringSetting settings = Settings.EXTERNAL_DOWNLOADER_PACKAGE_NAME;
    private static final String[] mEntries = ResourceUtils.getStringArray("revanced_external_downloader_label");
    private static final String[] mEntryValues = ResourceUtils.getStringArray("revanced_external_downloader_package_name");
    private static final String[] mWebsiteEntries = ResourceUtils.getStringArray("revanced_external_downloader_website");
    private static String packageName;
    private static int mClickedDialogEntryIndex;

    private static final TextWatcher textWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        public void afterTextChanged(Editable s) {
            packageName = s.toString();
            mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);
        }
    };

    public static void showDialog(Activity mActivity) {
        packageName = settings.get();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);

        AlertDialog.Builder builder = getDialogBuilder(mActivity);

        TableLayout table = new TableLayout(mActivity);
        table.setOrientation(LinearLayout.HORIZONTAL);
        table.setPadding(15, 0, 15, 0);

        TableRow row = new TableRow(mActivity);

        EditText mEditText = new EditText(mActivity);
        mEditText.setText(packageName);
        mEditText.addTextChangedListener(textWatcher);
        mEditText.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9);
        mEditText.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(mEditText);

        table.addView(row);
        builder.setView(table);

        builder.setTitle(str("revanced_external_downloader_dialog_title"));
        builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex, (dialog, which) -> {
            mClickedDialogEntryIndex = which;
            mEditText.setText(mEntryValues[which].toString());
        });
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            final String packageName = mEditText.getText().toString().trim();
            settings.save(packageName);
            checkPackageIsValid(mActivity, packageName);
            dialog.dismiss();
        });
        builder.setNeutralButton(str("revanced_settings_reset"), (dialog, which) -> settings.resetToDefault());
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();
    }

    private static boolean checkPackageIsValid(Activity mActivity, String packageName) {
        String appName = "";
        String website = "";
        if (mClickedDialogEntryIndex >= 0) {
            appName = mEntries[mClickedDialogEntryIndex].toString();
            website = mWebsiteEntries[mClickedDialogEntryIndex].toString();
        }
        return showToastOrOpenWebsites(mActivity, appName, packageName, website);
    }

    private static boolean showToastOrOpenWebsites(Activity mActivity, String appName, String packageName, String website) {
        if (ExtendedUtils.isPackageEnabled(packageName))
            return true;

        if (website.isEmpty()) {
            Utils.showToastShort(str("revanced_external_downloader_not_installed_warning", packageName));
            return false;
        }

        getDialogBuilder(mActivity)
                .setTitle(str("revanced_external_downloader_not_installed_dialog_title"))
                .setMessage(str("revanced_external_downloader_not_installed_dialog_message", appName, appName))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                    mActivity.startActivity(i);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        return false;
    }

    public static boolean checkPackageIsEnabled() {
        final Activity mActivity = Utils.getActivity();
        packageName = settings.get().toString();
        mClickedDialogEntryIndex = Arrays.asList(mEntryValues).indexOf(packageName);
        return checkPackageIsValid(mActivity, packageName);
    }

}