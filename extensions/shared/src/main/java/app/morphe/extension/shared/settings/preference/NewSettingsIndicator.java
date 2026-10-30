/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/4162
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.settings.preference;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.os.Build;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import androidx.annotation.ColorInt;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;

/**
 * Shows a dot next to settings and list entries that are new to the user,
 * for the number of days set with {@link SharedYouTubeSettings#SETTINGS_NEW_INDICATOR_DAYS}.
 * <p>
 * Indicators fade when their rows become visible and are permanently acknowledged.
 * Unviewed settings remain highlighted until the configured age limit.
 * A setting is new from the first time its key is in the settings menu.
 * The first time this runs, all current keys are saved as already known,
 * so nothing is new after a fresh install or after updating to the first version with this feature.
 */
@SuppressWarnings("deprecation")
public final class NewSettingsIndicator {

    /**
     * Twitter blue.
     */
    @ColorInt
    private static final int DOT_COLOR = 0xFF1DA1F2;

    // Give users time to notice the highlight before it starts fading.
    private static final long FADE_DELAY_MILLIS = 2000;
    private static final long FADE_DURATION_MILLIS = 600;

    private static final SharedPreferences preferences =
            new SharedPrefCategory("morphe_new_settings").preferences;

    /**
     * All preference keys and list entries that were seen in the settings menu.
     */
    private static final String KNOWN_IDS_KEY = "known_ids";

    /**
     * Prefix of the keys that store the time an id was first seen.
     * Only ids first seen within the max days of the seek bar are stored.
     */
    private static final String FIRST_SEEN_KEY_PREFIX = "first_seen:";

    // Row indicators and option indicators are acknowledged independently.
    private static final String VIEWED_TITLE_PREFIX = "viewed_title:";

    /**
     * Separates the preference key and the entry value in the id of a list entry.
     */
    private static final String LIST_ENTRY_SEPARATOR = "|";

    private static final Map<Preference, Set<String>> pending = new WeakHashMap<>();
    private static final Set<ListView> watched =
            Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Watches laid out rows, including rows revealed by scrolling. Screen links are not
     * acknowledged until their contents are actually visible. Known ids are retained so
     * acknowledged indicators cannot return after a restart or a longer highlight duration.
     */
    public static void watchVisiblePreferences(ListView listView) {
        if (!watched.add(listView)) return;
        redrawOnWindowFocus(listView);
        listView.getViewTreeObserver().addOnPreDrawListener(() -> {
            // Covered parent windows wait until the user returns before fading.
            if (!listView.hasWindowFocus()) return true;
            for (int i = 0; i < listView.getChildCount(); i++) {
                View row = listView.getChildAt(i);
                if (!row.getGlobalVisibleRect(new Rect())) continue;
                int position = listView.getFirstVisiblePosition() + i;
                Object item = listView.getItemAtPosition(position);
                if (item instanceof Preference pref) {
                    Set<String> ids = pending.get(pref);
                    if (ids == null) continue;
                    if (pref instanceof PreferenceGroup) {
                        // A parent may be hidden by its child dialog. Start its fade when
                        // the row is drawn again, so returning users can see the animation.
                        if (ids.isEmpty()) {
                            pending.remove(pref);
                            fadeText(pref.getTitle(), row, 0);
                        }
                        continue;
                    }
                    if (ids.isEmpty()) continue;
                    SharedPreferences.Editor editor = preferences.edit();
                    boolean unseenTitle = false;
                    for (String id : ids) {
                        if (!preferences.getBoolean(VIEWED_TITLE_PREFIX + id, false)) {
                            editor.putBoolean(VIEWED_TITLE_PREFIX + id, true);
                            unseenTitle = true;
                        }
                    }
                    if (unseenTitle) {
                        editor.apply();
                        fadeTitle(pref, row);
                    }
                    if (ids.remove(pref.getKey())) acknowledge(pref.getKey());
                }
            }
            return true;
        });
    }

    /**
     * The first pre-draw can happen before the dialog gains focus. Request another
     * frame on focus so visible rows are checked without waiting for touch input.
     * This also starts completed parent fades when a child dialog is dismissed.
     */
    private static void redrawOnWindowFocus(ListView listView) {
        listView.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
            if (hasFocus) listView.postInvalidateOnAnimation();
        });
        listView.postInvalidateOnAnimation();
    }

    private static void acknowledge(String id) {
        preferences.edit().remove(FIRST_SEEN_KEY_PREFIX + id).apply();
        for (Map.Entry<Preference, Set<String>> entry : pending.entrySet()) {
            if (entry.getKey() instanceof PreferenceGroup) {
                entry.getValue().remove(id);
            }
        }
    }

    private static void fadeTitle(Preference pref, View row) {
        fadeText(pref.getTitle(), row);
    }

    /** Animate shared spans so rebinding a row cannot bring its dot back. */
    private static void fadeText(CharSequence text, View view) {
        fadeText(text, view, FADE_DELAY_MILLIS);
    }

    /** Parent dots fade immediately on return. Leaf dots retain their reading delay. */
    private static void fadeText(CharSequence text, View view, long delayMillis) {
        if (!(text instanceof Spanned spanned)) return;
        for (NewIndicatorSpan span : spanned.getSpans(0, text.length(), NewIndicatorSpan.class)) {
            ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
            animator.setStartDelay(delayMillis);
            animator.setDuration(FADE_DURATION_MILLIS);
            animator.addUpdateListener(value -> {
                span.setOpacity((float) value.getAnimatedValue());
                invalidateText(view);
            });
            animator.start();
        }
    }

    private static void invalidateText(View view) {
        view.invalidate();
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) invalidateText(group.getChildAt(i));
        }
    }

    /** Acknowledges only list options actually displayed in the dialog. */
    static void watchVisibleEntries(ListView listView, CustomDialogListPreference pref,
                                    CharSequence[] entries, CharSequence[] values) {
        listView.getViewTreeObserver().addOnPreDrawListener(() -> {
            Set<String> ids = pending.get(pref);
            if (ids == null) return true;
            for (int i = 0; i < listView.getChildCount(); i++) {
                View row = listView.getChildAt(i);
                int position = listView.getFirstVisiblePosition() + i;
                if (!row.getGlobalVisibleRect(new Rect())) continue;
                String id = listEntryId(pref.getKey(), values[position]);
                if (ids.remove(id)) {
                    acknowledge(id);
                    pref.removeNewEntryValue(values[position].toString());
                    fadeText(entries[position], row);
                    if (ids.isEmpty()) fadeTitle(pref, listView);
                }
            }
            return true;
        });
    }

    private static String listEntryId(String key, CharSequence entryValue) {
        return key + LIST_ENTRY_SEPARATOR + entryValue;
    }

    /**
     * Saves the keys that are seen for the first time, and adds a dot to new preferences,
     * list entries and the preference screens that contain them.
     * Must be called after all preferences are added to and removed from the screen.
     */
    public static void markNewPreferences(PreferenceScreen screen) {
        try {
            Set<String> currentIds = new HashSet<>();
            collectIds(screen, currentIds);

            Map<String, Long> firstSeenTimes = updateFirstSeenTimes(currentIds);

            final int days = SeekBarPreference.clampToRange(SharedYouTubeSettings.SETTINGS_NEW_INDICATOR_DAYS);
            if (days == 0 || firstSeenTimes.isEmpty()) {
                return;
            }

            final long newAfter = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
            Set<String> newIds = new HashSet<>();
            for (Map.Entry<String, Long> entry : firstSeenTimes.entrySet()) {
                if (entry.getValue() > newAfter) {
                    newIds.add(entry.getKey());
                }
            }
            if (newIds.isEmpty()) {
                return;
            }

            Logger.printDebug(() -> "New settings: " + newIds);
            markGroup(screen, newIds);
        } catch (Exception ex) {
            Logger.printException(() -> "markNewPreferences failure", ex);
        }
    }

    private static void collectIds(PreferenceGroup group, Set<String> ids) {
        for (int i = 0, count = group.getPreferenceCount(); i < count; i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof PreferenceGroup subGroup) {
                // Group keys include the sorting and are not tracked.
                // A group is new if anything inside it is new.
                collectIds(subGroup, ids);
                continue;
            }

            String key = pref.getKey();
            if (key == null) continue;
            ids.add(key);

            if (pref instanceof CustomDialogListPreference listPref && listPref.tracksNewEntries()) {
                CharSequence[] entryValues = listPref.getEntryValues();
                if (entryValues != null) {
                    for (CharSequence entryValue : entryValues) {
                        ids.add(listEntryId(key, entryValue));
                    }
                }
            }
        }
    }

    /**
     * Saves ids seen for the first time, and removes first seen times older than the max days.
     *
     * @return The first seen times of the ids added within the max days.
     */
    private static Map<String, Long> updateFirstSeenTimes(Set<String> currentIds) {
        if (!preferences.contains(KNOWN_IDS_KEY)) {
            Logger.printDebug(() -> "Saving " + currentIds.size() + " known settings");
            preferences.edit().putStringSet(KNOWN_IDS_KEY, currentIds).apply();
            return Collections.emptyMap();
        }

        Set<String> knownIds = new HashSet<>(preferences.getStringSet(KNOWN_IDS_KEY, Collections.emptySet()));
        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;
        final long now = System.currentTimeMillis();
        Map<String, Long> firstSeenTimes = new HashMap<>();
        for (String id : currentIds) {
            if (knownIds.add(id)) {
                editor.putLong(FIRST_SEEN_KEY_PREFIX + id, now);
                firstSeenTimes.put(id, now);
                changed = true;
            }
        }
        if (changed) {
            editor.putStringSet(KNOWN_IDS_KEY, knownIds);
        }

        SeekBarPreference.SeekBarConfig config =
                SeekBarPreference.configFor(SharedYouTubeSettings.SETTINGS_NEW_INDICATOR_DAYS);
        final long expiredBefore = now - TimeUnit.DAYS.toMillis(config.max());
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(FIRST_SEEN_KEY_PREFIX)) continue;

            final long firstSeen = (Long) entry.getValue();
            if (firstSeen < expiredBefore) {
                editor.remove(key);
                changed = true;
            } else {
                firstSeenTimes.put(key.substring(FIRST_SEEN_KEY_PREFIX.length()), firstSeen);
            }
        }

        if (changed) {
            editor.apply();
        }
        return firstSeenTimes;
    }

    /**
     * @return If the group contains a new preference or list entry.
     */
    private static boolean markGroup(PreferenceGroup group, Set<String> newIds) {
        boolean groupHasNew = false;

        for (int i = 0, count = group.getPreferenceCount(); i < count; i++) {
            Preference pref = group.getPreference(i);
            final boolean isNew;

            if (pref instanceof PreferenceGroup subGroup) {
                isNew = markGroup(subGroup, newIds);
            } else {
                String key = pref.getKey();
                if (key == null) continue;

                boolean hasNewEntry = false;
                if (pref instanceof CustomDialogListPreference listPref && listPref.tracksNewEntries()) {
                    CharSequence[] entryValues = listPref.getEntryValues();
                    if (entryValues != null) {
                        Set<String> newEntryValues = new HashSet<>();
                        for (CharSequence entryValue : entryValues) {
                            if (newIds.contains(listEntryId(key, entryValue))) {
                                newEntryValues.add(entryValue.toString());
                            }
                        }
                        if (!newEntryValues.isEmpty()) {
                            listPref.setNewEntryValues(newEntryValues, DOT_COLOR);
                            hasNewEntry = true;
                        }
                    }
                }

                Set<String> ids = new HashSet<>();
                if (newIds.contains(key)) ids.add(key);
                if (hasNewEntry && pref instanceof CustomDialogListPreference listPref) {
                    for (CharSequence value : listPref.getEntryValues()) {
                        String id = listEntryId(key, value);
                        if (newIds.contains(id)) ids.add(id);
                    }
                }
                if (!ids.isEmpty()) pending.put(pref, ids);
                isNew = !ids.isEmpty();
            }

            // Categories are not marked, since the new preference is shown right below them.
            final boolean canMark = pref instanceof PreferenceScreen
                    ? canMarkScreenTitle()
                    : !(pref instanceof PreferenceGroup);
            boolean unseenTitle = pref instanceof PreferenceGroup;
            Set<String> titleIds = pending.get(pref);
            if (titleIds != null) {
                for (String id : titleIds) {
                    unseenTitle |= !preferences.getBoolean(VIEWED_TITLE_PREFIX + id, false);
                }
            }
            if (isNew && canMark && unseenTitle) {
                pref.setTitle(NewIndicatorSpan.addTo(pref.getTitle(), DOT_COLOR));
            }
            groupHasNew |= isNew;
        }

        if (groupHasNew) {
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Set<String> childIds = pending.get(group.getPreference(i));
                if (childIds != null) ids.addAll(childIds);
            }
            pending.put(group, ids);
        }
        return groupHasNew;
    }

    /**
     * A preference screen opens as a dialog that uses the screen title as its window title.
     * Android 12 and 12L crash when logging a canceled input event of a window
     * if the title is not a String, so the title of a screen cannot have a span.
     */
    private static boolean canMarkScreenTitle() {
        return Build.VERSION.SDK_INT != Build.VERSION_CODES.S
                && Build.VERSION.SDK_INT != Build.VERSION_CODES.S_V2;
    }
}
