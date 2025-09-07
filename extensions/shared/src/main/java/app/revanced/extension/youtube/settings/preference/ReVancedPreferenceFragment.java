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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.patches.spoof.SpoofStreamingDataPatch;
import app.revanced.extension.shared.settings.BaseSettings;
import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.settings.EnumSetting;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.preference.LogBufferManager;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.StringRef;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.extension.youtube.settings.ClickablePreferenceCategory;
import app.revanced.extension.youtube.settings.LicenseActivityHook;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.ThemeUtils;
import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static app.revanced.extension.shared.patches.PatchStatus.PatchVersion;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.extension.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.extension.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.*;
import static app.revanced.extension.youtube.settings.LicenseActivityHook.rvxSettingsLabel;
import static app.revanced.extension.youtube.settings.LicenseActivityHook.searchViewRef;
import static app.revanced.extension.youtube.settings.Settings.*;

@SuppressWarnings("deprecation")
public class ReVancedPreferenceFragment extends PreferenceFragment {
    private static final int READ_REQUEST_CODE = 42;
    private static final int WRITE_REQUEST_CODE = 43;
    boolean settingExportInProgress = false;
    private boolean mDynamicPrefsIndexed = false;

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
    private static final String SEARCH_HISTORY_DELIMITER = "|";
    private static final int MAX_SEARCH_HISTORY_SIZE = 50;
    static boolean settingImportInProgress = false;
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
    private final Map<String, HistoryPreference> historyViewPreferenceCache = new HashMap<>();
    private final Map<String, Preference> allPreferencesByKey = new HashMap<>();
    private final Map<PreferenceGroup, String> groupFullPaths = new HashMap<>();
    private final Map<Preference, PreferenceGroup> preferenceToParentGroupMap = new HashMap<>();
    private final Map<Preference, PreferenceInfo> preferenceInfoMap = new HashMap<>();

    // endregion Search-related fields

    public Stack<PreferenceScreen> preferenceScreenStack = new Stack<>();
    public PreferenceScreen rootPreferenceScreen;
    private boolean showingUserDialogMessage = false;

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
                updateListPreferenceSummary(listPreference, setting);
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

    private List<String> searchHistoryCache = null;
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
     * @param pref    The {@link Preference} to check.
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

        throw new IllegalStateException("Must override method to handle preference type: " + pref.getClass());
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

            Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                    context,
                    str("revanced_extended_confirm_user_dialog_title"), // Title.
                    userDialogMessage.toString(), // No message.
                    null, // No EditText.
                    null, // OK button text.
                    () -> {
                        if (setting.rebootApp) {
                            showRestartDialog(context);
                        }
                    },
                    () -> {
                        // Cancel button action. Restore whatever the setting was before the change.
                        // Restore whatever the setting was before the change.
                        if (setting instanceof BooleanSetting booleanSetting &&
                                pref instanceof SwitchPreference switchPreference) {
                            switchPreference.setChecked(booleanSetting.defaultValue);
                        } else if (setting instanceof EnumSetting<?> enumSetting &&
                                pref instanceof ListPreference listPreference) {
                            listPreference.setValue(enumSetting.defaultValue.toString());
                            updateListPreferenceSummary(listPreference, setting);
                        }
                    },
                    null, // No Neutral button.
                    null, // No Neutral button action.
                    true  // Dismiss dialog when onNeutralClick.
            );

            Dialog dialog = dialogPair.first;
            dialog.setOnDismissListener(d ->
                    Utils.runOnMainThread(() -> showingUserDialogMessage = false)
            );
            dialog.show();
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
                        if (preferenceScreenDialog == null) return false;

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
                            toolbarTextView.setTextColor(ThemeUtils.getAppForegroundColor());
                        }

                        LicenseActivityHook.setToolbarLayoutParams(toolbar);

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
     * @param preferenceGroup     The PreferenceGroup to check and potentially add.
     */
    private void putPreferenceScreenMap(SortedMap<String, PreferenceScreen> preferenceScreenMap, PreferenceGroup preferenceGroup) {
        if (preferenceGroup instanceof PreferenceScreen mPreferenceScreen) {
            preferenceScreenMap.put(mPreferenceScreen.getKey(), mPreferenceScreen);
        }
    }

    /**
     * Creates and configures a Toolbar for a PreferenceScreen dialog.
     *
     * @param preferenceScreen       The {@link PreferenceScreen} associated with the toolbar.
     * @param preferenceScreenDialog The dialog containing the PreferenceScreen.
     * @return A configured {@link Toolbar} instance.
     */
    @NotNull
    private Toolbar getToolbar(Preference preferenceScreen, Dialog preferenceScreenDialog) {
        Toolbar toolbar = new Toolbar(preferenceScreen.getContext());
        toolbar.setTitle(preferenceScreen.getTitle());
        toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
        toolbar.setNavigationOnClickListener(view -> {
            preferenceScreenDialog.dismiss();
            SearchView searchView = searchViewRef.get();
            String searchQuery = (searchView != null) ? searchView.getQuery().toString() : "";
            if (!searchQuery.isEmpty()) filterPreferences(searchQuery);
        });

        int margin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()
        );
        toolbar.setTitleMargin(margin, 0, margin, 0);

        TextView toolbarTextView = getChildView(toolbar, TextView.class::isInstance);
        if (toolbarTextView != null) {
            toolbarTextView.setTextColor(ThemeUtils.getAppForegroundColor());
        }

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
            preferenceToParentGroupMap.put(pref, group);

            if (pref.getKey() != null) {
                allPreferencesByKey.put(pref.getKey(), pref);
            }

            // Store dependency for non-PreferenceGroup preferences with a key
            if (!(pref instanceof PreferenceGroup) && pref.getKey() != null) {
                String depKey = getCustomDependency(pref, group);
                if (depKey != null) {
                    dependencyMap.computeIfAbsent(depKey, k -> new ArrayList<>()).add(pref);
                }
            }

            if (pref instanceof PreferenceGroup) {
                storeAllPreferences((PreferenceGroup) pref);
            }
        }

        groupFullPaths.put(group, getFullPath(group));
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
        long startTime = System.nanoTime();

        PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) return;
        screen.removeAll();

        // Clear all previous highlights and reset highlighting state
        for (PreferenceInfo info : preferenceInfoMap.values()) clearHighlighting(info);

        if (query.isEmpty() && Settings.SETTINGS_SEARCH_HISTORY.get()) {
            displaySearchHistory(screen);
        } else if (query.isEmpty() || groupedPreferences.isEmpty()) {
            resetPreferences();
        } else {
            filterAndDisplayPreferences(screen, query.toLowerCase());
        }

        long endTime = System.nanoTime();
        Logger.printDebug(() -> "filterPreferences took " + ((endTime - startTime) / 1_000_000.0) + " ms");
    }

    /**
     * Filters preferences based on a query and displays them on the provided screen.
     *
     * @param screen     The PreferenceScreen to display filtered preferences.
     * @param lowerQuery The lowercase query string to match against preference titles/keys.
     */
    private void filterAndDisplayPreferences(PreferenceScreen screen, String lowerQuery) {
        if (screen == null || lowerQuery == null) {
            throw new IllegalArgumentException("Screen and query must not be null");
        }

        // Map of groups to their directly matched preferences
        Map<PreferenceGroup, List<Preference>> matchedPreferencesByGroup = new LinkedHashMap<>();
        // This set tracks keyed preferences that have been fully processed (added along with their dependencies)
        // during *this specific search operation* to prevent redundant processing and cycles.
        Set<String> processedKeyedItemsForSearch = new HashSet<>();

        // Find direct matches
        findDirectMatches(lowerQuery, matchedPreferencesByGroup);

        // Display matches or show "No results"
        if (matchedPreferencesByGroup.isEmpty()) {
            screen.addPreference(new NoResultsPreference(screen.getContext(), lowerQuery, false));
        } else {
            matchedPreferencesByGroup.forEach((xmlParentOfMatchedItem, listOfDirectlyMatchedItems) -> {
                PreferenceGroup displayGroupForMatchedItems = xmlParentOfMatchedItem == rootPreferenceScreen
                        ? screen
                        : createCategoryGroup(screen, xmlParentOfMatchedItem);

                for (Preference directlyMatchedItem : listOfDirectlyMatchedItems) {
                    applyHighlighting(directlyMatchedItem, lowerQuery);
                    addPreferenceWithDependencies(
                            displayGroupForMatchedItems,
                            directlyMatchedItem,
                            getDependencyKey(directlyMatchedItem, xmlParentOfMatchedItem),
                            processedKeyedItemsForSearch
                    );
                }
            });
        }
    }

    /**
     * Finds preferences that match the query and organizes them by their original group.
     *
     * @param lowerQuery                The lowercase query string to match against preference fields.
     * @param matchedPreferencesByGroup The map to store matched preferences.
     */
    private void findDirectMatches(String lowerQuery, Map<PreferenceGroup, List<Preference>> matchedPreferencesByGroup) {
        for (Map.Entry<PreferenceGroup, LinkedHashSet<Preference>> entry : groupedPreferences.entrySet()) {
            PreferenceGroup parentOfPref = entry.getKey();
            for (Preference pref : entry.getValue()) {
                if (preferenceMatches(pref, lowerQuery)) {
                    if (!(pref instanceof PreferenceCategory)) {
                        matchedPreferencesByGroup.computeIfAbsent(parentOfPref, k -> new ArrayList<>()).add(pref);
                    }
                }
            }
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
        screen.addPreference(new SpacerPreference(getContext()));

        SearchResultCategory category = new SearchResultCategory(this, findClosestPreferenceScreen(group));
        category.setTitle(groupFullPaths.get(group));
        screen.addPreference(category);
        return category;
    }

    /**
     * Checks if a given preference matches the search query. The match is considered true if the query
     * is found in the preference's title, summary, or (for SwitchPreferences and ListPreferences) specific fields.
     *
     * @param preference The preference to check.
     * @param lowerQuery The lowercase query string to match against preference fields.
     * @return True if the preference matches the query, false otherwise.
     */
    private boolean preferenceMatches(Preference preference, String lowerQuery) {
        String title = preference.getTitle() != null ? preference.getTitle().toString().toLowerCase() : "";
        String summary = preference.getSummary() != null ? preference.getSummary().toString().toLowerCase() : "";
        return title.contains(lowerQuery) || summary.contains(lowerQuery) ||
                matchesSwitchPreference(preference, lowerQuery) ||
                matchesListPreference(preference, lowerQuery);
    }

    /**
     * Checks if a SwitchPreference matches the search query. The match is considered true if the query
     * is found in the preference's "summary on" or "summary off" text.
     *
     * @param preference The SwitchPreference to check.
     * @param lowerQuery The lowercase query string to match against the summaries.
     * @return True if the SwitchPreference matches the query, false otherwise.
     */
    private boolean matchesSwitchPreference(Preference preference, String lowerQuery) {
        if (!(preference instanceof SwitchPreference switchPreference)) return false;
        String summaryOn = switchPreference.getSummaryOn() != null ? switchPreference.getSummaryOn().toString().toLowerCase() : "";
        String summaryOff = switchPreference.getSummaryOff() != null ? switchPreference.getSummaryOff().toString().toLowerCase() : "";
        return summaryOn.contains(lowerQuery) || summaryOff.contains(lowerQuery);
    }

    /**
     * Checks if a ListPreference matches the search query. The match is considered true if the query
     * is found in any of the preference's entry titles or entry values.
     *
     * @param preference The ListPreference to check.
     * @param lowerQuery The lowercase query string to match against the entries.
     * @return True if the ListPreference matches the query, false otherwise.
     */
    private boolean matchesListPreference(Preference preference, String lowerQuery) {
        if (!(preference instanceof ListPreference listPreference)) return false;
        return hasMatchingEntries(listPreference.getEntries(), lowerQuery) ||
                hasMatchingEntries(listPreference.getEntryValues(), lowerQuery);
    }

    /**
     * Checks if any entry in a CharSequence array matches the query.
     *
     * @param entries    The array of entries to check.
     * @param lowerQuery The lowercase query string to match against the entries.
     * @return True if any entry matches, false otherwise.
     */
    private boolean hasMatchingEntries(CharSequence[] entries, String lowerQuery) {
        if (entries == null) return false;
        for (CharSequence entry : entries)
            if (entry != null && entry.toString().toLowerCase().contains(lowerQuery)) return true;
        return false;
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
    private void addPreferenceWithDependencies(
            PreferenceGroup preferenceGroup,
            Preference preference,
            String dependencyKey,
            Set<String> visitedPreferences
    ) {
        String preferenceKey = preference.getKey();

        if (preferenceKey == null) {
            // Handle keyless preference (e.g., <Preference title="foo"/>).
            // These preferences already have a parent in the original hierarchy, so we cannot
            // add the same instance to the search results screen.
            // Instead, we create a new, simple Preference to act as a placeholder/link.
            try {
                // Create a new proxy preference that is safe to add.
                final Preference proxyPreference = getProxyPreference(preference);

                // Add the new, parent-less proxy preference to the search results.
                preferenceGroup.addPreference(proxyPreference);
            } catch (Exception e) {
                Logger.printException(() -> "Failed to add proxy for keyless matched item: " + (preference.getTitle() != null ? preference.getTitle() : "Untitled"), e);
            }
            return;
        }

        // Handle keyed preference
        if (visitedPreferences.contains(preferenceKey)) {
            // This item's dependency chain has been (or is being) processed.
            // However, it might need to be visually added to *this specific* preferenceGroup
            // if it wasn't already (e.g., if it's a dependency reached via another path).
            // Add the preference itself to the preferenceGroup
            // Ensure it's not already visually there (e.g. if it was added as a dependency of something else)
            boolean alreadyInThisGroup = false;
            for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                Preference p = preferenceGroup.getPreference(i);
                // Check by instance and by key
                if (p == preference || (p.getKey() != null && p.getKey().equals(preferenceKey))) {
                    alreadyInThisGroup = true;
                    break;
                }
            }
            if (!alreadyInThisGroup) {
                // Temporarily disable dependency to skip findPreferenceInHierarchy validation during add
                String originalDependency = preference.getDependency();
                if (originalDependency != null) {
                    preference.setDependency(null);
                }

                try {
                    preferenceGroup.addPreference(preference);

                    // Immediately restore dependency AFTER add (while attached to search hierarchy)
                    // This enables state evaluation (en/disabling) in search results
                    if (originalDependency != null) {
                        preference.setDependency(originalDependency);
                    }

                    // Optional: Force notify hierarchy change to trigger immediate dep state update
                    // (e.g., if parent was added earlier and is unchecked, this grays out the pref now)
                    if (preference.getPreferenceManager() != null) {
                        preference.getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener); // Ensure listener is active
                    }
                } catch (Exception e) {
                    // Logger.printException(() -> "Failed to re-add keyed item to a different group: " + preferenceKey, e);
                    visitedPreferences.remove(preferenceKey);
                    return;
                }
            }
            return;
        }
        visitedPreferences.add(preferenceKey);

        // Resolve and add its main dependency (dependencyKey refers to android:dependency or app:searchDependency parent)
        if (dependencyKey != null) {
            Preference dependency = findPreferenceInAllGroups(dependencyKey);
            if (dependency != null) {
                PreferenceGroup dependencyParent = findParentGroup(dependency);
                addPreferenceWithDependencies(preferenceGroup, dependency,
                        getDependencyKey(dependency, dependencyParent),
                        visitedPreferences);
            } else {
                Logger.printDebug(() -> "Dependency " + dependencyKey + " for " + preferenceKey + " not found.");
            }
        }

        // Add the preference itself to the preferenceGroup
        // Ensure it's not already visually there (e.g. if it was added as a dependency of something else)
        boolean alreadyInThisGroup = false;
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference p = preferenceGroup.getPreference(i);
            // Check by instance and by key
            if (p == preference || (p.getKey() != null && p.getKey().equals(preferenceKey))) {
                alreadyInThisGroup = true;
                break;
            }
        }
        if (!alreadyInThisGroup) {
            // Temporarily disable dependency to skip findPreferenceInHierarchy validation during add
            String originalDependency = preference.getDependency();
            if (originalDependency != null) {
                preference.setDependency(null);
            }

            try {
                preferenceGroup.addPreference(preference);
            } catch (Exception e) {
                // Restore dep even on failure
                if (originalDependency != null) {
                    preference.setDependency(originalDependency);
                }
                Logger.printException(() -> "Failed to add keyed item: " + preferenceKey, e);
                visitedPreferences.remove(preferenceKey);
                return;
            } finally {
                // Always restore original dependency after add (or failure)
                if (originalDependency != null) {
                    preference.setDependency(originalDependency);
                }
            }
        }

        // Resolve and add items that have 'preference' as their app:searchDependency parent.
        // dependencyMap stores: parentKey -> List<prefs that have app:searchDependency="parentKey">
        List<Preference> dependents = dependencyMap.get(preferenceKey);
        if (dependents != null) {
            for (Preference dependentPref : dependents) {
                PreferenceGroup dependentParent = findParentGroup(dependentPref);
                addPreferenceWithDependencies(preferenceGroup, dependentPref,
                        getDependencyKey(dependentPref, dependentParent),
                        visitedPreferences);
            }
        }
    }

    @NotNull
    private Preference getProxyPreference(Preference preference) {
        final Preference proxyPreference = new Preference(preference.getContext());

        // The title and summary on the original 'preference' object have already been
        // highlighted by the `applyHighlighting` call. We just copy these values.
        proxyPreference.setTitle(preference.getTitle());
        proxyPreference.setSummary(preference.getSummary());
        proxyPreference.setIcon(preference.getIcon());
        proxyPreference.setEnabled(preference.isEnabled());
        proxyPreference.setOrder(preference.getOrder());

        // Make the proxy behave like the original on click.
        if (preference instanceof PreferenceScreen) {
            // If the original was a PreferenceScreen, clicking the proxy should navigate to it.
            proxyPreference.setOnPreferenceClickListener(p -> {
                setPreferenceScreen((PreferenceScreen) preference);
                return true;
            });
        }
        else if (preference instanceof ImportExportPreference originalPref) {
            proxyPreference.setOnPreferenceClickListener(p -> {
                originalPref.onPreferenceClick(originalPref);
                originalPref.showDialog(null);
                return true;
            });
        }
        else {
            // For all other types, just copy the original click listener.
            proxyPreference.setOnPreferenceClickListener(preference.getOnPreferenceClickListener());
        }
        return proxyPreference;
    }

    /**
     * Determines the dependency key for a given preference. It first checks for a custom dependency
     * (app:searchDependency), and if none is found, it falls back to the standard Android dependency
     * (android:dependency) attribute.
     *
     * @param preference The preference for which to find the dependency key.
     * @param group      The PreferenceGroup containing the preference. Can be null, in which case
     *                   customDependency lookup might be less specific or skipped if it relies on group title.
     * @return The dependency key (which is the key of another preference), or null if no dependency is found.
     */
    @Nullable
    private String getDependencyKey(Preference preference, @Nullable PreferenceGroup group) {
        // Try custom dependency first
        String customDependency = getCustomDependency(preference, group);
        if (customDependency != null) {
            return customDependency;
        }
        // Fallback to standard android:dependency
        return preference.getDependency();
    }

    /**
     * Retrieves the custom dependency key for a given preference, if one is defined in the {@link #customDependencyMap}.
     *
     * @param preference The preference for which to find the custom dependency.
     * @param group      The PreferenceGroup containing the preference. Can be null.
     * @return The custom dependency key (which is the key of another preference) if found, otherwise null.
     */
    @Nullable
    private String getCustomDependency(Preference preference, @Nullable PreferenceGroup group) {
        if (preference.getKey() == null) return null;

        String categoryTitle = (group == null || group.getTitle() == null) ? "" : group.getTitle().toString();
        String categoryKey;

        if (group == rootPreferenceScreen || group == null) {
            if (categoryTitle.isEmpty()) {
                String directKeyLookup = customDependencyMap.get(preference.getKey());
                if (directKeyLookup != null) return directKeyLookup;
            }
        }

        // Original formation
        categoryKey = categoryTitle + ":" + preference.getKey();
        String dep = customDependencyMap.get(categoryKey);
        if (dep != null) return dep;

        // Fallback if the category was derived from a full path and customDependencyMap uses simpler keys
        if (group != null && group.getTitle() != null) {
            String immediateParentTitle = group.getTitle().toString();
            if (!immediateParentTitle.equals(categoryTitle) && !immediateParentTitle.isEmpty()) {
                categoryKey = immediateParentTitle + ":" + preference.getKey();
                dep = customDependencyMap.get(categoryKey);
                return dep;
            }
        }

        return null;
    }

    /**
     * Finds a preference within all the stored preference groups based on its key.
     *
     * @param key The key of the preference to search for.
     * @return The found Preference object, or null if no preference with the given key is found.
     */
    private Preference findPreferenceInAllGroups(String key) {
        return allPreferencesByKey.get(key);
    }

    /**
     * Finds the original XML parent PreferenceGroup of a given Preference.
     *
     * @param pref The preference whose XML parent is sought.
     * @return The parent PreferenceGroup, or null if not found as a child in groupedPreferences
     * (e.g., if pref is a top-level PreferenceScreen itself and is a key in groupedPreferences).
     */
    @Nullable
    private PreferenceGroup findParentGroup(Preference pref) {
        return preferenceToParentGroupMap.get(pref);
    }

    /**
     * Resets the {@link PreferenceScreen} to its original state, restoring all preferences
     * from the {@link #originalPreferenceScreen} and clearing search results.
     * Updates the main activity's toolbar title to the original settings label.
     */
    public void resetPreferences() {
        PreferenceScreen current = getPreferenceScreen();
        if (current != rootPreferenceScreen) {
            super.setPreferenceScreen(rootPreferenceScreen);
        }
        rootPreferenceScreen.removeAll();
        getAllPreferencesBy(originalPreferenceScreen).forEach(rootPreferenceScreen::addPreference);

        if (getActivity() != null) {
            LicenseActivityHook.updateToolbarTitle(rvxSettingsLabel);
        }

        Logger.printDebug(() -> "resetPreferences: Refreshed and reset PreferenceScreen for UI update");
    }

    // endregion [Search] Filter Preferences

    // region [Search] Highlight Filtered Preferences

    /**
     * Applies text highlighting to a preference's fields (title, summary, etc.) based on a search query.
     * Highlighting is applied only if the {@link Settings#SETTINGS_SEARCH_HIGHLIGHT} setting is enabled.
     * The method processes the preference's title, summary, and, for specific preference types like
     * {@link SwitchPreference} or {@link ListPreference}, additional fields like summaryOn/summaryOff or entries.
     * <p>
     * FIXME: Highlighting does not work for SponsorBlock segment summaries, and maybe in some other areas (unknown).
     *        Mostly works as expected.
     *
     * @param preference The preference to apply highlighting to.
     * @param lowerQuery The lowercase search query string for matching and highlighting.
     */
    private void applyHighlighting(Preference preference, String lowerQuery) {
        PreferenceInfo info = findOrCreatePreferenceInfo(preference);
        if (!Settings.SETTINGS_SEARCH_HIGHLIGHT.get() || TextUtils.isEmpty(lowerQuery)) return;

        boolean wasChanged = false;
        int highlightColor = ThemeUtils.getHighlightColor();

        // Define fields to highlight
        List<FieldHighlightInfo> fields = Arrays.asList(
                new FieldHighlightInfo(info.originalTitle, preference::setTitle),
                new FieldHighlightInfo(info.originalSummary, preference::setSummary),
                new FieldHighlightInfo(
                        preference instanceof SwitchPreference ? ((SwitchPreference) preference).getSummaryOn() : null,
                        text -> { if (preference instanceof SwitchPreference) ((SwitchPreference) preference).setSummaryOn(text); }
                ),
                new FieldHighlightInfo(
                        preference instanceof SwitchPreference ? ((SwitchPreference) preference).getSummaryOff() : null,
                        text -> { if (preference instanceof SwitchPreference) ((SwitchPreference) preference).setSummaryOff(text); }
                )
        );

        // Highlight text fields
        for (FieldHighlightInfo field : fields) {
            if (field.text != null && field.text.toString().toLowerCase().contains(lowerQuery)) {
                field.setter.accept(highlightText(field.text, lowerQuery, highlightColor));
                wasChanged = true;
            } else if (field.text != null) {
                field.setter.accept(field.text); // Restore original
            }
        }

        // Handle ListPreference entries
        if (preference instanceof ListPreference lp && info.originalEntries != null) {
            wasChanged |= highlightListEntries(lp, info.originalEntries, lowerQuery, highlightColor);
        }

        info.highlightingApplied = wasChanged;
    }

    /**
     * Highlights the entries of a {@link ListPreference} if they contain the search query.
     * The original entries are preserved in the {@link PreferenceInfo}, and highlighted versions
     * are applied to the preference if a match is found.
     *
     * @param lp              The {@link ListPreference} whose entries are to be highlighted.
     * @param originalEntries The original entries of the preference, stored in {@link PreferenceInfo}.
     * @param lowerQuery      The lowercase search query string for highlighting matched text.
     * @param highlightColor  The precomputed highlight color to apply.
     * @return {@code true} if any entry was highlighted, {@code false} otherwise.
     */
    private boolean highlightListEntries(ListPreference lp, CharSequence[] originalEntries, String lowerQuery, int highlightColor) {
        CharSequence[] processedEntries = new CharSequence[originalEntries.length];
        boolean changed = false;

        for (int i = 0; i < originalEntries.length; i++) {
            CharSequence entry = originalEntries[i];
            if (entry != null && entry.toString().toLowerCase().contains(lowerQuery)) {
                processedEntries[i] = highlightText(entry, lowerQuery, highlightColor);
                changed = true;
            } else {
                processedEntries[i] = entry;
            }
        }

        if (changed) lp.setEntries(processedEntries);
        return changed;
    }

    /**
     * Highlights occurrences of a search query in the provided text by applying a background color.
     * Highlighting is only applied if {@link Settings#SETTINGS_SEARCH_HIGHLIGHT} is enabled.
     *
     * @param text           The text to highlight.
     * @param lowerQuery     The lowercase search query string for matching.
     * @param highlightColor The precomputed highlight color to apply.
     * @return A {@link SpannableStringBuilder} with highlighted text, or the original text if
     * highlighting is disabled or the text is empty.
     */
    private CharSequence highlightText(CharSequence text, String lowerQuery, int highlightColor) {
        if (TextUtils.isEmpty(text) || !Settings.SETTINGS_SEARCH_HIGHLIGHT.get() || TextUtils.isEmpty(lowerQuery)) {
            return text;
        }

        SpannableStringBuilder spannable = new SpannableStringBuilder(text);
        String textString = text.toString().toLowerCase();

        int lastIndex = 0;
        while (lastIndex < textString.length()) {
            int foundAtIndex = textString.indexOf(lowerQuery, lastIndex);
            if (foundAtIndex != -1) {
                int endIndex = foundAtIndex + lowerQuery.length();
                spannable.setSpan(
                        new BackgroundColorSpan(highlightColor),
                        foundAtIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                lastIndex = endIndex;
            } else {
                break;
            }
        }

        return spannable;
    }

    /**
     * Clears highlighting from a preference by restoring its original text fields.
     * The original values are retrieved from the {@link PreferenceInfo} and applied to the
     * preference's title, summary, and, for specific types like {@link SwitchPreference} or
     * {@link ListPreference}, additional fields like summaryOn/summaryOff or entries.
     *
     * @param info The {@link PreferenceInfo} containing the preference and its original values.
     */
    private void clearHighlighting(PreferenceInfo info) {
        Preference preference = info.preference;

        // Restore preference's text fields to their original states stored in PreferenceInfo.
        preference.setTitle(info.originalTitle);
        preference.setSummary(info.originalSummary);

        if (preference instanceof SwitchPreference sp) {
            sp.setSummaryOn(info.originalSummaryOn);
            sp.setSummaryOff(info.originalSummaryOff);
        }
        if (preference instanceof ListPreference lp && info.originalEntries != null) {
            lp.setEntries(info.originalEntries);
        }
        info.highlightingApplied = false;
    }

    /**
     * Retrieves or creates a {@link PreferenceInfo} object for a given preference.
     * If a matching {@link PreferenceInfo} exists in the {@code preferenceList}, it is returned.
     * Otherwise, a new {@link PreferenceInfo} is created, added to the list, and returned.
     *
     * @param preference The preference for which to find or create a {@link PreferenceInfo}.
     * @return The existing or newly created {@link PreferenceInfo} for the preference.
     */
    private PreferenceInfo findOrCreatePreferenceInfo(Preference preference) {
        return preferenceInfoMap.computeIfAbsent(preference, p -> {
            PreferenceInfo info = new PreferenceInfo(p, getGroupTitle(p));
            Logger.printDebug(() -> "Created new PreferenceInfo for: " + p.getKey());
            return info;
        });
    }

    /**
     * Retrieves the title of the preference group containing the specified preference.
     * Iterates through the {@code groupedPreferences} map to find the group that includes
     * the preference and returns its title.
     *
     * @param preference The preference whose group title is to be retrieved.
     * @return The title of the group containing the preference, or an empty string if not found.
     */
    private String getGroupTitle(Preference preference) {
        return groupedPreferences.entrySet().stream()
                .filter(entry -> entry.getValue().contains(preference))
                .map(entry -> entry.getKey().getTitle() != null ? entry.getKey().getTitle().toString() : "")
                .findFirst()
                .orElse("");
    }

    // endregion [Search] Highlight Filtered Preferences

    // region [Search] Search History

    /**
     * Adds a search query to the search history, removing duplicates and enforcing a maximum size.
     * Inserts the query at the beginning of the history and removes any existing instance to prevent
     * duplication. Trims older entries if the history exceeds {@link #MAX_SEARCH_HISTORY_SIZE}.
     * Takes no action if the {@link Settings#SETTINGS_SEARCH_HISTORY} setting is disabled or the query
     * is empty or contains only whitespace.
     *
     * @param query The search query to add to the history.
     */
    public void addToSearchHistory(String query) {
        Logger.printDebug(() -> "addToSearchHistory called with: " + query);
        if (!Settings.SETTINGS_SEARCH_HISTORY.get() || query.trim().isEmpty()) {
            return;
        }
        List<String> history = loadSearchHistory();
        history.remove(query); // Remove duplicates, move to front
        history.add(0, query); // Add to the beginning
        if (history.size() > MAX_SEARCH_HISTORY_SIZE) {
            history = history.subList(0, MAX_SEARCH_HISTORY_SIZE); // Trim to max size
        }
        saveSearchHistory(history);
    }

    /**
     * Loads the search history from the settings as a list of search queries.
     * Returns an empty list if the {@link Settings#SETTINGS_SEARCH_HISTORY} setting is disabled
     * or if the stored history is empty.
     *
     * @return A {@link List} of search query strings, or an empty list if no history is available.
     */
    private List<String> loadSearchHistory() {
        if (!Settings.SETTINGS_SEARCH_HISTORY.get()) {
            return new ArrayList<>();
        }

        if (searchHistoryCache != null) {
            return new ArrayList<>(searchHistoryCache);
        }

        String historyString = Settings.SETTINGS_SEARCH_HISTORY_ENTRIES.get();
        if (historyString.isEmpty()) {
            searchHistoryCache = new ArrayList<>();
        } else {
            searchHistoryCache = new ArrayList<>(Arrays.asList(historyString.split(Pattern.quote(SEARCH_HISTORY_DELIMITER))));
        }
        return new ArrayList<>(searchHistoryCache);
    }

    /**
     * Saves the provided search history to the settings.
     * The history is converted to a single string using {@link #SEARCH_HISTORY_DELIMITER} as a separator
     * and stored in {@link Settings#SETTINGS_SEARCH_HISTORY_ENTRIES}. No action is taken if
     * the {@link Settings#SETTINGS_SEARCH_HISTORY} setting is disabled.
     *
     * @param history The list of search query strings to save.
     */
    private void saveSearchHistory(List<String> history) {
        if (!Settings.SETTINGS_SEARCH_HISTORY.get()) {
            searchHistoryCache = null; // Clear cache if history is disabled
            return;
        }
        String historyString = String.join(SEARCH_HISTORY_DELIMITER, history);
        Settings.SETTINGS_SEARCH_HISTORY_ENTRIES.save(historyString);
        searchHistoryCache = new ArrayList<>(history); // Update cache
        historyViewPreferenceCache.clear(); // Clear view cache on save to force re-creation on next display
    }

    /**
     * Displays the search history on the provided {@link PreferenceScreen}.
     * The screen is cleared and populated with a {@link PreferenceCategory} containing
     * {@link HistoryPreference} entries for each search query in the history.
     * If the history is empty, a {@link NoResultsPreference} is displayed.
     * A "Clear History" preference is added to allow clearing the search history.
     *
     * @param screen The {@link PreferenceScreen} to display the search history on.
     */
    private void displaySearchHistory(PreferenceScreen screen) {
        long startTime = System.nanoTime();
        if (getActivity() != null) {
            LicenseActivityHook.updateToolbarTitle(str("revanced_search_title"));
        }

        List<String> history = loadSearchHistory();
        screen.removeAll();

        if (history.isEmpty()) {
            Preference noHistoryPref = new NoResultsPreference(getContext(), "", true);
            screen.addPreference(noHistoryPref);
        } else {
            PreferenceCategory historyCategory = new PreferenceCategory(getContext());
            historyCategory.setTitle(str("revanced_search_history_title"));
            screen.addPreference(historyCategory);

            for (String searchQuery : history) {
                HistoryPreference historyPref = getHistoryPreference(searchQuery);
                historyCategory.addPreference(historyPref);
            }

            Preference clearPref = new Preference(getContext());
            clearPref.setKey("clear_history");
            clearPref.setTitle(str("revanced_search_clear_history_title"));
            clearPref.setOnPreferenceClickListener(pref -> {
                new AlertDialog.Builder(getContext())
                        .setTitle(str("revanced_search_clear_confirm_title"))
                        .setMessage(str("revanced_search_clear_confirm_message"))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            saveSearchHistory(new ArrayList<>());
                            filterPreferences("");
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                return true;
            });
            screen.addPreference(clearPref);
        }

        long endTime = System.nanoTime();
        Logger.printDebug(() -> "displaySearchHistory took " + ((endTime - startTime) / 1_000_000.0) + " ms");
    }

    /**
     * Creates a {@link HistoryPreference} for a given search query.
     * The preference allows the user to re-run the search query by clicking it or delete it
     * via a confirmation dialog.
     *
     * @param searchQuery The search query for which to create the preference.
     * @return A configured {@link HistoryPreference} for the search query.
     */
    @NotNull
    private HistoryPreference getHistoryPreference(String searchQuery) {
        if (historyViewPreferenceCache.containsKey(searchQuery)) {
            return Objects.requireNonNull(historyViewPreferenceCache.get(searchQuery));
        }

        HistoryPreference historyPref = new HistoryPreference(getContext(), searchQuery, this::showDeleteHistoryDialog);
        historyPref.setOnPreferenceClickListener(pref -> {
            SearchView searchView = searchViewRef.get();
            if (searchView != null) {
                searchView.setQuery(searchQuery, true);
            }
            return true;
        });
        historyViewPreferenceCache.put(searchQuery, historyPref);
        return historyPref;
    }

    /**
     * Displays a confirmation dialog to delete a specific search query from the search history.
     *
     * @param query The search query to be deleted from the history.
     */
    private void showDeleteHistoryDialog(String query) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle(str("revanced_search_delete_confirm_title"))
                .setMessage(str("revanced_search_delete_confirm_message", query))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    List<String> updatedHistory = loadSearchHistory();
                    updatedHistory.remove(query);
                    saveSearchHistory(updatedHistory);
                    filterPreferences("");
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // endregion [Search] Search History

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
            settingExportInProgress = true;
            exportActivity();
            return false;
        });
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    private void exportActivity() {
        if (!settingExportInProgress && !BaseSettings.DEBUG.get()) {
            Utils.showToastShort(str("revanced_debug_logs_disabled"));
            return;
        }

        @SuppressLint("SimpleDateFormat")
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        final String appName = ExtendedUtils.getAppLabel();
        final String versionName = ExtendedUtils.getAppVersionName();
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
    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(isSDKAbove(29) ? "text/plain" : "*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
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
                showToastShort(str("revanced_extended_settings_export_success"));
            } else {
                showToastShort(str("revanced_debug_logs_export_success"));
            }
        } catch (IOException e) {
            if (settingExportInProgress) {
                showToastShort(str("revanced_extended_settings_export_failed"));
            } else {
                showToastShort(String.format(str("revanced_debug_logs_failed_to_export"), e.getMessage()));
            }
        } finally {
            settingExportInProgress = false;
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

    /**
     * Set Preference to Debug settings submenu
     */
    private void setDebugLogPreference() {
        Preference clearLog = findPreference("revanced_debug_logs_clear_buffer");
        if (clearLog == null) {
            return;
        }
        clearLog.setOnPreferenceClickListener(pref -> {
            LogBufferManager.clearLogBuffer();
            return false;
        });

        Preference exportLogToClipboard = findPreference("revanced_debug_export_logs_to_clipboard");
        if (exportLogToClipboard == null) {
            return;
        }
        exportLogToClipboard.setOnPreferenceClickListener(pref -> {
            LogBufferManager.exportToClipboard();
            return false;
        });

        Preference exportLogToFile = findPreference("revanced_debug_export_logs_to_file");
        if (exportLogToFile == null) {
            return;
        }
        exportLogToFile.setOnPreferenceClickListener(pref -> {
            exportActivity();
            return false;
        });
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

            // Import/export
            setBackupRestorePreference();

            // Debug log
            setDebugLogPreference();

            // Store all preferences and their dependencies for search
            initializeCustomDependencies();
            storeAllPreferences(getPreferenceScreen());

            // Log sizes of all relevant collections for search
            Logger.printDebug(() -> "allPreferencesByKey size: " + allPreferencesByKey.size());
            Logger.printDebug(() -> "customDependencyMap size: " + customDependencyMap.size());
            Logger.printDebug(() -> "dependencyMap size: " + dependencyMap.size());
            Logger.printDebug(() -> "groupFullPaths size: " + groupFullPaths.size());
            Logger.printDebug(() -> "groupedPreferences size: " + groupedPreferences.size());
            Logger.printDebug(() -> "preferenceToParentGroupMap size: " + preferenceToParentGroupMap.size());

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
                    updateListPreferenceSummary(listPreference, setting);
                }
            }

            // Register preference change listener
            mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);

            originalPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            copyPreferences(getPreferenceScreen(), originalPreferenceScreen);

            sortPreferenceListMenu(Settings.CHANGE_START_PAGE);
            sortPreferenceListMenu(BaseSettings.REVANCED_LANGUAGE);
        } catch (Exception th) {
            Logger.printException(() -> "Error during onCreate()", th);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // The dynamic preference group initializes itself in onAttachedToActivity,
        // which happens before the fragment's onResume. This is the perfect
        // time to check for and index its children, and we use a flag to
        // ensure this operation only runs once per fragment instance.
        if (!mDynamicPrefsIndexed) {
            indexDynamicallyLoadedPreferences();
            mDynamicPrefsIndexed = true;
        }
    }

    /**
     * Identifies all PreferenceGroups that load their children dynamically and triggers
     * the process to add their preferences to the search index.
     * This is called from onResume to ensure the dynamic groups have had
     * time to initialize.
     */
    private void indexDynamicallyLoadedPreferences() {
        Logger.printDebug(() -> "Attempting to index all dynamically loaded preference groups...");

        // Define all keys for preference groups that load their children dynamically.
        List<String> dynamicGroupKeys = Arrays.asList(
                "revanced_preference_group_sb",
                "revanced_sb_stats"
        );

        for (String key : dynamicGroupKeys) {
            tryIndexDynamicGroup(key);
        }

        Logger.printDebug(() -> "Dynamic indexing finished. Final allPreferencesByKey size: "
                + allPreferencesByKey.size());
    }

    /**
     * Finds a PreferenceGroup by its key and, if it has been dynamically populated with children,
     * adds them to the search index.
     *
     * @param preferenceKey The android:key of the PreferenceGroup to index.
     */
    private void tryIndexDynamicGroup(String preferenceKey) {
        Preference preference = findPreference(preferenceKey);

        if (preference instanceof PreferenceGroup dynamicGroup) {
            if (dynamicGroup.getPreferenceCount() > 0) {
                Logger.printDebug(() -> "Found populated group with key '" + preferenceKey + "'. Indexing "
                        + dynamicGroup.getPreferenceCount() + " children.");
                storeAllPreferences(dynamicGroup);
            } else {
                // This is a normal case if the group has no dynamic children to add.
                Logger.printDebug(() -> "Found group with key '" + preferenceKey + "', but it is empty.");
            }
        } else {
            // This is useful for debugging if the key is mistyped or the preference is not a group.
            if (preference == null) {
                Logger.printDebug(() -> "Could not find any preference with key '" + preferenceKey + "'.");
            } else {
                Logger.printDebug(() -> "Preference with key '" + preferenceKey + "' is not a PreferenceGroup.");
            }
        }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (getPreferenceScreen() != null && getPreferenceScreen() != preferenceScreen) {
            preferenceScreenStack.push(getPreferenceScreen());
        }
        super.setPreferenceScreen(preferenceScreen);
        if (preferenceScreen != null) {
            Activity activity = getActivity();
            if (activity != null) {
                String title = preferenceScreen.getTitle() != null
                        ? preferenceScreen.getTitle().toString()
                        : rvxSettingsLabel;
                LicenseActivityHook.updateToolbarTitle(title);
            }
        }
    }

    /**
     * Handles the back press action in the preference screen navigation.
     * <p>
     * The behavior follows a clear hierarchy:
     * <ol>
     *   <li><b>If on a sub-screen:</b> Navigates back to the previous screen in the stack.</li>
     *   <li><b>If the SearchView is focused and the query is not empty:</b> The first back press clears focus and
     *       hides the keyboard. The search results remain visible.</li>
     *   <li><b>If the SearchView is focused but the query is empty, OR if a query exists but the view is not focused:</b>
     *       The back press clears the search query, removes focus, and resets to the full preference list.</li>
     *   <li><b>If on the root screen with no query and no focus:</b> The back press signals to exit the activity.</li>
     * </ol>
     *
     * @param currentQuery The current search query string.
     * @return {@code true} if the back press should exit the activity (by calling
     * {@code super.onBackPressed()} in {@code LicenseActivity}), <br>
     * {@code false} if the back press is handled internally.
     */
    public boolean handleOnBackPressed(String currentQuery) {
        // First, handle navigation out of sub-screens
        if (getPreferenceScreen() != rootPreferenceScreen) {
            if (!preferenceScreenStack.isEmpty()) {
                PreferenceScreen previous = preferenceScreenStack.pop();
                setPreferenceScreen(previous); // This will update the toolbar title automatically
                if (!currentQuery.isEmpty()) {
                    filterPreferences(currentQuery); // Restore search results
                }
                return false;
            } else {
                // If stack is empty but not on root screen, exit the activity
                return true;
            }
        }

        // At this point, we are on the root preference screen.
        SearchView searchView = searchViewRef.get();

        // Case 1: SearchView has focus AND there's a search query.
        // A single back press should clear focus.
        // if (searchView != null && searchView.hasFocus() && !currentQuery.isEmpty()) {
        //     searchView.clearFocus();
        //     return false; // Back press is consumed. Search results remain.
        // }

        // Case 2: The search state needs to be fully cleared. This happens if:
        // a) The search view has focus but the query is empty.
        // b) A query exists, but the search view is no longer focused.
        if ((searchView != null && searchView.hasFocus()) || !currentQuery.isEmpty()) {
            if (searchView != null) {
                searchView.setQuery("", false);
                searchView.clearFocus();
            }
            resetPreferences();
            return false; // Back press is consumed. The view is reset.
        }

        // Case 3: We are on the root screen, with no search query and no focus.
        // The back press should exit the activity.
        return true;
    }

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
        mPreferenceManager = null;

        // Clear all cached collections
        preferenceInfoMap.clear();
        allPreferencesByKey.clear();
        groupFullPaths.clear();
        preferenceToParentGroupMap.clear();
        groupedPreferences.values().forEach(Set::clear);
        groupedPreferences.clear();
        dependencyMap.values().forEach(List::clear);
        dependencyMap.clear();
        customDependencyMap.clear();
        preferenceScreenStack.clear();
        searchHistoryCache = null;
        historyViewPreferenceCache.clear();
        mDynamicPrefsIndexed = false;

        Utils.resetLocalizedContext();
        super.onDestroy();
    }

    /**
     * A custom {@link Preference} that acts as a visual separator between preference groups.
     * It is non-selectable and has a fixed height to create vertical space.
     */
    private static class SpacerPreference extends Preference {
        public SpacerPreference(Context context) {
            super(context);
            // This preference is purely for visual spacing and should not be interactive.
            setSelectable(false);
            setEnabled(false);
            // Unique key to prevent any potential conflicts.
            setKey("spacer_" + System.nanoTime() + "_" + Math.random());
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            // Hide any default text or icons that might appear.
            view.setVisibility(View.INVISIBLE);
            int height = dipToPixels(16);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            params.height = height;
            view.setLayoutParams(params);
        }
    }

    /**
     * A custom {@link ClickablePreferenceCategory} for displaying group titles in search results.
     * It adds styling to make the titles distinct, such as italic font and reduced opacity.
     */
    private static class SearchResultCategory extends ClickablePreferenceCategory {

        public SearchResultCategory(ReVancedPreferenceFragment fragment, PreferenceScreen screen) {
            super(fragment, screen);
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                // Make text bold and italic.
                titleView.setTypeface(titleView.getTypeface(), Typeface.ITALIC);

                // Set opacity to 80% to make it distinguishable.
                titleView.setAlpha(0.7f);
            }
        }
    }

    /**
     * Encapsulates information about a field to be highlighted, including the text content,
     * the search query, and a setter to apply the highlighted or original text.
     *
     * @param text   The text content of the field (e.g., title, summary).
     * @param setter A consumer that applies the highlighted or original text to the field.
     */
    private record FieldHighlightInfo(CharSequence text, Consumer<CharSequence> setter) {}

    /**
     * A helper class to store metadata about a {@link Preference}, including its original text fields
     * and highlighting state. This class is used to manage the state of a preference during search
     * highlighting, preserving original values for restoration and tracking whether highlighting
     * has been applied.
     */
    private static class PreferenceInfo {
        /**
         * The preference associated with this info.
         */
        final Preference preference;
        /**
         * The key of the preference, used for identification.
         */
        final String key;
        /**
         * The title of the preference's group, or an empty string if none.
         */
        final String groupTitle;
        /**
         * A unique cache key combining group title and preference key.
         */
        final String cacheKey;
        /**
         * The original title of the preference, possibly null.
         */
        @Nullable
        CharSequence originalTitle;
        /**
         * The original summary of the preference, possibly null.
         */
        @Nullable
        CharSequence originalSummary;
        /**
         * The original summaryOn text for a {@link SwitchPreference}, possibly null.
         */
        @Nullable
        CharSequence originalSummaryOn;
        /**
         * The original summaryOff text for a {@link SwitchPreference}, possibly null.
         */
        @Nullable
        CharSequence originalSummaryOff;
        /**
         * The original entries for a {@link ListPreference}, possibly null.
         */
        @Nullable
        CharSequence[] originalEntries;
        /**
         * Tracks whether highlighting has been applied to this preference.
         */
        boolean highlightingApplied;

        /**
         * Constructs a {@link PreferenceInfo} for the given preference, capturing its original state.
         * The original title, summary, and, for specific preference types like {@link SwitchPreference}
         * or {@link ListPreference}, additional fields like summaryOn/summaryOff or entries are stored.
         * A copy of the entries array is made to prevent unintended modifications.
         *
         * @param preference The preference to store information for.
         * @param groupTitle The title of the preference's group, or null if none.
         */
        PreferenceInfo(Preference preference, String groupTitle) {
            this.preference = preference;
            this.key = preference.getKey();
            this.groupTitle = groupTitle != null ? groupTitle : "";
            this.cacheKey = this.groupTitle + ":" + this.key;

            // Store original state AS-IS, without stripping spans.
            this.originalTitle = preference.getTitle();
            this.originalSummary = preference.getSummary();

            if (preference instanceof SwitchPreference switchPref) {
                this.originalSummaryOn = switchPref.getSummaryOn();
                this.originalSummaryOff = switchPref.getSummaryOff();
            }
            if (preference instanceof ListPreference listPref) {
                // It's good practice to copy arrays if they might be modified elsewhere,
                // or if the highlighting modifies entries in-place (which highlightText shouldn't).
                // CharSequence elements themselves are often immutable (like String) or new
                // SpannableStringBuilders are created by highlightText.
                this.originalEntries = listPref.getEntries() != null ? Arrays.copyOf(listPref.getEntries(), listPref.getEntries().length) : null;
            }
            this.highlightingApplied = false;
        }
    }

    /**
     * A custom {@link Preference} representing a single search history entry.
     * Displays the search query as the title, an icon, and a close button to delete the entry.
     * Clicking the preference re-runs the search query, while clicking the close button triggers
     * a confirmation dialog to remove the entry from history.
     */
    private static class HistoryPreference extends Preference {
        /**
         * The search query associated with this history entry.
         */
        private final String query;
        /**
         * The action to run when the close button is clicked, typically showing a deletion dialog.
         */
        private final Consumer<String> onDeleteRequest;

        /**
         * Constructs a {@link HistoryPreference} for a search query.
         * The preference is initialized with the query as its title and a key based on the query.
         * No default widget is used, and the close button's action is set via the provided runnable.
         *
         * @param context         The context used to initialize the preference.
         * @param query           The search query for this history entry.
         * @param onDeleteRequest The action to perform when the close button is clicked.
         */
        HistoryPreference(Context context, String query, Consumer<String> onDeleteRequest) {
            super(context);
            this.query = query;
            this.onDeleteRequest = onDeleteRequest;
            setKey("history_" + query);
            setTitle(query);
            setWidgetLayoutResource(0); // No default widget
        }

        /**
         * Binds the preference's view, customizing its appearance to include an icon, the search query
         * as the title, and a close button. The default widget frame is cleared, and a custom
         * {@link LinearLayout} is used to arrange the components. The layout is left-to-right, and
         * the title, icon, and close button are styled for consistency with the application theme.
         *
         * @param view The view to bind to the preference.
         */
        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            ViewGroup container = (ViewGroup) view;
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            // layout.setPadding(dipToPixels(4), dipToPixels(4), dipToPixels(4), dipToPixels(4));
            layout.setGravity(Gravity.CENTER_VERTICAL);

            // Clear existing widget frame to avoid overlap
            LinearLayout widgetFrame = container.findViewById(android.R.id.widget_frame);
            if (widgetFrame != null) {
                widgetFrame.removeAllViews();
                widgetFrame.setVisibility(View.GONE);
            }

            // Icon (yt_outline_arrow_time_vd_theme_24)
            ImageView iconView = new ImageView(getContext());
            Drawable iconDrawable = ResourceUtils.getDrawable("yt_outline_arrow_time_vd_theme_24");
            if (iconDrawable != null) {
                iconDrawable.setTint(ThemeUtils.getAppForegroundColor());
                iconView.setImageDrawable(iconDrawable);
                int iconSize = dipToPixels(24);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.setMargins(0, 0, dipToPixels(8), 0); // Margin between icon and title
                iconView.setLayoutParams(iconParams);
                layout.addView(iconView);
            } else {
                // Fallback to a generic drawable with tint
                iconView.setImageResource(android.R.drawable.ic_menu_recent_history);
                iconView.setColorFilter(ThemeUtils.getAppForegroundColor());
                Logger.printException(() -> "Failed to load drawable: yt_outline_arrow_time_vd_theme_24, using fallback");
            }

            // Title
            TextView titleView = new TextView(getContext());
            titleView.setText(query);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleView.setTextColor(ThemeUtils.getAppForegroundColor());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            layout.addView(titleView, titleParams);

            // Close button
            ImageView closeButton = new ImageView(getContext());
            closeButton.setImageDrawable(ThemeUtils.getTrashButtonDrawable());
            closeButton.setContentDescription(str("revanced_search_delete_entry"));
            int size = dipToPixels(24);
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(size, size);
            closeButton.setLayoutParams(closeParams);
            closeButton.setOnClickListener(v -> {
                if (onDeleteRequest != null) {
                    onDeleteRequest.accept(query);
                }
            });
            layout.addView(closeButton);

            // Add layout to container
            container.removeAllViews();
            container.addView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    /**
     * A custom {@link Preference} displayed when no search results or history entries are found.
     * Shows a title and summary indicating no results, with centered text and no selectable behavior.
     */
    private static class NoResultsPreference extends Preference {

        /**
         * Constructs a {@link NoResultsPreference}.
         *
         * @param context                  The context used to initialize the preference.
         * @param query                    The search query that yielded no results. Used if not in "empty history" mode.
         * @param isForEmptyHistoryContext True if this preference should display "no history" messages,
         *                                 false if it should display "no results for query" messages.
         */
        NoResultsPreference(Context context, String query, boolean isForEmptyHistoryContext) {
            super(context);
            if (isForEmptyHistoryContext) {
                setKey("no_history");
                setTitle(str("revanced_search_no_history_title"));
                setSummary(str("revanced_search_no_history_summary"));
            } else {
                setKey("no_results_" + query);
                setTitle(str("revanced_search_settings_no_results_title", query));
                setSummary(str("revanced_search_settings_no_results_summary"));
            }
            setSelectable(false);
        }

        /**
         * Binds the preference's view, centering the title and summary text horizontally.
         * The title and summary views are adjusted to span the full width of the preference.
         *
         * @param view The view to bind to the preference.
         */
        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = titleView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                titleView.setLayoutParams(params);
            }

            TextView summaryView = view.findViewById(android.R.id.summary);
            if (summaryView != null) {
                summaryView.setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = summaryView.getLayoutParams();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                summaryView.setLayoutParams(params);
            }
        }
    }
}
