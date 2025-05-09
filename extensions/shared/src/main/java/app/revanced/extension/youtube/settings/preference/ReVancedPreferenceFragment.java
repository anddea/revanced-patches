package app.revanced.extension.youtube.settings.preference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toolbar;
import androidx.annotation.NonNull;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.StringRef;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.settings.ClickablePreferenceCategory;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;
import com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import static app.revanced.extension.shared.settings.BaseSettings.SPOOF_STREAMING_DATA_TYPE;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.*;
import static app.revanced.extension.youtube.settings.Settings.*;
import static com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity.*;

@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends PreferenceFragment {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;

    /**
     * XML tag name for PreferenceScreen.
     * Used for parsing the preferences XML file.
     * Example: <pre>{@code <PreferenceScreen ...>}</pre>
     */
    private static final String PREFERENCE_SCREEN_TAG = "PreferenceScreen";

    /**
     * XML attribute name for the preference key.
     * Used to identify each preference uniquely.
     * Example: <pre>{@code <Preference android:key="preference_key" ...>>}</pre>
     */
    private static final String KEY_ATTRIBUTE = "key";

    /**
     * Custom XML attribute name for specifying a preference's dependency.
     * This attribute defines a custom dependency relationship between preferences,
     * where the value is the key of the preference this one depends on.
     * Example: <pre>{@code <Preference ... app:searchDependency="parent_preference_key" ...>}</pre>
     */
    private static final String DEPENDENCY_ATTRIBUTE = "searchDependency";

    /**
     * XML attribute name for the preference title.
     * Used to display the human-readable name of the preference.
     * Example: <pre>{@code <Preference ... android:title="@string/preference_title" ...>}</pre>
     */
    private static final String TITLE_ATTRIBUTE = "title";

    static boolean settingImportInProgress = false;
    static boolean showingUserDialogMessage;
    static PreferenceManager mPreferenceManager;

    // region Search-related fields

    /**
     * Stores dependencies between preferences.
     * <p>
     * <b>Key:</b> <i>The key of a preference that other preferences depend on.</i><br>
     * <b>Value:</b> <i>A list of preferences that depend on the key preference.</i>
     * <p>
     * Example: For preferences <pre>{@code
     * <Preference1 android:key="preference_key_1" ...>
     * <Preference2 ... android:dependency="preference_key_1" ...>
     * <Preference3 ... android:dependency="preference_key_1" ...>
     * ...
     * }</pre>
     * <p>
     * dependencyMap will be <pre>{@code
     * dependencyMap = {preference_key_1: [Preference2, Preference3], ...}
     * }</pre>
     */
    private final Map<String, List<Preference>> dependencyMap = new HashMap<>();

    /**
     * Stores preferences grouped by their parent PreferenceGroup (or PreferenceScreen).
     * <p>
     * <b>Key:</b> <i>The parent PreferenceGroup. The root PreferenceScreen is represented by a special top-level group.</i><br>
     * <b>Value:</b> <i>A list of preferences belonging to that group.</i>
     * <p>
     * Example: For preferences <pre>{@code
     * <PreferenceScreen ... android:title="@string/preference_screen_top_level_title" ...>
     *     <PreferenceScreen ... android:title="@string/preference_screen_1_title" ...>
     *         <Preference ... android:title="@string/preference_screen_1_preference_title_1" ...>
     *         <Preference ... android:title="@string/preference_screen_1_preference_title_2" ...>
     *         ...
     *     </PreferenceScreen>
     *     <PreferenceScreen ... android:title="@string/preference_screen_2_title" ...>
     *         <Preference ... android:title="@string/preference_screen_2_preference_title_1" ...>
     *         <Preference ... android:title="@string/preference_screen_2_preference_title_2" ...>
     *         ...
     *     </PreferenceScreen>
     *     ...
     * </PreferenceScreen>
     * }</pre>
     * <p>
     * groupedPreferences will be <pre>{@code
     * groupedPreferences = {preference_screen_top_level_title=[preference_screen_1_title, preference_screen_2_title, ...], preference_screen_1_title=[preference_screen_1_preference_title_1, preference_screen_1_preference_title_2, ...], ...}
     * }</pre>
     */
    private final Map<PreferenceGroup, LinkedHashSet<Preference>> groupedPreferences = new LinkedHashMap<>();

    /**
     * Stores custom dependencies defined in the preferences XML file with {@link #DEPENDENCY_ATTRIBUTE}.
     * <p>
     * <b>Key:</b> <i>A string representing the category and key of a preference in the format "Category:Key".
     * The category is derived from the title of the parent PreferenceScreen.</i><br>
     * <b>Value:</b> <i>The key of the preference on which the key preference depends.</i>
     * <p>
     * Example: For preferences <pre>{@code
     * <PreferenceScreen ... android:title="@string/preference_screen_title" ...>
     *     <Preference android:key="preference_key_1" ...>
     *     <Preference android:key="preference_key_2" ... app:searchDependency="preference_key_1">
     *     <Preference android:key="preference_key_3" ... app:searchDependency="preference_key_1">
     *     ...
     * </PreferenceScreen>
     * ...
     * }</pre>
     * <p>
     * customDependencyMap will be <pre>{@code
     * customDependencyMap = {preference_screen_title:preference_key_2 = preference_key_1, preference_screen_title:preference_key_3 = preference_key_1, ...}
     * }</pre>
     */
    private final Map<String, String> customDependencyMap = new HashMap<>();

    // endregion Search-related fields

    @SuppressLint("SuspiciousIndentation")
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            if (str == null) {
                return;
            }

            Setting<?> setting = Setting.getSettingFromPath(str);
            if (setting == null) {
                return;
            }

            Preference mPreference = findPreference(str);
            if (mPreference == null) {
                return;
            }

            if (mPreference instanceof SwitchPreference switchPreference) {
                BooleanSetting boolSetting = (BooleanSetting) setting;
                Logger.printDebug(() -> "SwitchPreference: " + str + ", checked: " + switchPreference.isChecked());
                if (settingImportInProgress) {
                    switchPreference.setChecked(boolSetting.get());
                } else {
                    BooleanSetting.privateSetValue(boolSetting, switchPreference.isChecked());
                }

                if (ExtendedUtils.anyMatchSetting(setting)) {
                    ExtendedUtils.setPlayerFlyoutMenuAdditionalSettings();
                } else if (setting.equals(HIDE_PREVIEW_COMMENT) || setting.equals(HIDE_PREVIEW_COMMENT_TYPE)) {
                    ExtendedUtils.setCommentPreviewSettings();
                }
            } else if (mPreference instanceof EditTextPreference editTextPreference) {
                if (settingImportInProgress) {
                    editTextPreference.setText(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, editTextPreference.getText());
                }
            } else if (mPreference instanceof ListPreference listPreference) {
                if (settingImportInProgress) {
                    listPreference.setValue(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, listPreference.getValue());
                }
                if (setting.equals(DEFAULT_PLAYBACK_SPEED) || setting.equals(DEFAULT_PLAYBACK_SPEED_SHORTS)) {
                    listPreference.setEntries(CustomPlaybackSpeedPatch.getEntries());
                    listPreference.setEntryValues(CustomPlaybackSpeedPatch.getEntryValues());
                }
                if (setting.equals(SPOOF_STREAMING_DATA_TYPE)) {
                    listPreference.setEntries(SpoofStreamingDataPatch.getEntries());
                    listPreference.setEntryValues(SpoofStreamingDataPatch.getEntryValues());
                }
                if (!(mPreference instanceof app.revanced.extension.youtube.settings.preference.SegmentCategoryListPreference)) {
                    updateListPreferenceSummary(listPreference, setting);
                }
            } else {
                Logger.printException(() -> "Setting cannot be handled: " + mPreference.getClass() + " " + mPreference);
                return;
            }

            ReVancedSettingsPreference.initializeReVancedSettings();

            if (!settingImportInProgress && !showingUserDialogMessage) {
                final Context context = getActivity();

                if (setting.userDialogMessage != null && !prefIsSetToDefault(mPreference, setting)) {
                    showSettingUserDialogConfirmation(context, mPreference, setting);
                } else if (setting.rebootApp) {
                    showRestartDialog(context);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };

    private final List<PreferenceInfo> preferenceList = new ArrayList<>();
    private final Map<String, List<Integer>> dependencyCache = new HashMap<>();
    public Stack<PreferenceScreen> preferenceScreenStack = new Stack<>();
    public PreferenceScreen rootPreferenceScreen;
    private PreferenceScreen originalPreferenceScreen;
    private SharedPreferences mSharedPreferences;

    public ReVancedPreferenceFragment() {
        // Required empty public constructor
    }

    // region Preference Management

    // region [Preference Management] Preference Logic

    /**
     * Sorts the entries of a {@link ListPreference} alphabetically by their display text, while preserving
     * the specified number of initial entries in their original order.
     *
     * @param listPreference The {@link ListPreference} to sort.
     * @throws IllegalStateException If the number of entries and entry values do not match.
     */
    private static void sortListPreferenceByValues(ListPreference listPreference) {
        CharSequence[] entries = listPreference.getEntries();
        CharSequence[] entryValues = listPreference.getEntryValues();
        final int entrySize = entries.length;

        if (entrySize != entryValues.length) {
            // Xml array declaration has a missing/extra entry.
            throw new IllegalStateException();
        }

        // Since the text of Preference is Spanned, CharSequence#toString() should not be used.
        // If CharSequence#toString() is used, Spanned styling, such as HTML syntax, will be broken.
        List<Pair<CharSequence, CharSequence>> firstPairs = new ArrayList<>(1);
        List<Pair<CharSequence, CharSequence>> pairsToSort = new ArrayList<>(entrySize);

        for (int i = 0; i < entrySize; i++) {
            Pair<CharSequence, CharSequence> pair = new Pair<>(entries[i], entryValues[i]);
            if (i < 1) {
                firstPairs.add(pair);
            } else {
                pairsToSort.add(pair);
            }
        }

        pairsToSort.sort((pair1, pair2)
                -> pair1.first.toString().compareToIgnoreCase(pair2.first.toString()));

        CharSequence[] sortedEntries = new CharSequence[entrySize];
        CharSequence[] sortedEntryValues = new CharSequence[entrySize];

        int i = 0;
        for (Pair<CharSequence, CharSequence> pair : firstPairs) {
            sortedEntries[i] = pair.first;
            sortedEntryValues[i] = pair.second;
            i++;
        }

        for (Pair<CharSequence, CharSequence> pair : pairsToSort) {
            sortedEntries[i] = pair.first;
            sortedEntryValues[i] = pair.second;
            i++;
        }

        listPreference.setEntries(sortedEntries);
        listPreference.setEntryValues(sortedEntryValues);
    }

    /**
     * Constructs a string representing the full hierarchical path of a PreferenceGroup,
     * from the root to the current group, using titles separated by " › ".
     * Includes both PreferenceScreen and PreferenceCategory titles for display purposes.
     *
     * @param group The PreferenceGroup for which to build the path.
     * @return A string representing the full path of titles, or an empty string if no titles are found.
     */
    private String getFullPath(PreferenceGroup group) {
        Deque<String> titles = new ArrayDeque<>();
        for (PreferenceGroup current = group;
             current != null;
             current = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? current.getParent() : null
        ) {
            CharSequence title = current.getTitle();
            if (title != null) {
                String t = title.toString().trim();
                if (!t.isEmpty()) titles.addFirst(t);
            }
        }
        return String.join(" > ", titles);
    }

    /**
     * Finds the closest parent PreferenceScreen for a given PreferenceGroup.
     * If the group is a PreferenceScreen, it returns that group. Otherwise, it traverses up the hierarchy.
     *
     * @param group The PreferenceGroup to start from.
     * @return The closest PreferenceScreen, or null if none is found.
     */
    private PreferenceScreen findClosestPreferenceScreen(PreferenceGroup group) {
        PreferenceGroup current = group;
        while (current != null) {
            if (current instanceof PreferenceScreen) {
                return (PreferenceScreen) current;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                current = current.getParent();
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Checks if the preference is currently set to the default value of the associated Setting.
     *
     * @param pref   The {@link Preference} to check.
     * @param setting The {@link Setting} associated with the preference.
     * @return True if the preference is set to the default value, false otherwise.
     * @throws IllegalStateException If the preference type is not supported.
     */
    private boolean prefIsSetToDefault(Preference pref, Setting<?> setting) {
        Object defaultValue = setting.defaultValue;
        if (pref instanceof SwitchPreference switchPref) {
            return switchPref.isChecked() == (Boolean) defaultValue;
        }
        String defaultValueString = defaultValue.toString();
        if (pref instanceof EditTextPreference editPreference) {
            return editPreference.getText().equals(defaultValueString);
        }
        if (pref instanceof ListPreference listPref) {
            return listPref.getValue().equals(defaultValueString);
        }

        throw new IllegalStateException("Must override method to handle "
                + "preference type: " + pref.getClass());
    }

    /**
     * Displays a confirmation dialog for a setting change when the setting has a user dialog message
     * and is not set to its default value. Allows the user to confirm or revert the change.
     *
     * @param context The context used to create the dialog.
     * @param pref    The {@link Preference} associated with the setting.
     * @param setting The {@link Setting} that was changed.
     */
    private void showSettingUserDialogConfirmation(Context context, Preference pref, Setting<?> setting) {
        Utils.verifyOnMainThread();

        final StringRef userDialogMessage = setting.userDialogMessage;
        if (context != null && userDialogMessage != null) {
            showingUserDialogMessage = true;

            new AlertDialog.Builder(context)
                    .setTitle(str("revanced_extended_confirm_user_dialog_title"))
                    .setMessage(userDialogMessage.toString())
                    .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                        if (setting.rebootApp) {
                            showRestartDialog(context);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                        // Restore whatever the setting was before the change.
                        if (setting instanceof BooleanSetting booleanSetting &&
                                pref instanceof SwitchPreference switchPreference) {
                            switchPreference.setChecked(booleanSetting.defaultValue);
                        } else if (setting instanceof EnumSetting<?> enumSetting &&
                                pref instanceof ListPreference listPreference) {
                            listPreference.setValue(enumSetting.defaultValue.toString());
                            updateListPreferenceSummary(listPreference, setting);
                        }
                    })
                    .setOnDismissListener(dialog -> showingUserDialogMessage = false)
                    .setCancelable(false)
                    .show();
        }
    }

    /**
     * Sorts the entries of a ListPreference associated with an EnumSetting alphabetically.
     *
     * @param setting The {@link EnumSetting} whose associated ListPreference should be sorted.
     */
    private void sortPreferenceListMenu(EnumSetting<?> setting) {
        Preference preference = findPreference(setting.key);
        if (preference instanceof ListPreference languagePreference) {
            sortListPreferenceByValues(languagePreference);
        }
    }

    /**
     * Copies all preferences from a source PreferenceScreen to a destination PreferenceScreen.
     *
     * @param source      The source {@link PreferenceScreen} containing the preferences to copy.
     * @param destination The destination {@link PreferenceScreen} to receive the copied preferences.
     */
    private void copyPreferences(PreferenceScreen source, PreferenceScreen destination) {
        for (Preference preference : getAllPreferencesBy(source)) {
            destination.addPreference(preference);
        }
    }

    /**
     * Recursively stores all preferences and their dependencies grouped by their parent PreferenceGroup.
     *
     * @param preferenceGroup The PreferenceGroup to retrieve preferences from.
     * @return A list of preferences.
     */
    private List<Preference> getAllPreferencesBy(PreferenceGroup preferenceGroup) {
        List<Preference> preferences = new ArrayList<>();
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            preferences.add(preferenceGroup.getPreference(i));
        }
        return preferences;
    }

    // endregion [Preference Management] Preference Logic

    // region [Preference Management] UI Setup

    /**
     * Configures toolbars for all {@link PreferenceScreen} instances in the preference hierarchy,
     * setting up navigation, titles, and edge-to-edge display support for API 35+.
     */
    private void setPreferenceScreenToolbar() {
        SortedMap<String, PreferenceScreen> preferenceScreenMap = new TreeMap<>();

        PreferenceScreen rootPreferenceScreen = getPreferenceScreen();
        for (Preference preference : getAllPreferencesBy(rootPreferenceScreen)) {
            if (!(preference instanceof PreferenceGroup preferenceGroup)) continue;
            putPreferenceScreenMap(preferenceScreenMap, preferenceGroup);
            for (Preference childPreference : getAllPreferencesBy(preferenceGroup)) {
                if (!(childPreference instanceof PreferenceGroup nestedPreferenceGroup)) continue;
                putPreferenceScreenMap(preferenceScreenMap, nestedPreferenceGroup);
                for (Preference nestedPreference : getAllPreferencesBy(nestedPreferenceGroup)) {
                    if (!(nestedPreference instanceof PreferenceGroup childPreferenceGroup)) continue;
                    putPreferenceScreenMap(preferenceScreenMap, childPreferenceGroup);
                }
            }
        }

        Integer targetSDKVersion = ExtendedUtils.getTargetSDKVersion(getContext().getPackageName());
        boolean isEdgeToEdgeSupported = isSDKAbove(35) && targetSDKVersion != null && targetSDKVersion >= 35;

        for (PreferenceScreen mPreferenceScreen : preferenceScreenMap.values()) {
            mPreferenceScreen.setOnPreferenceClickListener(
                    preferenceScreen -> {
                        Dialog preferenceScreenDialog = mPreferenceScreen.getDialog();
                        ViewGroup rootView = (ViewGroup) preferenceScreenDialog
                                .findViewById(android.R.id.content)
                                .getParent();

                        if (isEdgeToEdgeSupported) {
                            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                                Insets statusInsets = insets.getInsets(WindowInsets.Type.statusBars());
                                Insets navInsets = insets.getInsets(WindowInsets.Type.navigationBars());
                                v.setPadding(0, statusInsets.top, 0, navInsets.bottom);
                                return insets;
                            });
                        }

                        Toolbar toolbar = getToolbar(preferenceScreen, preferenceScreenDialog);

                        int margin = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()
                        );
                        toolbar.setTitleMargin(margin, 0, margin, 0);

                        TextView toolbarTextView = getChildView(toolbar, TextView.class::isInstance);
                        if (toolbarTextView != null) {
                            toolbarTextView.setTextColor(ThemeUtils.getForegroundColor());
                        }

                        setToolbarLayoutParams(toolbar);
                        rootView.addView(toolbar, 0);

                        return false;
                    }
            );
        }
    }

    /**
     * Adds a PreferenceGroup to a sorted map if it is a PreferenceScreen.
     *
     * @param preferenceScreenMap The sorted map to store PreferenceScreen instances.
     * @param preferenceGroup    The PreferenceGroup to check and potentially add.
     */
    private void putPreferenceScreenMap(SortedMap<String, PreferenceScreen> preferenceScreenMap, PreferenceGroup preferenceGroup) {
        if (preferenceGroup instanceof PreferenceScreen mPreferenceScreen) {
            preferenceScreenMap.put(mPreferenceScreen.getKey(), mPreferenceScreen);
        }
    }

    /**
     * Creates and configures a Toolbar for a PreferenceScreen dialog.
     *
     * @param preferenceScreen      The {@link PreferenceScreen} associated with the toolbar.
     * @param preferenceScreenDialog The dialog containing the PreferenceScreen.
     * @return A configured {@link Toolbar} instance.
     */
    @NotNull
    private Toolbar getToolbar(Preference preferenceScreen, Dialog preferenceScreenDialog) {
        Toolbar toolbar = new Toolbar(preferenceScreen.getContext());
        toolbar.setTitle(preferenceScreen.getTitle());
        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
        toolbar.setNavigationOnClickListener(view -> {
            SearchView searchView = searchViewRef.get();
            String searchQuery = (searchView != null) ? searchView.getQuery().toString() : "";
            preferenceScreenDialog.dismiss();
            if (!searchQuery.isEmpty()) filterPreferences(searchQuery);
        });
        return toolbar;
    }

    // endregion [Preference Management] UI Setup

    // endregion Preference Management

    // region Search

    // region [Search] Initialize and Store Preferences

    /**
     * Initializes the {@link #customDependencyMap} by parsing the preferences XML resource.
     * <p>
     * It reads the "searchDependency" attribute to establish custom dependency relationships.
     * It uses a stack to keep track of the current category (PreferenceScreen title) hierarchy.
     * <p>
     * The dependencies are stored in the {@link #customDependencyMap}.
     */
    private void initializeCustomDependencies() {
        int xmlId = ResourceUtils.getXmlIdentifier("revanced_prefs");
        if (xmlId == 0) {
            Logger.printException(() -> "Could not find XML resource: revanced_prefs");
            return;
        }

        try (XmlResourceParser parser = getResources().getXml(xmlId)) {
            Stack<String> categoryTitleStack = new Stack<>();
            int eventType = parser.getEventType();

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    handleStartTag(parser, categoryTitleStack);
                } else if (eventType == XmlPullParser.END_TAG && PREFERENCE_SCREEN_TAG.equals(parser.getName())) {
                    if (!categoryTitleStack.isEmpty()) {
                        categoryTitleStack.pop();
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Logger.printException(() -> "Error parsing XML for custom dependencies", e);
        }
    }

    /**
     * Handles a start tag in the XML parser, updating the category stack and dependency map.
     *
     * @param parser             The XML parser.
     * @param categoryTitleStack The stack of category titles.
     */
    private void handleStartTag(XmlResourceParser parser, Stack<String> categoryTitleStack) {
        String tagName = parser.getName();
        if (PREFERENCE_SCREEN_TAG.equals(tagName)) {
            String screenTitle = getAttributeValue(parser, TITLE_ATTRIBUTE);
            if (screenTitle != null) {
                categoryTitleStack.push(ResourceUtils.getString(screenTitle.substring(1)));
            }
            return;
        }

        String key = getAttributeValue(parser, KEY_ATTRIBUTE);
        String dependency = getAttributeValue(parser, DEPENDENCY_ATTRIBUTE);
        if (key != null && dependency != null) {
            String categoryKey = categoryTitleStack.isEmpty() ? "" : categoryTitleStack.peek() + ":" + key;
            customDependencyMap.put(categoryKey, dependency);
        }
    }

    /**
     * Retrieves the value of a specified attribute from the given XML parser.
     *
     * @param parser        The {@link XmlResourceParser} currently parsing the XML document.
     * @param attributeName The name of the attribute to retrieve.
     * @return The value of the attribute if found, otherwise null.
     */
    private String getAttributeValue(XmlResourceParser parser, String attributeName) {
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            if (attributeName.equals(parser.getAttributeName(i))) {
                return parser.getAttributeValue(i);
            }
        }
        return null;
    }

    /**
     * Recursively scans a PreferenceGroup and stores all its preferences, including those in nested groups,
     * into the {@link #groupedPreferences} map. It also populates the {@link #dependencyMap} with
     * dependency relationships based on custom dependencies.
     *
     * @param group The PreferenceGroup to scan.
     */
    private void storeAllPreferences(PreferenceGroup group) {
        Set<Preference> prefs = groupedPreferences.computeIfAbsent(group, k -> new LinkedHashSet<>());

        for (int i = 0, n = group.getPreferenceCount(); i < n; i++) {
            Preference pref = group.getPreference(i);
            prefs.add(pref);

            // Only add non-PreferenceGroup preferences to dependencyMap
            if (!(pref instanceof PreferenceGroup)) {
                String depKey = getCustomDependency(pref, group);
                if (depKey != null) {
                    dependencyMap.computeIfAbsent(depKey, k -> new ArrayList<>()).add(pref);
                }
            }

            if (pref instanceof PreferenceGroup) {
                storeAllPreferences((PreferenceGroup) pref);
            }
        }
    }

    /**
     * Asynchronously builds the dependency cache for preferences to optimize search performance.
     * No real need to use it, but keep it for now.
     */
    @SuppressWarnings("unused")
    private void buildDependencyCacheAsync() {
        new Thread(() -> {
            buildDependencyCache();
            Logger.printDebug(() -> "Dependency cache built successfully");
        }).start();
    }

    /**
     * Builds a cache mapping each preference's cache key to a list of indices of related preferences.
     */
    private void buildDependencyCache() {
        preferenceList.clear();
        dependencyCache.clear();

        // Populate preferenceList with PreferenceInfo objects
        for (var entry : groupedPreferences.entrySet()) {
            PreferenceGroup group = entry.getKey();
            String groupTitle = group.getTitle() != null ? group.getTitle().toString() : "";
            for (Preference pref : entry.getValue()) {
                if (pref.getKey() != null) {
                    preferenceList.add(new PreferenceInfo(pref, groupTitle));
                }
            }
        }

        // Build dependency cache
        for (int index = 0; index < preferenceList.size(); index++) {
            PreferenceInfo info = preferenceList.get(index);
            if (info.key == null) continue;
            List<Integer> relatedIndices = new ArrayList<>();
            addPreferenceAndDependenciesToCache(relatedIndices, index);
            dependencyCache.put(info.cacheKey, relatedIndices);
        }
    }

    /**
     * Recursively collects indices of a preference and its dependencies.
     *
     * @param relatedIndices The list to store indices of related preferences.
     * @param currentIndex   The index of the current preference in preferenceList.
     */
    private void addPreferenceAndDependenciesToCache(List<Integer> relatedIndices, int currentIndex) {
        Set<Integer> visited = new HashSet<>();  // Tracks visited indices to prevent cycles
        Stack<Integer> stack = new Stack<>();
        stack.push(currentIndex);

        while (!stack.isEmpty()) {
            int index = stack.pop();
            if (visited.contains(index)) continue;
            visited.add(index);
            relatedIndices.add(index);

            PreferenceInfo currentInfo = preferenceList.get(index);
            PreferenceGroup group = groupedPreferences.keySet().stream()
                    .filter(g -> g.getTitle() != null && g.getTitle().toString().equals(currentInfo.groupTitle))
                    .findFirst().orElse(null);
            String currentDependencyKey = getDependencyKey(currentInfo.preference, group);

            // Handle direct dependency
            if (currentDependencyKey != null) {
                int depIndex = findPreferenceIndexByKey(currentDependencyKey);
                if (depIndex >= 0 && !visited.contains(depIndex)) {
                    stack.push(depIndex);
                }
            }

            // Handle dependent preferences
            if (dependencyMap.containsKey(currentInfo.key)) {
                for (Preference depPref : Objects.requireNonNull(dependencyMap.get(currentInfo.key))) {
                    int depIndex = findPreferenceIndexByKey(depPref.getKey());
                    if (depIndex >= 0 && !visited.contains(depIndex)) {
                        stack.push(depIndex);
                    }
                }
            }
        }
    }

    /**
     * Finds the index of a preference in preferenceList by its key.
     *
     * @param key The preference key to search for.
     * @return The index, or -1 if not found.
     */
    private int findPreferenceIndexByKey(String key) {
        for (int i = 0; i < preferenceList.size(); i++) {
            if (key.equals(preferenceList.get(i).key)) {
                return i;
            }
        }
        return -1;
    }

    // endregion [Search] Initialize and Store Preferences

    // region [Search] Filter Preferences

    /**
     * Filters the preferences based on a given search query.
     * It populates the PreferenceScreen with the search results.
     * <p>
     * The filtering logic considers the preference title, summary, and entries (for ListPreferences).
     * It also takes into account dependencies between preferences, ensuring that dependent preferences are
     * displayed along with the preferences they depend on.
     * <p>
     * {@code android:dependency} dependencies show the queried setting itself and its dependency (parent).<br>
     * {@code app:searchDependency} dependencies show the queried setting itself, alongside its peers that have the same
     * {@code app:searchDependency} key, and their dependency (parent).
     *
     * @param query The search query string.
     */
    public void filterPreferences(@NonNull String query) {
        if (query.isEmpty() || groupedPreferences.isEmpty()) {
            resetPreferences();
            Logger.printInfo(() -> "No preferences available or query empty");
            return;
        }

        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            Logger.printException(() -> "PreferenceScreen is null");
            return;
        }
        screen.removeAll();

        String lowerQuery = query.toLowerCase();
        Map<PreferenceGroup, List<Preference>> matchedGroups = new LinkedHashMap<>();

        for (var entry : groupedPreferences.entrySet()) {
            processGroup(entry.getKey(), entry.getValue(), lowerQuery, matchedGroups);
        }

        matchedGroups.forEach((group, matches) -> {
            PreferenceGroup targetGroup = group == rootPreferenceScreen ? screen : createCategoryGroup(screen, group);
            addPreferencesToGroup(targetGroup, matches, group);
        });

        int totalPreferences = matchedGroups.values().stream().mapToInt(List::size).sum();
        Logger.printDebug(() -> "Filtered '" + query + "': " + matchedGroups.size() + " groups, " + totalPreferences + " preferences");
    }

    /**
     * Processes a preference group to find matches for the search query.
     *
     * @param group         The PreferenceGroup to process.
     * @param preferences   The preferences in the group.
     * @param lowerQuery    The lowercase search query.
     * @param matchedGroups The map to store matched groups and preferences.
     */
    private void processGroup(PreferenceGroup group, Set<Preference> preferences, String lowerQuery,
                              Map<PreferenceGroup, List<Preference>> matchedGroups) {
        CharSequence title = group.getTitle() != null ? group.getTitle() : "";
        Set<String> keysToInclude = new LinkedHashSet<>();
        List<Preference> matches = new ArrayList<>();

        for (Preference pref : preferences) {
            boolean isRootGroup = group == rootPreferenceScreen;
            boolean isPreferenceGroup = pref instanceof PreferenceGroup;
            if (shouldIncludePreference(pref, lowerQuery, title, keysToInclude) &&
                    (!isPreferenceGroup || isRootGroup)) {
                matches.add(pref);
            }
        }

        if (!matches.isEmpty()) {
            matchedGroups.put(group, matches);
        }
    }

    /**
     * Creates a ClickablePreferenceCategory for a group and adds it to the screen.
     *
     * @param screen The PreferenceScreen to add the category to.
     * @param group  The PreferenceGroup to create a category for.
     * @return The created ClickablePreferenceCategory.
     */
    private PreferenceGroup createCategoryGroup(PreferenceScreen screen, PreferenceGroup group) {
        ClickablePreferenceCategory category = new ClickablePreferenceCategory(this, findClosestPreferenceScreen(group));
        category.setTitle(getFullPath(group));
        screen.addPreference(category);
        return category;
    }

    /**
     * Determines if a preference should be included in search results based on the query and dependencies.
     *
     * @param pref          The preference to evaluate.
     * @param lowerQuery    The lowercase search query.
     * @param groupTitle    The title of the preference's group.
     * @param keysToInclude The set to store keys of related preferences.
     * @return True if the preference should be included, false otherwise.
     */
    private boolean shouldIncludePreference(@NonNull Preference pref, @NonNull String lowerQuery,
                                            @NonNull CharSequence groupTitle, @NonNull Set<String> keysToInclude) {

        if (!preferenceMatches(pref, lowerQuery)) return false;

        String key = pref.getKey();
        if (key == null) return true;

        String cacheKey = groupTitle + ":" + key;
        List<Integer> cachedIndices = dependencyCache.get(cacheKey);

        if (cachedIndices != null) {
            for (int index : cachedIndices) keysToInclude.add(preferenceList.get(index).cacheKey);
        } else {
            Logger.printDebug(() -> "Missing cache for key: " + cacheKey);
            keysToInclude.add(cacheKey);
        }

        return true;
    }

    /**
     * Checks if a given preference matches the search query. The match is considered true if the query
     * is found in the preference's title, summary, or (for SwitchPreferences and ListPreferences) specific fields.
     *
     * @param preference The preference to check.
     * @param query      The search query (already converted to lowercase).
     * @return True if the preference matches the query, false otherwise.
     */
    private boolean preferenceMatches(Preference preference, String query) {
        // Check if the query is in the title or summary.
        return preference.getTitle().toString().toLowerCase().contains(query) ||
                (preference.getSummary() != null && preference.getSummary().toString().toLowerCase().contains(query)) ||
                // Check specific fields for SwitchPreferences and ListPreferences.
                matchesSwitchPreference(preference, query) ||
                matchesListPreference(preference, query);
    }

    /**
     * Checks if a SwitchPreference matches the search query. The match is considered true if the query
     * is found in the preference's "summary on" or "summary off" text.
     *
     * @param preference The SwitchPreference to check.
     * @param query      The search query (already converted to lowercase).
     * @return True if the SwitchPreference matches the query, false otherwise.
     */
    private boolean matchesSwitchPreference(Preference preference, String query) {
        if (!(preference instanceof SwitchPreference switchPreference)) return false;
        return (switchPreference.getSummaryOn() != null && switchPreference.getSummaryOn().toString().toLowerCase().contains(query)) ||
                (switchPreference.getSummaryOff() != null && switchPreference.getSummaryOff().toString().toLowerCase().contains(query));
    }

    /**
     * Checks if a ListPreference matches the search query. The match is considered true if the query
     * is found in any of the preference's entry titles or entry values.
     *
     * @param preference The ListPreference to check.
     * @param query      The search query (already converted to lowercase).
     * @return True if the ListPreference matches the query, false otherwise.
     */
    private boolean matchesListPreference(Preference preference, String query) {
        if (!(preference instanceof ListPreference listPreference)) return false;
        return hasMatchingEntries(listPreference.getEntries(), query) ||
                hasMatchingEntries(listPreference.getEntryValues(), query);
    }

    /**
     * Checks if any entry in a CharSequence array matches the query.
     *
     * @param entries The array of entries to check.
     * @param query   The lowercase search query.
     * @return True if any entry matches, false otherwise.
     */
    private boolean hasMatchingEntries(CharSequence[] entries, String query) {
        if (entries == null) return false;
        for (CharSequence entry : entries)
            if (entry.toString().toLowerCase().contains(query)) return true;
        return false;
    }

    /**
     * Adds a list of preferences and their dependencies to the target PreferenceGroup.
     *
     * @param targetGroup The PreferenceGroup (PreferenceScreen or ClickablePreferenceCategory) to add preferences to.
     * @param preferences The list of preferences to add.
     * @param group       The original PreferenceGroup containing the preferences (for dependency lookup).
     */
    private void addPreferencesToGroup(PreferenceGroup targetGroup, List<Preference> preferences, PreferenceGroup group) {
        for (Preference preference : preferences) {
            if (preference instanceof PreferenceCategory && targetGroup instanceof PreferenceScreen) {
                handleCategoryPreference(targetGroup, (PreferenceCategory) preference, group);
            } else {
                addSinglePreference(targetGroup, preference, group);
            }
        }
    }

    /**
     * Handles adding preferences from a PreferenceCategory to a target group.
     *
     * @param targetGroup The target PreferenceGroup.
     * @param category    The PreferenceCategory to process.
     * @param group       The original PreferenceGroup.
     */
    private void handleCategoryPreference(PreferenceGroup targetGroup, PreferenceCategory category, PreferenceGroup group) {
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            Preference childPref = category.getPreference(i);
            addSinglePreference(targetGroup, childPref, group);
        }
    }

    /**
     * Adds a single preference to a target group, handling dependencies.
     *
     * @param targetGroup The target PreferenceGroup.
     * @param preference  The preference to add.
     * @param group       The original PreferenceGroup.
     */
    private void addSinglePreference(PreferenceGroup targetGroup, Preference preference, PreferenceGroup group) {
        String key = preference.getKey();

        if (key != null) {
            String dependencyKey = getDependencyKey(preference, group);
            addPreferenceWithDependencies(targetGroup, preference, dependencyKey);
        } else {
            targetGroup.addPreference(preference);
        }

        Logger.printDebug(() -> "Added preference: " + (key != null ? key : "No Key") +
                " to group: " + (group == rootPreferenceScreen ? "top-level" : group.getTitle()));
    }

    /**
     * Recursively adds a preference and its dependencies to the specified {@link PreferenceGroup}.
     * Ensures that:
     * <ul>
     *   <li>Dependencies are added before dependent preferences to avoid display issues or crashes.</li>
     *   <li>Each preference is added only once, using a set of visited preferences to prevent duplicates.</li>
     * </ul>
     *
     * @param preferenceGroup    The {@link PreferenceGroup} to which the preference and its dependencies are added.
     * @param preference         The {@link Preference} to add.
     * @param dependencyKey      The key of the preference on which this preference depends, or null if none.
     * @param visitedPreferences A {@link Set} tracking unique preference keys to avoid duplicate additions.
     */
    private void addPreferenceWithDependencies(PreferenceGroup preferenceGroup, Preference preference,
                                               String dependencyKey, Set<String> visitedPreferences) {
        String key = preference.getKey();
        String uniqueKey = (preferenceGroup.getTitle() != null ? preferenceGroup.getTitle().toString() : "") + ":" + key;
        if (key == null || visitedPreferences.contains(uniqueKey)) {
            return;
        }
        visitedPreferences.add(uniqueKey);

        String groupTitle = preferenceGroup.getTitle() != null ? preferenceGroup.getTitle().toString() : "Unknown Group";
        if (!canAddPreference(preference, dependencyKey, groupTitle, preferenceGroup, visitedPreferences)) {
            return;
        }

        preferenceGroup.addPreference(preference);
        addDependentPreferences(preferenceGroup, key, preferenceGroup, visitedPreferences);
    }

    /**
     * Checks if a preference can be added based on its dependencies.
     *
     * @param preference         The preference to check.
     * @param dependencyKey      The custom dependency key.
     * @param groupTitle         The title of the preference group.
     * @param preferenceGroup    The target PreferenceGroup.
     * @param visitedPreferences The set of visited preference keys.
     * @return True if the preference can be added, false otherwise.
     */
    private boolean canAddPreference(Preference preference, String dependencyKey, String groupTitle,
                                     PreferenceGroup preferenceGroup, Set<String> visitedPreferences) {
        if (dependencyKey != null) {
            Preference dependency = findPreferenceInAllGroups(dependencyKey);
            if (dependency == null) {
                Logger.printDebug(() -> "Skipping preference '" + preference.getKey() + "' in group '" + groupTitle +
                        "' due to missing custom dependency: " + dependencyKey);
                return false;
            }
            // Pass visitedPreferences to prevent cycles
            addPreferenceWithDependencies(preferenceGroup, dependency, getDependencyKey(dependency, preferenceGroup), visitedPreferences);
        }

        String standardDependency = preference.getDependency();
        if (standardDependency != null && findPreference(standardDependency) == null) {
            Logger.printDebug(() -> "Skipping preference '" + preference.getKey() + "' in group '" + groupTitle +
                    "' because standard dependency '" + standardDependency + "' not found");
            return false;
        }
        return true;
    }

    /**
     * Adds preferences that depend on a given key to the target group.
     *
     * @param preferenceGroup    The target PreferenceGroup.
     * @param key                The key of the preference with dependents.
     * @param group              The original PreferenceGroup.
     * @param visitedPreferences The set of visited preference keys.
     */
    private void addDependentPreferences(PreferenceGroup preferenceGroup, String key, PreferenceGroup group,
                                         Set<String> visitedPreferences) {
        List<Preference> dependents = dependencyMap.get(key);
        if (dependents != null) {
            for (Preference dependentPreference : dependents) {
                addPreferenceWithDependencies(preferenceGroup, dependentPreference, getDependencyKey(dependentPreference, group), visitedPreferences);
            }
        }
    }

    /**
     * Recursively adds a preference and its dependencies to the given PreferenceGroup.
     * This method ensures that:<p>
     * 1. Dependencies are added before the preferences that depend on them to prevent display issues and crashes.<br>
     * 2. A preference is added only once, even if it's a dependency of multiple other preferences.
     *
     * @param preferenceGroup The PreferenceGroup to which the preference should be added.
     * @param preference      The preference to add.
     * @param dependencyKey   The dependency key of the preference.
     */
    private void addPreferenceWithDependencies(PreferenceGroup preferenceGroup, Preference preference, String dependencyKey) {
        addPreferenceWithDependencies(preferenceGroup, preference, dependencyKey, new HashSet<>());
    }

    /**
     * Retrieves the custom dependency key for a given preference, if one is defined in the {@link #customDependencyMap}.
     *
     * @param preference The preference for which to find the custom dependency.
     * @param group      The PreferenceGroup containing the preference.
     * @return The custom dependency key (which is the key of another preference) if found, otherwise null.
     */
    private String getCustomDependency(Preference preference, PreferenceGroup group) {
        String categoryTitle = (group == null || group.getTitle() == null) ? "" : group.getTitle().toString();
        String categoryKey = categoryTitle + ":" + preference.getKey();
        return customDependencyMap.get(categoryKey);
    }

    /**
     * Determines the dependency key for a given preference. It first checks for a custom dependency
     * (app:searchDependency), and if none is found, it falls back to the standard Android dependency
     * (android:dependency) attribute.
     *
     * @param preference The preference for which to find the dependency key.
     * @param group      The PreferenceGroup containing the preference.
     * @return The dependency key (which is the key of another preference), or null if no dependency is found.
     */
    private String getDependencyKey(Preference preference, PreferenceGroup group) {
        String customDependency = getCustomDependency(preference, group);
        return customDependency != null ? customDependency : preference.getDependency();
    }

    /**
     * Finds a preference within all the stored preference groups based on its key.
     *
     * @param key The key of the preference to search for.
     * @return The found Preference object, or null if no preference with the given key is found.
     */
    private Preference findPreferenceInAllGroups(String key) {
        for (Set<Preference> preferences : groupedPreferences.values()) {
            for (Preference preference : preferences) {
                if (preference.getKey() != null && preference.getKey().equals(key)) {
                    return preference;
                }
            }
        }
        return null;
    }

    /**
     * Resets the {@link PreferenceScreen} to its original state, restoring all preferences
     * from the {@link #originalPreferenceScreen} and clearing search results.
     */
    public void resetPreferences() {
        PreferenceScreen current = getPreferenceScreen();
        if (current != rootPreferenceScreen) {
            super.setPreferenceScreen(rootPreferenceScreen);
        }
        rootPreferenceScreen.removeAll();
        getAllPreferencesBy(originalPreferenceScreen).forEach(rootPreferenceScreen::addPreference);

        Activity activity = getActivity();
        if (activity instanceof VideoQualitySettingsActivity) {
            ((VideoQualitySettingsActivity) activity).updateToolbarTitle(rvxSettingsLabel);
        }

        Logger.printDebug(() -> "resetPreferences: Refreshed and reset PreferenceScreen for UI update");
    }

    // endregion [Search] Filter Preferences

    // endregion Search

    // region Import/Export

    /**
     * Add Preference to Import/Export settings submenu
     */
    private void setBackupRestorePreference() {
        findPreference("revanced_extended_settings_import").setOnPreferenceClickListener(pref -> {
            importActivity();
            return false;
        });

        findPreference("revanced_extended_settings_export").setOnPreferenceClickListener(pref -> {
            exportActivity();
            return false;
        });
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    private void exportActivity() {
        @SuppressLint("SimpleDateFormat") final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final String appName = ExtendedUtils.getAppLabel();
        final String versionName = ExtendedUtils.getAppVersionName();
        final String formatDate = dateFormat.format(new Date(System.currentTimeMillis()));
        final String fileName = String.format("%s_v%s_%s.txt", appName, versionName, formatDate);

        final Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    /**
     * Invoke the SAF(Storage Access Framework) to import settings
     */
    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Exports settings to a text file using the provided URI.
     *
     * @param uri The URI of the file to write settings to.
     */
    private void exportText(Uri uri) {
        final Context context = this.getActivity();

        try {
            @SuppressLint("Recycle")
            FileWriter jsonFileWriter =
                    new FileWriter(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "w"))
                                    .getFileDescriptor()
                    );
            PrintWriter printWriter = new PrintWriter(jsonFileWriter);
            printWriter.write(Setting.exportToJson(context));
            printWriter.close();
            jsonFileWriter.close();

            showToastShort(str("revanced_extended_settings_export_success"));
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_export_failed"));
        }
    }

    /**
     * Imports settings from a text file using the provided URI.
     *
     * @param uri The URI of the file to read settings from.
     */
    private void importText(Uri uri) {
        final Context context = this.getActivity();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            settingImportInProgress = true;

            @SuppressLint("Recycle")
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
            showToastShort(str("revanced_extended_settings_import_failed"));
            throw new RuntimeException(e);
        } finally {
            settingImportInProgress = false;
        }
    }

    // endregion Import/Export

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            mPreferenceManager = getPreferenceManager();
            mPreferenceManager.setSharedPreferencesName(Setting.preferences.name);
            mSharedPreferences = mPreferenceManager.getSharedPreferences();
            addPreferencesFromResource(getXmlIdentifier("revanced_prefs"));

            rootPreferenceScreen = getPreferenceScreen();

            // Initialize toolbars and other UI elements
            setPreferenceScreenToolbar();

            // Initialize ReVanced settings
            ReVancedSettingsPreference.initializeReVancedSettings();
            SponsorBlockSettingsPreference.init(getActivity());

            // Import/export
            setBackupRestorePreference();

            // Store all preferences and their dependencies
            initializeCustomDependencies();
            storeAllPreferences(getPreferenceScreen());
            // buildDependencyCacheAsync();
            buildDependencyCache();

            // Load and set initial preferences states
            for (Setting<?> setting : Setting.allLoadedSettings()) {
                final Preference preference = mPreferenceManager.findPreference(setting.key);
                if (preference != null && isSDKAbove(26)) {
                    preference.setSingleLineTitle(false);
                }

                if (preference instanceof SwitchPreference switchPreference) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    switchPreference.setChecked(boolSetting.get());
                } else if (preference instanceof EditTextPreference editTextPreference) {
                    editTextPreference.setText(setting.get().toString());
                } else if (preference instanceof ListPreference listPreference) {
                    if (setting.equals(DEFAULT_PLAYBACK_SPEED) || setting.equals(DEFAULT_PLAYBACK_SPEED_SHORTS)) {
                        listPreference.setEntries(CustomPlaybackSpeedPatch.getEntries());
                        listPreference.setEntryValues(CustomPlaybackSpeedPatch.getEntryValues());
                    }
                    if (setting.equals(SPOOF_STREAMING_DATA_TYPE)) {
                        listPreference.setEntries(SpoofStreamingDataPatch.getEntries());
                        listPreference.setEntryValues(SpoofStreamingDataPatch.getEntryValues());
                    }
                    if (!(preference instanceof app.revanced.extension.youtube.settings.preference.SegmentCategoryListPreference)) {
                        updateListPreferenceSummary(listPreference, setting);
                    }
                }
            }

            // Register preference change listener
            mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);

            originalPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            copyPreferences(getPreferenceScreen(), originalPreferenceScreen);

            sortPreferenceListMenu(Settings.CHANGE_START_PAGE);
            sortPreferenceListMenu(Settings.SPOOF_STREAMING_DATA_LANGUAGE);
            sortPreferenceListMenu(BaseSettings.REVANCED_LANGUAGE);
        } catch (Exception th) {
            Logger.printException(() -> "Error during onCreate()", th);
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (getPreferenceScreen() != null && getPreferenceScreen() != preferenceScreen) {
            preferenceScreenStack.push(getPreferenceScreen());
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null && preferenceScreen.getTitle() != null) {
            Activity activity = getActivity();
            if (activity instanceof VideoQualitySettingsActivity) {
                ((VideoQualitySettingsActivity) activity).updateToolbarTitle(preferenceScreen.getTitle().toString());
            }
        }
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

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        dependencyCache.clear();
        preferenceList.clear();
        Utils.resetLocalizedContext();
        super.onDestroy();
    }

    /**
     * Stores metadata for a preference to avoid repeated string operations.
     */
    private static class PreferenceInfo {
        final Preference preference;
        final String key;
        final String groupTitle;
        final String cacheKey;

        PreferenceInfo(Preference preference, String groupTitle) {
            this.preference = preference;
            this.key = preference.getKey();
            this.groupTitle = groupTitle != null ? groupTitle : "";
            this.cacheKey = this.groupTitle + ":" + this.key;
        }
    }
}
