/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Modification author(s):
 * - anddea (https://github.com/anddea)
 *
 * Originally ported from:
 * https://github.com/ReVanced/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.shared.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.ResourceUtils;

/**
 * A custom ListPreference that uses a styled custom dialog with a custom checkmark indicator,
 * supports a static summary and highlighted entries for search functionality.
 */
@SuppressWarnings({"unused", "deprecation"})
public class CustomDialogListPreference extends ListPreference {

    public static final int ID_REVANCED_CHECK_ICON =
            ResourceUtils.getIdIdentifier("revanced_check_icon");
    public static final int ID_REVANCED_CHECK_ICON_PLACEHOLDER =
            ResourceUtils.getIdIdentifier("revanced_check_icon_placeholder");
    public static final int ID_REVANCED_ITEM_TEXT =
            ResourceUtils.getIdIdentifier("revanced_item_text");
    public static final int LAYOUT_REVANCED_CUSTOM_LIST_ITEM_CHECKED =
            ResourceUtils.getLayoutIdentifier("revanced_custom_list_item_checked");

    private String staticSummary = null;
    private CharSequence[] highlightedEntriesForDialog = null;

    /**
     * Set a static summary that will not be overwritten by value changes.
     */
    public void setStaticSummary(String summary) {
        this.staticSummary = summary;
    }

    /**
     * Returns the static summary if set, otherwise null.
     */
    @Nullable
    public String getStaticSummary() {
        return staticSummary;
    }

    /**
     * Always return static summary if set.
     */
    @Override
    public CharSequence getSummary() {
        if (staticSummary != null) {
            return staticSummary;
        }
        return super.getSummary();
    }

    /**
     * Sets highlighted entries for display in the dialog.
     * These entries are used only for the current dialog and are automatically cleared.
     */
    public void setHighlightedEntriesForDialog(CharSequence[] highlightedEntries) {
        this.highlightedEntriesForDialog = highlightedEntries;
    }

    /**
     * Clears highlighted entries after the dialog is closed.
     */
    public void clearHighlightedEntriesForDialog() {
        this.highlightedEntriesForDialog = null;
    }

    /**
     * Returns the entries rendered in the current dialog. Subclasses may reuse this when they
     * add custom row content such as an icon preview.
     */
    protected CharSequence[] getEntriesForDialog() {
        return highlightedEntriesForDialog != null ? highlightedEntriesForDialog : getEntries();
    }

    /**
     * Custom ArrayAdapter to handle checkmark visibility.
     */
    public static class ListPreferenceArrayAdapter extends ArrayAdapter<CharSequence> {
        private static class SubViewDataContainer {
            ImageView checkIcon;
            View placeholder;
            TextView itemText;
        }

        final int layoutResourceId;
        final CharSequence[] entryValues;
        String selectedValue;

        public ListPreferenceArrayAdapter(Context context, int resource,
                                          CharSequence[] entries,
                                          CharSequence[] entryValues,
                                          String selectedValue) {
            super(context, resource, entries);
            this.layoutResourceId = resource;
            this.entryValues = entryValues;
            this.selectedValue = selectedValue;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            SubViewDataContainer holder;

            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(layoutResourceId, parent, false);
                holder = new SubViewDataContainer();
                holder.checkIcon = view.findViewById(ID_REVANCED_CHECK_ICON);
                holder.placeholder = view.findViewById(ID_REVANCED_CHECK_ICON_PLACEHOLDER);
                holder.itemText = view.findViewById(ID_REVANCED_ITEM_TEXT);
                view.setTag(holder);
            } else {
                holder = (SubViewDataContainer) view.getTag();
            }

            CharSequence itemText = getItem(position);
            holder.itemText.setText(itemText);
            holder.itemText.setTextColor(BaseThemeUtils.getAppForegroundColor());

            // Show or hide checkmark and placeholder.
            String currentValue = entryValues[position].toString();
            boolean isSelected = currentValue.equals(selectedValue);
            holder.checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.checkIcon.setColorFilter(BaseThemeUtils.getAppForegroundColor());
            holder.placeholder.setVisibility(isSelected ? View.GONE : View.VISIBLE);

            return view;
        }

        public void setSelectedValue(String value) {
            this.selectedValue = value;
        }
    }

    public CustomDialogListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomDialogListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomDialogListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDialogListPreference(Context context) {
        super(context);
    }

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();

        CharSequence[] allEntries = getEntriesForDialog();
        CharSequence[] allEntryValues = getEntryValues();
        boolean hideCustomTheme = shouldHideCustomThemeEntry(allEntryValues);
        final CharSequence[] entriesToShow = hideCustomTheme
                ? Arrays.copyOf(allEntries, allEntries.length - 1)
                : allEntries;
        final CharSequence[] entryValues = hideCustomTheme
                ? Arrays.copyOf(allEntryValues, allEntryValues.length - 1)
                : allEntryValues;

        // Create ListView.
        ListView listView = new ListView(context);
        listView.setId(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        // Create custom adapter for the ListView.
        ListPreferenceArrayAdapter adapter = new ListPreferenceArrayAdapter(
                context,
                LAYOUT_REVANCED_CUSTOM_LIST_ITEM_CHECKED,
                entriesToShow,
                entryValues,
                getValue()
        );
        listView.setAdapter(adapter);

        // Set checked item.
        String currentValue = getValue();
        if (currentValue != null) {
            for (int i = 0, length = entryValues.length; i < length; i++) {
                if (currentValue.equals(entryValues[i].toString())) {
                    listView.setItemChecked(i, true);
                    listView.setSelection(i);
                    break;
                }
            }
        }

        // Create the custom dialog without OK button.
        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                getTitle() != null ? getTitle().toString() : "",
                null,
                null,
                null,
                null,
                this::clearHighlightedEntriesForDialog, // Cancel button action.
                null,
                null,
                true
        );

        Dialog dialog = dialogPair.first;
        // Add a listener to clear when the dialog is closed in any way.
        dialog.setOnDismissListener(dialogInterface -> clearHighlightedEntriesForDialog());

        // Add the ListView to the main layout.
        LinearLayout mainLayout = dialogPair.second;
        LinearLayout.LayoutParams listViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        mainLayout.addView(listView, mainLayout.getChildCount() - 1, listViewParams);

        // Handle item click to select value and dismiss dialog.
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedValue = entryValues[position].toString();
            if (callChangeListener(selectedValue)) {
                setValue(selectedValue);

                // Update summaries from the original entries (without highlighting).
                if (staticSummary == null) {
                    CharSequence[] originalEntries = getEntries();
                    if (originalEntries != null && position < originalEntries.length) {
                        setSummary(originalEntries[position]);
                    }
                }

                adapter.setSelectedValue(selectedValue);
                adapter.notifyDataSetChanged();
            }

            // Clear highlighted entries before closing.
            clearHighlightedEntriesForDialog();
            dialog.dismiss();
        });

        // Show the dialog.
        dialog.show();
    }

    /**
     * Android 8–10 support only the precompiled theme presets. Keep the trailing Custom row out
     * of both theme selectors because arbitrary runtime colors require ResourcesLoader (API 30).
     */
    private boolean shouldHideCustomThemeEntry(CharSequence[] entryValues) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || entryValues.length == 0) {
            return false;
        }

        String key = getKey();
        boolean isThemeSelector = "morphe_dark_theme".equals(key)
                || "morphe_light_theme".equals(key);
        return isThemeSelector
                && "custom".contentEquals(entryValues[entryValues.length - 1]);
    }
}
