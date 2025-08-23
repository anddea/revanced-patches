package app.revanced.extension.youtube.settings;

import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.preference.ReVancedPreferenceFragment;
import app.revanced.extension.youtube.utils.ThemeUtils;

@SuppressWarnings("deprecation")
public class SearchViewController {
    private final Activity activity;
    private final ReVancedPreferenceFragment fragment;
    private final SearchView searchView;

    // region Search-related fields
    private static final String PREFERENCE_SCREEN_TAG = "PreferenceScreen";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String DEPENDENCY_ATTRIBUTE = "searchDependency";
    private static final String TITLE_ATTRIBUTE = "title";
    private static final String SEARCH_HISTORY_DELIMITER = "|";
    private static final int MAX_SEARCH_HISTORY_SIZE = 50;
    private final Map<String, List<Preference>> dependencyMap = new HashMap<>();
    private final Map<PreferenceGroup, LinkedHashSet<Preference>> groupedPreferences = new LinkedHashMap<>();
    private final Map<String, String> customDependencyMap = new HashMap<>();
    private final Map<String, HistoryPreference> historyViewPreferenceCache = new HashMap<>();
    private final Map<String, Preference> allPreferencesByKey = new HashMap<>();
    private final Map<PreferenceGroup, String> groupFullPaths = new HashMap<>();
    private final Map<Preference, PreferenceGroup> preferenceToParentGroupMap = new HashMap<>();
    private final Map<Preference, ReVancedPreferenceFragment.PreferenceInfo> preferenceInfoMap = new HashMap<>();
    private List<String> searchHistoryCache = null;
    // endregion

    public SearchViewController(Activity activity, ReVancedPreferenceFragment fragment) {
        this.activity = activity;
        this.fragment = fragment;
        this.searchView = activity.findViewById(ResourceUtils.getIdIdentifier("search_view"));

        // Initialize and setup all search functionalities
        initializeCustomDependencies();
        storeAllPreferences(fragment.getPreferenceScreen());
        setupSearchView();
    }

    public String getCurrentQuery() {
        return searchView != null ? searchView.getQuery().toString() : "";
    }

    /**
     * Configures the SearchView for the LicenseActivity.
     * <p>
     * This method sets up the {@link SearchView} by applying a query hint, adjusting font size via reflection,
     * setting margins, applying a themed background, and configuring query text and focus listeners. The listeners
     * handle search queries, filter preferences, and manage search history. The keyboard is hidden after query submission.
     * If the SearchView or required resources are not found, an error is logged.
     */
    private void setupSearchView() {
        if (searchView == null) {
            Logger.printException(() -> "SearchView not found in layout");
            return;
        }

        // Set query hint
        String finalSearchHint = String.format(fragment.searchLabel, fragment.rvxSettingsLabel);
        searchView.setQueryHint(finalSearchHint);

        // Set font size via reflection
        try {
            Field field = searchView.getClass().getDeclaredField("mSearchSrcTextView");
            field.setAccessible(true);
            if (field.get(searchView) instanceof EditText searchEditText) {
                searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            Logger.printException(() -> "Reflection error accessing mSearchSrcTextView", ex);
        }

        // Set SearchView dimensions
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) searchView.getLayoutParams();
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, activity.getResources().getDisplayMetrics());
        layoutParams.setMargins(margin, layoutParams.topMargin, margin, layoutParams.bottomMargin);
        searchView.setLayoutParams(layoutParams);

        // Set SearchView color
        searchView.setBackground(ThemeUtils.getSearchViewShape());

        // Set query text listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Logger.printDebug(() -> "onQueryTextSubmit called with: " + query);
                String queryTrimmed = query.trim();
                if (!queryTrimmed.isEmpty()) {
                    fragment.setPreferenceScreen(fragment.rootPreferenceScreen);
                    filterPreferences(queryTrimmed);
                    addToSearchHistory(queryTrimmed);
                    Logger.printDebug(() -> "Added to search history: " + queryTrimmed);
                }

                // Hide keyboard and remove focus
                searchView.clearFocus();
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                fragment.setPreferenceScreen(fragment.rootPreferenceScreen);
                filterPreferences(newText);
                return true;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((view, hasFocus) -> {
            Logger.printDebug(() -> "SearchView focus changed: " + hasFocus);
            if (hasFocus) {
                filterPreferences(""); // Show history when focused
            }
        });
    }

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

        try (XmlResourceParser parser = activity.getResources().getXml(xmlId)) {
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

        groupFullPaths.put(group, fragment.getFullPath(group));
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
        PreferenceScreen screen = fragment.getPreferenceScreen();
        if (screen == null) return;
        screen.removeAll();

        // Clear all previous highlights and reset highlighting state
        for (ReVancedPreferenceFragment.PreferenceInfo info : preferenceInfoMap.values()) clearHighlighting(info);

        if (query.isEmpty() && Settings.SETTINGS_SEARCH_HISTORY.get()) {
            displaySearchHistory(screen);
        } else if (query.isEmpty() || groupedPreferences.isEmpty()) {
            fragment.resetPreferences();
        } else {
            filterAndDisplayPreferences(screen, query.toLowerCase());
        }
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
                PreferenceGroup displayGroupForMatchedItems = xmlParentOfMatchedItem == fragment.rootPreferenceScreen
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
        ClickablePreferenceCategory category = new ClickablePreferenceCategory(fragment, fragment.findClosestPreferenceScreen(group));
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
            // Handle keyless preference (e.g., <Preference title="foo"/>)
            // These don't have dependencies in the same way keyed items do.
            // Just add it to the search results under the appropriate preferenceGroup.
            // Ensure it's not a duplicate instance in that specific preferenceGroup.
            boolean alreadyAdded = false;
            for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                if (preferenceGroup.getPreference(i) == preference) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                try {
                    preferenceGroup.addPreference(preference);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to add keyless matched item: " + (preference.getTitle() != null ? preference.getTitle() : "Untitled"), e);
                }
            }
            return;
        }

        // Handle keyed preference
        if (visitedPreferences.contains(preferenceKey)) {
            // This item's dependency chain has been (or is being) processed.
            // However, it might need to be visually added to *this specific* preferenceGroup
            // if it wasn't already (e.g., if it's a dependency reached via another path).
            boolean alreadyInThisGroup = false;
            for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                Preference p = preferenceGroup.getPreference(i);
                if (p == preference || (p.getKey() != null && p.getKey().equals(preferenceKey))) {
                    alreadyInThisGroup = true;
                    break;
                }
            }
            if (!alreadyInThisGroup) {
                try {
                    preferenceGroup.addPreference(preference);
                } catch (Exception e) {
                    Logger.printException(() -> "Failed to re-add keyed item to a different group: " + preferenceKey, e);
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
            try {
                preferenceGroup.addPreference(preference);
            } catch (Exception e) {
                Logger.printException(() -> "Failed to add keyed item: " + preferenceKey, e);
                visitedPreferences.remove(preferenceKey);
                return;
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

        // if (group == fragment.rootPreferenceScreen || group == null) {
        //     if (categoryTitle.isEmpty()) {
        //         String directKeyLookup = customDependencyMap.get(preference.getKey());
        //         if (directKeyLookup != null) return directKeyLookup;
        //     }
        // }

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

    // endregion [Search] Filter Preferences

    // region [Search] Highlight Filtered Preferences

    /**
     * Applies text highlighting to a preference's fields (title, summary, etc.) based on a search query.
     * Highlighting is applied only if the {@link Settings#SETTINGS_SEARCH_HIGHLIGHT} setting is enabled.
     * The method processes the preference's title, summary, and, for specific preference types like
     * {@link SwitchPreference} or {@link ListPreference}, additional fields like summaryOn/summaryOff or entries.
     * <p>
     *
     * @param preference The preference to apply highlighting to.
     * @param lowerQuery The lowercase search query string for matching and highlighting.
     */
    private void applyHighlighting(Preference preference, String lowerQuery) {
        ReVancedPreferenceFragment.PreferenceInfo info = findOrCreatePreferenceInfo(preference);
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
     * The original entries are preserved in the {@link ReVancedPreferenceFragment.PreferenceInfo}, and highlighted versions
     * are applied to the preference if a match is found.
     *
     * @param lp              The {@link ListPreference} whose entries are to be highlighted.
     * @param originalEntries The original entries of the preference, stored in {@link ReVancedPreferenceFragment.PreferenceInfo}.
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
     * The original values are retrieved from the {@link ReVancedPreferenceFragment.PreferenceInfo} and applied to the
     * preference's title, summary, and, for specific types like {@link SwitchPreference} or
     * {@link ListPreference}, additional fields like summaryOn/summaryOff or entries.
     *
     * @param info The {@link ReVancedPreferenceFragment.PreferenceInfo} containing the preference and its original values.
     */
    private void clearHighlighting(ReVancedPreferenceFragment.PreferenceInfo info) {
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
     * Retrieves or creates a {@link ReVancedPreferenceFragment.PreferenceInfo} object for a given preference.
     * If a matching {@link ReVancedPreferenceFragment.PreferenceInfo} exists in the {@code preferenceList}, it is returned.
     * Otherwise, a new {@link ReVancedPreferenceFragment.PreferenceInfo} is created, added to the list, and returned.
     *
     * @param preference The preference for which to find or create a {@link ReVancedPreferenceFragment.PreferenceInfo}.
     * @return The existing or newly created {@link ReVancedPreferenceFragment.PreferenceInfo} for the preference.
     */
    private ReVancedPreferenceFragment.PreferenceInfo findOrCreatePreferenceInfo(Preference preference) {
        return preferenceInfoMap.computeIfAbsent(preference, p -> new ReVancedPreferenceFragment.PreferenceInfo(p, getGroupTitle(p)));
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
        ReVancedSettingsHostActivity.updateToolbarTitle(str("revanced_search_title"));
        List<String> history = loadSearchHistory();
        screen.removeAll();

        if (history.isEmpty()) {
            screen.addPreference(new NoResultsPreference(activity, "", true));
        } else {
            PreferenceCategory historyCategory = new PreferenceCategory(activity);
            historyCategory.setTitle(str("revanced_search_history_title"));
            screen.addPreference(historyCategory);

            for (String searchQuery : history) {
                historyCategory.addPreference(getHistoryPreference(searchQuery));
            }

            Preference clearPref = new Preference(activity);
            clearPref.setKey("clear_history");
            clearPref.setTitle(str("revanced_search_clear_history_title"));
            clearPref.setOnPreferenceClickListener(p -> {
                new AlertDialog.Builder(activity)
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
        HistoryPreference historyPref = new HistoryPreference(activity, searchQuery, this::showDeleteHistoryDialog);
        historyPref.setOnPreferenceClickListener(p -> {
            if (searchView != null) searchView.setQuery(searchQuery, true);
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
        new AlertDialog.Builder(activity)
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

    /**
     * Clears all cached collections to release memory and prevent potential leaks.
     * This should be called when the controller is no longer needed, typically in onDestroy.
     */
    public void destroy() {
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
        searchHistoryCache = null;
        historyViewPreferenceCache.clear();
    }

    // endregion [Search] Search History

    // endregion Search

    // region Inner Classes for Search
    private record FieldHighlightInfo(CharSequence text, Consumer<CharSequence> setter) {
    }

    private static class HistoryPreference extends Preference {
        private final String query;
        private final Consumer<String> onDeleteRequest;

        HistoryPreference(Context context, String query, Consumer<String> onDeleteRequest) {
            super(context);
            this.query = query;
            this.onDeleteRequest = onDeleteRequest;
            setKey("history_" + query);
            setTitle(query);
            setWidgetLayoutResource(0); // No default widget
        }

        private int dipToPixels(float dip) {
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getContext().getResources().getDisplayMetrics());
        }

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);
            ViewGroup container = (ViewGroup) view;
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout widgetFrame = container.findViewById(android.R.id.widget_frame);
            if (widgetFrame != null) {
                widgetFrame.removeAllViews();
                widgetFrame.setVisibility(View.GONE);
            }

            ImageView iconView = new ImageView(getContext());
            Drawable iconDrawable = ResourceUtils.getDrawable("yt_outline_arrow_time_vd_theme_24");
            if (iconDrawable != null) {
                iconDrawable.setTint(ThemeUtils.getAppForegroundColor());
                iconView.setImageDrawable(iconDrawable);
                int iconSize = dipToPixels(24);
                LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconParams.setMargins(0, 0, dipToPixels(8), 0);
                iconView.setLayoutParams(iconParams);
                layout.addView(iconView);
            }

            TextView titleView = new TextView(getContext());
            titleView.setText(query);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleView.setTextColor(ThemeUtils.getAppForegroundColor());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            layout.addView(titleView, titleParams);

            ImageView closeButton = new ImageView(getContext());
            closeButton.setImageDrawable(ThemeUtils.getTrashButtonDrawable());
            closeButton.setContentDescription(str("revanced_search_delete_entry"));
            int size = dipToPixels(24);
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(size, size);
            closeButton.setLayoutParams(closeParams);
            closeButton.setOnClickListener(v -> {
                if (onDeleteRequest != null) onDeleteRequest.accept(query);
            });
            layout.addView(closeButton);

            container.removeAllViews();
            container.addView(layout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private static class NoResultsPreference extends Preference {
        NoResultsPreference(Context context, String query, boolean isForEmptyHistory) {
            super(context);
            if (isForEmptyHistory) {
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

        @Override
        protected void onBindView(View view) {
            super.onBindView(view);

            TextView titleView = view.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = titleView.getLayoutParams();
                if (params != null) {
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    titleView.setLayoutParams(params);
                }
            }

            TextView summaryView = view.findViewById(android.R.id.summary);
            if (summaryView != null) {
                summaryView.setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = summaryView.getLayoutParams();
                if (params != null) {
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    summaryView.setLayoutParams(params);
                }
            }
        }
    }
    // endregion
}
