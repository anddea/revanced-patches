package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.PackageUtils.isVersionOrGreater;
import static app.morphe.extension.shared.utils.StringRef.sf;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import app.morphe.extension.shared.settings.preference.CustomDialogListPreference;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ThemeUtils;

/**
 * A custom ListPreference for selecting a spoof app version with checkmarks and EditText for custom.
 */
@SuppressWarnings({"unused", "deprecation"})
public class SpoofAppVersionPreference extends CustomDialogListPreference {

    /**
     * Enum representing supported YouTube Music app with their display names, package names.
     */
    private enum AppVersion {
        YT_20_13_41(),
        YT_20_05_46(),
        YT_19_33_37(),
        YT_19_28_42(),
        YT_19_26_42(),
        YT_19_01_34(),
        OTHER(sf("revanced_spoof_app_version_other_item").toString());

        @NonNull
        public static AppVersion findAppVersion(String versionName) {
            for (AppVersion appVersion : AppVersion.values()) {
                if (appVersion.versionName.equals(versionName)) {
                    return appVersion;
                }
            }
            return AppVersion.OTHER;
        }

        public final String entryName;
        public final String versionName;

        AppVersion() {
            String tmpVersionName = name().replaceAll("YT_", "");
            this.entryName = sf("revanced_spoof_app_version_target_entry_" + tmpVersionName).toString();
            this.versionName = tmpVersionName.replaceAll("_", ".");
        }

        AppVersion(String itemName) {
            this.entryName = itemName;
            this.versionName = itemName;
        }

        public boolean isAvailable() {
            return this == OTHER || isVersionOrGreater(this.versionName);
        }
    }

    private EditText editText;
    private CustomDialogListPreference.ListPreferenceArrayAdapter adapter;
    private Function<String, Void> updateListViewSelection;

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

    private void updateEntries() {
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        for (AppVersion appVersion : AppVersion.values()) {
            if (appVersion.isAvailable()) {
                entries.add(appVersion.entryName);
                entryValues.add(appVersion.versionName);
            }
        }

        setEntries(entries.toArray(new CharSequence[0]));
        setEntryValues(entryValues.toArray(new CharSequence[0]));
    }

    /**
     * Shows a custom dialog with a ListView for predefined third party packages and EditText for custom package input.
     */
    @Override
    protected void showDialog(@Nullable Bundle state) {
        // Must set entries before showing the dialog, to handle if
        // an app is installed while the settings are open in the background.
        updateEntries();

        Context context = getContext();
        String appVersionName = Settings.SPOOF_APP_VERSION_TARGET.get();

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Create ListView for predefined third party apps.
        ListView listView = new ListView(context);
        listView.setId(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Create custom adapter for the ListView.
        final boolean isOther = AppVersion.findAppVersion(appVersionName) == AppVersion.OTHER;
        adapter = new CustomDialogListPreference.ListPreferenceArrayAdapter(
                context,
                LAYOUT_REVANCED_CUSTOM_LIST_ITEM_CHECKED,
                getEntries(),
                getEntryValues(),
                isOther
                        ? AppVersion.OTHER.versionName
                        : appVersionName
        );
        listView.setAdapter(adapter);

        updateListViewSelection = (updatedPackageName) -> {
            CharSequence[] entryValues = getEntryValues();

            for (int i = 0, length = entryValues.length; i < length; i++) {
                String entryString = entryValues[i].toString();
                if (entryString.equals(updatedPackageName)) {
                    listView.setItemChecked(i, true);
                    listView.setSelection(i);
                    adapter.setSelectedValue(entryString);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
            return null;
        };
        updateListViewSelection.apply(appVersionName);

        // Handle item click to select value.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedValue = getEntryValues()[position].toString();
            AppVersion appVersion = AppVersion.findAppVersion(selectedValue);

            if (appVersion != AppVersion.OTHER) {
                editText.setText(appVersion.versionName);
                editText.setEnabled(false); // Disable editing for predefined options.
            } else {
                String savedPackageName = Settings.SPOOF_APP_VERSION_TARGET.get();
                editText.setText(AppVersion.findAppVersion(savedPackageName) == AppVersion.OTHER
                        ? savedPackageName // If the user is clicking thru options then retain existing other app.
                        : ""
                );
                editText.setEnabled(true); // Enable editing for Custom.
                editText.requestFocus();
            }
            editText.setSelection(editText.getText().length());
            adapter.setSelectedValue(selectedValue);
            adapter.notifyDataSetChanged();
        });

        // Add ListView to content layout with initial height.
        LinearLayout.LayoutParams listViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0 // Initial height, will be updated.
        );
        listViewParams.bottomMargin = dipToPixels(16);
        contentLayout.addView(listView, listViewParams);

        // Add EditText for custom package name.
        editText = new EditText(context);
        editText.setText(appVersionName);
        editText.setSelection(appVersionName.length());
        editText.setHint(str("revanced_spoof_app_version_other_item_hint"));
        editText.setSingleLine(true); // Restrict EditText to a single line.
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editText.setEnabled(isOther);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable edit) {
                String updatedPackageName = edit.toString().trim();
                updateListViewSelection.apply(updatedPackageName);
            }
        });

        ShapeDrawable editTextBackground = new ShapeDrawable(new RoundRectShape(
                Utils.createCornerRadii(10), null, null));
        editTextBackground.getPaint().setColor(ThemeUtils.getEditTextBackground());
        final int dip8 = dipToPixels(8);
        editText.setPadding(dip8, dip8, dip8, dip8);
        editText.setBackground(editTextBackground);
        editText.setClipToOutline(true);
        contentLayout.addView(editText);

        // Create the custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                getTitle() != null ? getTitle().toString() : "",
                null,
                null,
                null,
                () -> {
                    String newValue = editText.getText().toString().trim();

                    if (showToastIfAppVersionIsInvalid(newValue)) {
                        return; // Invalid package. Do not save.
                    }

                    // Save custom package name.
                    if (callChangeListener(newValue)) {
                        setValue(newValue);
                    }
                },
                () -> {
                }, // Cancel button action (dismiss only).
                str("revanced_settings_reset"),
                () -> { // Reset action.
                    String defaultValue = Settings.SPOOF_APP_VERSION_TARGET.defaultValue;
                    editText.setText(defaultValue);
                    editText.setSelection(defaultValue.length());
                    editText.setEnabled(false); // Disable editing on reset.
                    updateListViewSelection.apply(defaultValue);
                },
                false
        );

        // Add the content layout directly to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentLayout, dialogMainLayout.getChildCount() - 1);

        // Update ListView height dynamically based on orientation.
        //noinspection ExtractMethodRecommender
        Runnable updateListViewHeight = () -> {
            int totalHeight = 0;
            ListAdapter listAdapter = listView.getAdapter();
            if (listAdapter != null) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                final int listAdapterCount = listAdapter.getCount();
                for (int i = 0; i < listAdapterCount; i++) {
                    View item = listAdapter.getView(i, null, listView);
                    item.measure(
                            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.AT_MOST),
                            View.MeasureSpec.UNSPECIFIED
                    );
                    totalHeight += item.getMeasuredHeight();
                }
                totalHeight += listView.getDividerHeight() * (listAdapterCount - 1);
            }

            final int orientation = context.getResources().getConfiguration().orientation;
            if (orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                // In portrait orientation, use WRAP_CONTENT for ListView height.
                listViewParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            } else {
                // In landscape orientation, limit ListView height to 30% of screen height.
                final int maxHeight = Utils.percentageHeightToPixels(30);
                listViewParams.height = Math.min(totalHeight, maxHeight);
            }
            listView.setLayoutParams(listViewParams);
        };

        // Initial height calculation.
        updateListViewHeight.run();

        // Listen for configuration changes (e.g., orientation).
        View dialogView = dialogPair.second;
        // Recalculate height when layout changes (e.g., orientation change).
        dialogView.getViewTreeObserver().addOnGlobalLayoutListener(updateListViewHeight::run);

        // Show the dialog.
        dialogPair.first.show();
    }

    /**
     * @return If the app is not installed and a dialog was shown.
     */
    private boolean showToastIfAppVersionIsInvalid(String appVersion) {
        if (appVersion.compareTo(Settings.SPOOF_APP_VERSION_TARGET.defaultValue) < 0) {
            Utils.showToastShort(str("revanced_spoof_app_version_target_invalid_toast", appVersion));
            Utils.showToastShort(str("revanced_reset_to_default_toast"));
            Settings.SPOOF_APP_VERSION_TARGET.resetToDefault();

            String defaultValue = Settings.SPOOF_APP_VERSION_TARGET.defaultValue;
            editText.setText(defaultValue);
            editText.setSelection(defaultValue.length());
            editText.setEnabled(false); // Disable editing on reset.
            updateListViewSelection.apply(defaultValue);

            return true;
        }
        return false;
    }
}
