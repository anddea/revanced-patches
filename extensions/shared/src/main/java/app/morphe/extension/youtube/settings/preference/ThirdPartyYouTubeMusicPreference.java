package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.patches.PatchStatus.PackageNameYouTubeMusic;
import static app.morphe.extension.shared.utils.StringRef.sf;
import static app.morphe.extension.shared.utils.StringRef.str;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
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

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import app.morphe.extension.shared.settings.StringSetting;
import app.morphe.extension.shared.settings.preference.CustomDialogListPreference;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ThemeUtils;

/**
 * A custom ListPreference for selecting a third party package with checkmarks and EditText for custom package names.
 */
@SuppressWarnings({"unused", "deprecation"})
public class ThirdPartyYouTubeMusicPreference extends CustomDialogListPreference {
    private static final String YOUTUBE_MUSIC_PACKAGE_NAME =
            "com.google.android.apps.youtube.music";
    private static final StringSetting settings =
            Settings.THIRD_PARTY_YOUTUBE_MUSIC_PACKAGE_NAME;

    /**
     * Enum representing supported YouTube Music app with their display names, package names.
     */
    private enum YouTubeMusic {
        RVX_MUSIC("RVX Music",
                PackageNameYouTubeMusic()
        ),
        YOUTUBE_MUSIC("YouTube Music",
                YOUTUBE_MUSIC_PACKAGE_NAME,
                true
        ),
        OTHER(sf("revanced_external_downloader_other_item").toString(),
                null,
                true
        );

        private static final Map<String, YouTubeMusic> PACKAGE_TO_ENUM = new HashMap<>();

        static {
            for (YouTubeMusic music : values()) {
                String packageName = music.packageName;
                if (packageName != null) {
                    if (music == RVX_MUSIC && packageName.equals(YOUTUBE_MUSIC_PACKAGE_NAME)) {
                        continue;
                    }
                    PACKAGE_TO_ENUM.put(packageName, music);
                }
            }
        }

        /**
         * Finds a YouTubeMusic by its package name. This method can never return {@link #OTHER}.
         *
         * @return The YouTubeMusic enum or null if not found.
         */
        @Nullable
        public static YouTubeMusic findByPackageName(String packageName) {
            return PACKAGE_TO_ENUM.get(Objects.requireNonNull(packageName));
        }

        public final String name;
        @Nullable
        public final String packageName;
        /**
         * If a YouTube Music app should be shown in the preference settings
         * if the app is not currently installed.
         */
        public final boolean isPreferred;

        YouTubeMusic(String name, String packageName) {
            this(name, packageName, false);
        }

        YouTubeMusic(String name, @Nullable String packageName, boolean isPreferred) {
            this.name = name;
            this.packageName = packageName;
            this.isPreferred = isPreferred;
        }

        public boolean isInstalled() {
            return packageName != null && isAppInstalledAndEnabled(packageName);
        }
    }

    private static boolean isAppInstalledAndEnabled(String packageName) {
        try {
            if (Utils.getContext().getPackageManager().getApplicationInfo(packageName, 0).enabled) {
                Logger.printDebug(() -> "App installed: " + packageName);
                return true;
            }
        } catch (PackageManager.NameNotFoundException error) {
            Logger.printDebug(() -> "App not installed: " + packageName);
        }
        return false;
    }

    private EditText editText;
    private CustomDialogListPreference.ListPreferenceArrayAdapter adapter;

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ThirdPartyYouTubeMusicPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThirdPartyYouTubeMusicPreference(Context context) {
        super(context);
    }

    private void updateEntries() {
        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> entryValues = new ArrayList<>();

        for (YouTubeMusic music : YouTubeMusic.values()) {
            if (music.isPreferred || music.isInstalled()) {
                String packageName = music.packageName;

                entries.add(music.name);
                entryValues.add(packageName != null
                        ? packageName
                        : YouTubeMusic.OTHER.name);
            }
        }

        setEntries(entries.toArray(new CharSequence[0]));
        setEntryValues(entryValues.toArray(new CharSequence[0]));
    }

    /**
     * Sets the summary for this ListPreference.
     */
    @Override
    public void setSummary(CharSequence summary) {
        // Ignore calls to set the summary.
        // Summary is always the description of the category.
        //
        // This is required otherwise the ReVanced preference fragment
        // sets all ListPreference summaries to show the current selection.
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
        String packageName = settings.get();

        // Create the main layout for the dialog content.
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Create ListView for predefined third party apps.
        ListView listView = new ListView(context);
        listView.setId(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Create custom adapter for the ListView.
        final boolean usingThirdPartyApp = YouTubeMusic.findByPackageName(packageName) == null;
        adapter = new CustomDialogListPreference.ListPreferenceArrayAdapter(
                context,
                LAYOUT_REVANCED_CUSTOM_LIST_ITEM_CHECKED,
                getEntries(),
                getEntryValues(),
                usingThirdPartyApp
                        ? YouTubeMusic.OTHER.name
                        : packageName
        );
        listView.setAdapter(adapter);

        Function<String, Void> updateListViewSelection = (updatedPackageName) -> {
            String entryValueName = YouTubeMusic.findByPackageName(updatedPackageName) == null
                    ? YouTubeMusic.OTHER.name
                    : updatedPackageName;
            CharSequence[] entryValues = getEntryValues();

            for (int i = 0, length = entryValues.length; i < length; i++) {
                String entryString = entryValues[i].toString();
                if (entryString.equals(entryValueName)) {
                    listView.setItemChecked(i, true);
                    listView.setSelection(i);
                    adapter.setSelectedValue(entryString);
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
            return null;
        };
        updateListViewSelection.apply(packageName);

        // Handle item click to select value.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedValue = getEntryValues()[position].toString();
            YouTubeMusic selectedApp = YouTubeMusic.findByPackageName(selectedValue);

            if (selectedApp != null) {
                editText.setText(selectedApp.packageName);
                editText.setEnabled(false); // Disable editing for predefined options.
            } else {
                String savedPackageName = settings.get();
                editText.setText(YouTubeMusic.findByPackageName(savedPackageName) == null
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
        editText.setText(packageName);
        editText.setSelection(packageName.length());
        editText.setHint(str("revanced_external_downloader_other_item_hint"));
        editText.setSingleLine(true); // Restrict EditText to a single line.
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        editText.setEnabled(usingThirdPartyApp);
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
                    if (newValue.isEmpty()) {
                        // Show dialog if EditText is empty.
                        CustomDialog.create(
                                context,
                                str("revanced_third_party_youtube_music_dialog_title"),
                                str("revanced_external_downloader_empty_warning"),
                                null,
                                null,
                                () -> {
                                }, // OK button does nothing (dismiss only).
                                null,
                                null,
                                null,
                                false
                        ).first.show();
                        return;
                    }

                    if (showDialogIfAppIsNotInstalled(getContext(), newValue)) {
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
                    String defaultValue = settings.defaultValue;
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
    public static boolean showDialogIfAppIsNotInstalled(Context context, String packageName) {
        if (isAppInstalledAndEnabled(packageName)) {
            return false;
        }

        YouTubeMusic music = YouTubeMusic.findByPackageName(packageName);
        // Show a dialog if the recommended app is not installed or if the custom package cannot be found.
        String message = music != null
                ? str("revanced_external_downloader_not_installed_warning", music.name)
                : str("revanced_external_downloader_package_not_found_warning", packageName);

        CustomDialog.create(
                context,
                str("revanced_external_downloader_not_found_title"),
                message,
                null,
                null,
                () -> {
                },
                null,
                null,
                null,
                false
        ).first.show();

        return true;
    }
}
