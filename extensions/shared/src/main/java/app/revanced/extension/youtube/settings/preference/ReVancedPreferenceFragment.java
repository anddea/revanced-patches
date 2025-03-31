package app.revanced.extension.youtube.settings.preference;

import static com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity.setToolbarLayoutParams;
import static app.revanced.extension.shared.settings.BaseSettings.SPOOF_STREAMING_DATA_TYPE;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;
import static app.revanced.extension.shared.utils.Utils.isSDKAbove;
import static app.revanced.extension.shared.utils.Utils.showToastShort;
import static app.revanced.extension.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED;
import static app.revanced.extension.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED_SHORTS;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT;
import static app.revanced.extension.youtube.settings.Settings.HIDE_PREVIEW_COMMENT_TYPE;

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
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Pair;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.TextView;
import android.widget.Toolbar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends PreferenceFragment {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    static boolean settingImportInProgress = false;
    static boolean showingUserDialogMessage;

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

    /**
     * @return If the preference is currently set to the default value of the Setting.
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

    static PreferenceManager mPreferenceManager;
    private SharedPreferences mSharedPreferences;

    private PreferenceScreen originalPreferenceScreen;

    public ReVancedPreferenceFragment() {
        // Required empty public constructor
    }

    private void putPreferenceScreenMap(SortedMap<String, PreferenceScreen> preferenceScreenMap, PreferenceGroup preferenceGroup) {
        if (preferenceGroup instanceof PreferenceScreen mPreferenceScreen) {
            preferenceScreenMap.put(mPreferenceScreen.getKey(), mPreferenceScreen);
        }
    }

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
                    if (!(nestedPreference instanceof PreferenceGroup childPreferenceGroup))
                        continue;
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

                        // Edge-to-edge is enforced if the following conditions are met:
                        // 1. targetSDK is 35 or greater (YouTube 19.44.39 or greater).
                        // 2. user is using Android 15 or greater.
                        //
                        // Related Issues:
                        // https://github.com/ReVanced/revanced-patches/issues/3976
                        // https://github.com/ReVanced/revanced-patches/issues/4606
                        //
                        // Docs:
                        // https://developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
                        //
                        // Since ReVanced Settings Activity do not use AndroidX libraries,
                        // You will need to manually fix the layout breakage caused by edge-to-edge.
                        if (isEdgeToEdgeSupported) {
                            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                                Insets statusInsets = insets.getInsets(WindowInsets.Type.statusBars());
                                Insets navInsets = insets.getInsets(WindowInsets.Type.navigationBars());
                                v.setPadding(0, statusInsets.top, 0, navInsets.bottom);
                                return insets;
                            });
                        }

                        Toolbar toolbar = new Toolbar(preferenceScreen.getContext());

                        toolbar.setTitle(preferenceScreen.getTitle());
                        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
                        toolbar.setNavigationOnClickListener(view -> preferenceScreenDialog.dismiss());

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

    // region Search Fragment: Constants

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

    // endregion

    // region Search Fragment: Variables

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
     * Keeps track of preferences that have already been added to the search results.
     * <p>
     * This prevents duplicate entries when a preference is matched multiple times
     * due to dependencies or multiple search criteria.
     * <p>
     * Example: For preferences <pre>{@code
     * <PreferenceScreen ... android:title="@string/preference_screen_title" ...>
     *     <Preference android:key="preference_key_1" ...>
     *     <Preference android:key="preference_key_2" ...>
     *     ...
     * </PreferenceScreen>
     * ...
     * }</pre>
     * <p>
     * addedPreferences will be <pre>{@code
     * addedPreferences = [preference_screen_title:preference_key_1, preference_screen_title:preference_key_2, ...]
     * }</pre>
     */
    private final Set<String> addedPreferences = new HashSet<>();

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
    private final Map<PreferenceGroup, List<Preference>> groupedPreferences = new LinkedHashMap<>();

    /**
     * Stores custom dependencies defined in the preferences XML file with {@link #DEPENDENCY_ATTRIBUTE}.
     * <p>
     * <b>Key:</b> <i>A string representing the category and key of a preference in the format "Category:Key".
     *                The category is derived from the title of the parent PreferenceScreen.</i><br>
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

    // endregion

    // region Search Fragment: Initialization

    /**
     * Initializes the {@link #customDependencyMap} by parsing the preferences XML resource.
     * <p>
     * It reads the "searchDependency" attribute to establish custom dependency relationships.
     * It uses a stack to keep track of the current category (PreferenceScreen title) hierarchy.
     * <p>
     * The dependencies are stored in the {@link #customDependencyMap}.
     */
    private void initializeCustomDependencies() {
        // Get the XML resource ID.
        int xmlId = ResourceUtils.getXmlIdentifier("revanced_prefs");
        if (xmlId == 0) {
            Logger.printException(() -> "Could not find XML resource: revanced_prefs");
            return;
        }

        try (XmlResourceParser parser = getResources().getXml(xmlId)) {
            // Stack to store the titles of PreferenceScreens, representing the category hierarchy.
            Stack<String> categoryTitleStack = new Stack<>();
            int eventType = parser.getEventType();

            // Iterate through the XML document.
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    // Handle PreferenceScreen tags to track the category hierarchy.
                    if (PREFERENCE_SCREEN_TAG.equals(tagName)) {
                        String screenTitle = getAttributeValue(parser, TITLE_ATTRIBUTE);
                        if (screenTitle != null) {
                            // Push the category title onto the stack. Assume titles start with "@" (e.g., "@string/title").
                            categoryTitleStack.push(ResourceUtils.getString(screenTitle.substring(1)));
                        }
                    }

                    // Extract key and custom dependency attributes.
                    String key = getAttributeValue(parser, KEY_ATTRIBUTE);
                    String dependency = getAttributeValue(parser, DEPENDENCY_ATTRIBUTE);

                    // If both key and custom dependency are present, add them to the map.
                    if (key != null && dependency != null) {
                        // Build the category key (e.g., "Category:Key").
                        String categoryKey = buildCategoryKey(categoryTitleStack) + ":" + key;
                        // Store the custom dependency.
                        customDependencyMap.put(categoryKey, dependency);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    // Pop the category title from the stack when exiting a PreferenceScreen.
                    if (PREFERENCE_SCREEN_TAG.equals(parser.getName()) && !categoryTitleStack.isEmpty()) {
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
     * Builds a string representing the current category hierarchy.
     *
     * @param categoryTitleStack The stack containing the title of the parent PreferenceScreen.
     *                           The top of the stack represents the most immediate parent category.
     * @return A string representing the most immediate parent title, or an empty string if the stack is empty.
     */
    private String buildCategoryKey(Stack<String> categoryTitleStack) {
        return categoryTitleStack.isEmpty() ? "" : categoryTitleStack.peek();
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

    // endregion

    // region Search Fragment: Preference Storage

    /**
     * Recursively scans a PreferenceGroup and stores all its preferences, including those in nested groups,
     * into the {@link #groupedPreferences} map. It also populates the {@link #dependencyMap} with
     * dependency relationships based on custom dependencies.
     *
     * @param preferenceGroup The PreferenceGroup to scan.
     */
    private void storeAllPreferences(PreferenceGroup preferenceGroup) {
        // Determine if this is the root PreferenceScreen.
        boolean isRootScreen = preferenceGroup == getPreferenceScreen();
        // Use a special top-level group for the root screen, otherwise use the given group.
        PreferenceGroup groupKey = isRootScreen ? createTopLevelGroup(preferenceGroup.getContext()) : preferenceGroup;

        // Get the list of preferences for this group, creating it if it doesn't exist.
        List<Preference> currentGroupPreferences = groupedPreferences.computeIfAbsent(groupKey, k -> new ArrayList<>());

        // Iterate through all preferences in the group.
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);

            // Add the preference to the group's list if it's not already present.
            if (!currentGroupPreferences.contains(preference)) {
                currentGroupPreferences.add(preference);
            }

            // Check for custom dependencies and update the dependencyMap.
            String dependencyKey = getCustomDependency(preference, preferenceGroup);
            if (dependencyKey != null) {
                dependencyMap.computeIfAbsent(dependencyKey, k -> new ArrayList<>()).add(preference);
            }

            // Recursively process nested PreferenceGroups.
            if (preference instanceof PreferenceGroup) {
                storeAllPreferences((PreferenceGroup) preference);
            }
        }
    }

    /**
     * Creates a special top-level PreferenceGroup to represent the root PreferenceScreen
     * in the {@link #groupedPreferences} map. This allows treating the root screen like
     * any other PreferenceGroup for easier processing.
     *
     * @param context The application context.
     * @return A new PreferenceCategory representing the top-level group, titled "ReVanced Extended".
     */
    private PreferenceGroup createTopLevelGroup(Context context) {
        PreferenceCategory topLevelGroup = new PreferenceCategory(context);
        topLevelGroup.setTitle(ResourceUtils.getString("revanced_extended_settings_title"));
        return topLevelGroup;
    }

    // endregion

    // region Search Fragment: Dependency Handling

    /**
     * Retrieves the custom dependency key for a given preference, if one is defined in the {@link #customDependencyMap}.
     *
     * @param preference The preference for which to find the custom dependency.
     * @param group      The PreferenceGroup containing the preference.
     * @return The custom dependency key (which is the key of another preference) if found, otherwise null.
     */
    private String getCustomDependency(Preference preference, PreferenceGroup group) {
        // Construct the category key based on the preference group's title and the preference's key.
        String categoryTitle = (group == null || group.getTitle() == null) ? "" : group.getTitle().toString();
        String categoryKey = categoryTitle + ":" + preference.getKey();

        // Return the custom dependency key from the map.
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
        // First, try to get a custom dependency.
        String dependencyKey = getCustomDependency(preference, group);
        // If no custom dependency is found, fall back to the standard dependency.
        if (dependencyKey == null) {
            dependencyKey = preference.getDependency();
        }
        return dependencyKey;
    }

    /**
     * Recursively adds a preference and its dependencies to the set of keys to include in the search results.
     * It handles both forward dependencies (preferences that depend on this one) and backward dependencies
     * (preferences on which this one depends).
     *
     * @param preference    The preference to add.
     * @param keysToInclude The set of keys to be included in the search results.
     *                      A key is added to this set only if it hasn't been added before, ensuring uniqueness.
     * @param dependencyKey The dependency key of the preference, which could be a custom dependency or a standard one.
     * @param group         The PreferenceGroup containing the preference.
     */
    private void addPreferenceAndDependencies(Preference preference, Set<String> keysToInclude, String dependencyKey, PreferenceGroup group) {
        String key = preference.getKey();
        // Add the preference's key to the set only if it's not already present.
        if (key != null && keysToInclude.add(group.getTitle() + ":" + key)) {
            // Add the parent preference on which this preference depends on
            if (dependencyKey != null) {
                Preference dependency = findPreferenceInAllGroups(dependencyKey);
                if (dependency != null) {
                    addPreferenceAndDependencies(dependency, keysToInclude, getDependencyKey(dependency, group), group);
                }
            }

            // Recursively add preferences that depend on this one.
            if (dependencyMap.containsKey(key)) {
                Objects.requireNonNull(dependencyMap.get(key)).forEach(dependentPreference -> addPreferenceAndDependencies(dependentPreference, keysToInclude, getDependencyKey(dependentPreference, group), group));
            }
        }
    }

    /**
     * Finds a preference within all the stored preference groups based on its key.
     *
     * @param key The key of the preference to search for.
     * @return The found Preference object, or null if no preference with the given key is found.
     */
    private Preference findPreferenceInAllGroups(String key) {
        return groupedPreferences.values().stream()
                .flatMap(List::stream)
                .filter(preference -> preference.getKey() != null && preference.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    // endregion

    // region Search Fragment: Search Filtering

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
    public void filterPreferences(String query) {
        // If the query is empty, reset the preference screen to its original state.
        if (query == null || query.isEmpty()) {
            resetPreferences();
            return;
        }

        // Get the root PreferenceScreen.
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        // Remove all existing preferences from the screen.
        preferenceScreen.removeAll();
        // Clear the set of added preferences.
        addedPreferences.clear();

        String finalQuery = query.toLowerCase();
        // This map will hold the preferences that match the search query, grouped by their parent group.
        Map<PreferenceGroup, List<Preference>> matchedGroupPreferences = new LinkedHashMap<>();
        // This set will hold the unique keys of preferences to be included in the results.
        Set<String> keysToInclude = new HashSet<>();

        // First pass: Identify preferences that match the query and their dependencies.
        groupedPreferences.forEach((group, preferences) -> preferences.stream()
                .filter(preference -> preferenceMatches(preference, finalQuery))
                .forEach(preference -> {
                    String dependencyKey = getDependencyKey(preference, group);
                    // Add the matching preference and its dependencies to keysToInclude.
                    addPreferenceAndDependencies(preference, keysToInclude, dependencyKey, group);
                }));

        // Second pass: Build the matchedGroupPreferences map using the identified keys.
        groupedPreferences.forEach((group, preferences) -> {
            List<Preference> matchedPreferences = preferences.stream()
                    .filter(preference -> keysToInclude.contains(group.getTitle() + ":" + preference.getKey()))
                    .collect(Collectors.toList());
            if (!matchedPreferences.isEmpty()) {
                matchedGroupPreferences.put(group, matchedPreferences);
            }
        });

        // Add the matched preferences to the screen, maintaining the original order.
        matchedGroupPreferences.forEach((group, matchedPreferences) -> {
            // Create a new PreferenceCategory for each group.
            PreferenceCategory category = new PreferenceCategory(preferenceScreen.getContext());
            category.setTitle(group.getTitle());
            preferenceScreen.addPreference(category);

            // Add each matched preference to the category.
            matchedPreferences.forEach(preference -> {
                String dependencyKey = getDependencyKey(preference, group);
                addPreferenceWithDependencies(category, preference, dependencyKey);
            });
        });
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
        // Only check if it's a SwitchPreference.
        if (!(preference instanceof SwitchPreference switchPreference)) return false;
        // Check if the query is in the "summary on" or "summary off" text.
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
        // Only check if it's a ListPreference.
        if (!(preference instanceof ListPreference listPreference)) return false;
        // Check if the query is in any of the entry titles or entry values.
        return listPreference.getEntries() != null &&
                Arrays.stream(listPreference.getEntries()).anyMatch(entry -> entry.toString().toLowerCase().contains(query)) ||
                listPreference.getEntryValues() != null &&
                        Arrays.stream(listPreference.getEntryValues()).anyMatch(entryValue -> entryValue.toString().toLowerCase().contains(query));
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
        String key = preference.getKey();
        String uniqueKey = preferenceGroup.getTitle() + ":" + key;

        // Only add the preference if it hasn't been added already.
        if (key != null && addedPreferences.add(uniqueKey)) {
            // Add dependencies first to avoid possible crashes.
            // Recursively add the preference on which this one depends.
            if (dependencyKey != null) {
                Preference dependency = findPreferenceInAllGroups(dependencyKey);
                if (dependency != null) {
                    addPreferenceWithDependencies(preferenceGroup, dependency, getDependencyKey(dependency, preferenceGroup));
                } else {
                    return;
                }
            }

            // Add the preference itself.
            preferenceGroup.addPreference(preference);

            // Recursively add preferences that depend on this one.
            if (dependencyMap.containsKey(key)) {
                Objects.requireNonNull(dependencyMap.get(key)).forEach(dependentPreference -> addPreferenceWithDependencies(preferenceGroup, dependentPreference, getDependencyKey(dependentPreference, preferenceGroup)));
            }
        }
    }

    // endregion

    // region Search Fragment: Resetting

    /**
     * Resets the preference screen to its original state, removing any search results and restoring
     * all preferences to their default positions.
     */
    public void resetPreferences() {
        // Get the root PreferenceScreen.
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        // Remove all preferences from the screen.
        preferenceScreen.removeAll();
        // Re-add all preferences from the original screen.
        getAllPreferencesBy(originalPreferenceScreen).forEach(preferenceScreen::addPreference);
    }

    // endregion

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            mPreferenceManager = getPreferenceManager();
            mPreferenceManager.setSharedPreferencesName(Setting.preferences.name);
            mSharedPreferences = mPreferenceManager.getSharedPreferences();
            addPreferencesFromResource(getXmlIdentifier("revanced_prefs"));

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

    private void copyPreferences(PreferenceScreen source, PreferenceScreen destination) {
        for (Preference preference : getAllPreferencesBy(source)) {
            destination.addPreference(preference);
        }
    }

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        Utils.resetLocalizedContext();
        super.onDestroy();
    }

    /**
     * Sorts a preference list by menu entries, but preserves the first value as the first entry.
     *
     * @noinspection SameParameterValue
     */
    private static void sortListPreferenceByValues(ListPreference listPreference, int firstEntriesToPreserve) {
        CharSequence[] entries = listPreference.getEntries();
        CharSequence[] entryValues = listPreference.getEntryValues();
        final int entrySize = entries.length;

        if (entrySize != entryValues.length) {
            // Xml array declaration has a missing/extra entry.
            throw new IllegalStateException();
        }

        // Since the text of Preference is Spanned, CharSequence#toString() should not be used.
        // If CharSequence#toString() is used, Spanned styling, such as HTML syntax, will be broken.
        List<Pair<CharSequence, CharSequence>> firstPairs = new ArrayList<>(firstEntriesToPreserve);
        List<Pair<CharSequence, CharSequence>> pairsToSort = new ArrayList<>(entrySize);

        for (int i = 0; i < entrySize; i++) {
            Pair<CharSequence, CharSequence> pair = new Pair<>(entries[i], entryValues[i]);
            if (i < firstEntriesToPreserve) {
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

    private void sortPreferenceListMenu(EnumSetting<?> setting) {
        Preference preference = findPreference(setting.key);
        if (preference instanceof ListPreference languagePreference) {
            sortListPreferenceByValues(languagePreference, 1);
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
}
