package app.morphe.extension.shared.settings.preference;

import static app.morphe.extension.shared.patches.PatchStatus.PatchVersion;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.isSDKAbove;
import static app.morphe.extension.shared.utils.Utils.showToastShort;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Insets;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import app.morphe.extension.shared.settings.BaseHostActivity;
import app.morphe.extension.shared.settings.BaseSettings;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.PackageUtils;
import app.morphe.extension.shared.utils.Utils;

@SuppressWarnings({"deprecation", "NewApi"})
public class ToolbarPreferenceFragment extends AbstractPreferenceFragment {
    protected final int READ_REQUEST_CODE = 42;
    protected final int WRITE_REQUEST_CODE = 43;

    /**
     * Sets toolbar for all nested preference screens.
     */
    protected void setPreferenceScreenToolbar(PreferenceScreen parentScreen) {
        Integer targetSDKVersion = PackageUtils.getTargetSDKVersion(getContext().getPackageName());
        boolean isEdgeToEdgeSupported = isSDKAbove(35) && targetSDKVersion != null && targetSDKVersion >= 35;

        for (int i = 0, count = parentScreen.getPreferenceCount(); i < count; i++) {
            Preference childPreference = parentScreen.getPreference(i);
            if (childPreference instanceof PreferenceScreen screen) {
                // Recursively set sub preferences.
                setPreferenceScreenToolbar(screen);

                childPreference.setOnPreferenceClickListener(
                        childScreen -> {
                            Dialog preferenceScreenDialog = screen.getDialog();
                            ViewGroup rootView = (ViewGroup) preferenceScreenDialog
                                    .findViewById(android.R.id.content)
                                    .getParent();

                            // Allow package-specific background customization.
                            BaseThemeUtils.customizeDialogBackground(rootView);

                            // Fix the system navigation bar color for submenus.
                            BaseThemeUtils.setNavigationBarColor(preferenceScreenDialog.getWindow());

                            // Fix edge-to-edge screen with Android 15 and YT 19.44+
                            // https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
                            if (isEdgeToEdgeSupported) {
                                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                                    Insets statusInsets = insets.getInsets(WindowInsets.Type.statusBars());
                                    Insets navInsets = insets.getInsets(WindowInsets.Type.navigationBars());
                                    v.setPadding(0, statusInsets.top, 0, navInsets.bottom);
                                    return insets;
                                });
                            }

                            Toolbar toolbar = new Toolbar(childScreen.getContext());
                            toolbar.setTitle(childScreen.getTitle());
                            toolbar.setNavigationIcon(BaseThemeUtils.getBackButtonDrawable());
                            toolbar.setNavigationOnClickListener(view -> preferenceScreenDialog.dismiss());

                            final int margin = Utils.dipToPixels(16);
                            toolbar.setTitleMargin(margin, 0, margin, 0);

                            TextView toolbarTextView = Utils.getChildView(toolbar,
                                    true, TextView.class::isInstance);
                            if (toolbarTextView != null) {
                                toolbarTextView.setTextColor(BaseThemeUtils.getAppForegroundColor());
                                toolbarTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                            }

                            // Allow package-specific toolbar customization.
                            customizeToolbar(toolbar);

                            // Allow package-specific post-toolbar setup.
                            onPostToolbarSetup(toolbar, preferenceScreenDialog);

                            rootView.addView(toolbar, 0);
                            return false;
                        }
                );
            }
        }
    }

    /**
     * Allows subclasses to customize the toolbar.
     */
    protected void customizeToolbar(Toolbar toolbar) {
        BaseHostActivity.setToolbarLayoutParams(toolbar);
    }

    /**
     * Allows subclasses to perform actions after toolbar setup.
     */
    protected void onPostToolbarSetup(Toolbar toolbar, Dialog preferenceScreenDialog) {
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    @SuppressLint("SimpleDateFormat")
    protected void exportActivity() {
        if (!settingExportInProgress && !BaseSettings.DEBUG.get()) {
            Utils.showToastShort(str("revanced_debug_logs_disabled"));
            return;
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final String appName = PackageUtils.getAppLabel();
        final String versionName = PackageUtils.getAppVersionName();
        final String formatDate = dateFormat.format(new Date(System.currentTimeMillis()));
        final StringBuilder sb = new StringBuilder();
        sb.append(appName);
        sb.append("_v");
        sb.append(versionName);
        String patchVersion = PatchVersion();
        if (!"Unknown".equals(patchVersion)) {
            sb.append("_rvp_v");
            sb.append(patchVersion);
        }
        sb.append("_");
        if (settingExportInProgress) {
            sb.append("settings");
        } else {
            sb.append("log");
        }
        sb.append("_");
        sb.append(formatDate);

        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, sb.toString());
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    /**
     * Invoke the SAF(Storage Access Framework) to import settings
     */
    protected void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Activity should be done within the lifecycle of PreferenceFragment
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            exportText(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importText(data.getData());
        }
    }

    protected void exportText(Uri uri) {
        final Context context = this.getActivity();
        try {
            FileWriter jsonFileWriter =
                    new FileWriter(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "w"))
                                    .getFileDescriptor()
                    );
            PrintWriter printWriter = new PrintWriter(jsonFileWriter);
            if (settingExportInProgress) {
                printWriter.write(Setting.exportToJson(context));
            } else {
                String message = LogBufferManager.exportToString();
                if (message != null) {
                    printWriter.write(message);
                }
            }
            printWriter.close();
            jsonFileWriter.close();

            if (settingExportInProgress) {
                showToastShort(str("revanced_settings_export_success"));
            } else {
                showToastShort(str("revanced_debug_logs_export_success"));
            }
        } catch (IOException e) {
            if (settingExportInProgress) {
                showToastShort(str("revanced_settings_export_failed"));
            } else {
                showToastShort(String.format(str("revanced_debug_logs_failed_to_export"), e.getMessage()));
            }
        } finally {
            settingExportInProgress = false;
        }
    }

    protected void importText(Uri uri) {
        final Context context = this.getActivity();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            settingImportInProgress = true;
            FileReader fileReader =
                    new FileReader(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "r"))
                                    .getFileDescriptor()
                    );
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
            fileReader.close();

            final boolean restartNeeded = Setting.importFromJSON(context, sb.toString());
            if (restartNeeded) {
                showRestartDialog(getActivity());
            }
        } catch (IOException e) {
            showToastShort(str("revanced_settings_import_failed"));
            throw new RuntimeException(e);
        } finally {
            settingImportInProgress = false;
        }
    }
}
