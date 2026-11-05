/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
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

package app.morphe.extension.youtube.settings.preference;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import app.morphe.extension.shared.settings.BooleanSetting;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.AbstractPreferenceFragment;
import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.PackageUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

/**
 * Displays the known YouTube navigation items in a drag-and-drop order and visibility editor.
 * Modified by Codex, 2026-11-03: stage visibility changes alongside the order.
 *
 * <p>The setting is saved only when the dialog's OK button is pressed. Optional items are shown
 * as well, so one saved order can be reused when YouTube changes the active layout.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public class NavigationBarOrderPreference extends Preference {

    private static final boolean SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER = PackageUtils.isVersionOrGreater("20.05.00");
    private static final List<NavigationButton> DEFAULT_ORDER = getEditorButtons();

    /** Older targets support the native hide flags, but not extra renderers or reordering. */
    private static List<NavigationButton> getEditorButtons() {
        List<NavigationButton> buttons = new ArrayList<>(NavigationBar.getDefaultNavigationButtonOrder());
        if (!SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER) {
            buttons.remove(NavigationButton.SEARCH);
            buttons.remove(NavigationButton.SETTINGS);
            buttons.remove(NavigationButton.WATCH_LATER);
        }
        return List.copyOf(buttons);
    }

    public NavigationBarOrderPreference(Context context, AttributeSet attrs,
                                        int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public NavigationBarOrderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public NavigationBarOrderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NavigationBarOrderPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        setSelectable(true);
        setPersistent(false);
        updateSummary();
    }

    /** Updates the summary after an import or a change made by another preference instance. */
    public void updateSummary() {
        List<NavigationButton> order = getCurrentOrder();
        StringBuilder summary = new StringBuilder();
        PendingVisibility visibility = new PendingVisibility();
        for (NavigationButton button : order) {
            if (!visibility.isEnabled(button)) continue;
            if (summary.length() > 0) {
                summary.append(SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER ? " → " : ", ");
            }
            summary.append(getLabel(button));
        }
        setSummary(summary.toString());
    }

    @Override
    protected void onClick() {
        showOrderDialog();
    }

    private void showOrderDialog() {
        Context context = getContext();
        List<NavigationButton> pendingOrder = getCurrentOrder();
        PendingVisibility pendingVisibility = new PendingVisibility();
        NavigationButtonAdapter adapter = new NavigationButtonAdapter(context, pendingOrder, pendingVisibility);

        ListView listView = getListView(context, adapter);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView instructions = new TextView(context);
        instructions.setText(str(SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER
                ? "revanced_navigation_bar_order_instructions"
                : "revanced_navigation_bar_visibility_instructions"));
        instructions.setTextColor(BaseThemeUtils.getAppForegroundColor());
        instructions.setPadding(Utils.dipToPixels(8), 0, Utils.dipToPixels(8), Utils.dipToPixels(12));
        content.addView(instructions);
        content.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Utils.dipToPixels(440)));

        Pair<android.app.Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                getTitle() != null ? getTitle().toString() : "",
                null,
                null,
                null,
                () -> {
                    boolean changed = pendingVisibility.save();
                    String order = SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER
                            ? serializeOrder(pendingOrder) : Settings.NAVIGATION_BAR_ORDER.get();
                    changed |= !order.equals(Settings.NAVIGATION_BAR_ORDER.get());
                    Settings.NAVIGATION_BAR_ORDER.save(order);
                    if (changed) AbstractPreferenceFragment.setRestartNeeded();
                    updateSummary();
                },
                null,
                str("revanced_settings_reset"),
                () -> {
                    pendingOrder.clear();
                    pendingOrder.addAll(DEFAULT_ORDER);
                    pendingVisibility.reset();
                    adapter.notifyDataSetChanged();
                },
                false
        );

        LinearLayout mainLayout = dialogPair.second;
        mainLayout.addView(content, mainLayout.getChildCount() - 1,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
        dialogPair.first.show();
    }

    @NonNull
    private static ListView getListView(Context context, NavigationButtonAdapter adapter) {
        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        if (!SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER) return listView;
        listView.setOnDragListener((view, event) -> {
            switch (event.getAction()) {
                case android.view.DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription() != null
                            && event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                case android.view.DragEvent.ACTION_DRAG_LOCATION:
                    Object state = event.getLocalState();
                    int targetPosition = listView.pointToPosition(0, (int) event.getY());
                    if (state instanceof NavigationButton button
                            && targetPosition != AdapterView.INVALID_POSITION) {
                        adapter.moveButton(button, targetPosition, listView);
                    }
                    return true;
                default:
                    return true;
            }
        });
        return listView;
    }

    /** Shared with the glass-size preview so both editors use the same order. */
    static List<NavigationButton> getCurrentOrder() {
        if (!SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER) return new ArrayList<>(DEFAULT_ORDER);
        String serializedOrder = Settings.NAVIGATION_BAR_ORDER.get();
        List<NavigationButton> order = new ArrayList<>();
        Set<NavigationButton> seenButtons = new HashSet<>();

        if (!serializedOrder.trim().isEmpty()) {
            for (String value : serializedOrder.split(",")) {
                try {
                    NavigationButton button = NavigationButton.valueOf(
                            value.trim().toUpperCase(Locale.ROOT));
                    if (seenButtons.add(button)) {
                        order.add(button);
                    }
                } catch (IllegalArgumentException ignored) {
                    // Ignore unknown values from imported settings.
                }
            }
        }

        if (order.isEmpty()) {
            order.addAll(DEFAULT_ORDER);
        } else {
            for (NavigationButton button : DEFAULT_ORDER) {
                if (seenButtons.add(button)) {
                    order.add(button);
                }
            }
        }
        return order;
    }

    private static String serializeOrder(List<NavigationButton> order) {
        if (DEFAULT_ORDER.equals(order)) {
            return "";
        }

        StringBuilder serialized = new StringBuilder();
        for (NavigationButton button : order) {
            if (serialized.length() > 0) {
                serialized.append(',');
            }
            serialized.append(button.name());
        }
        return serialized.toString();
    }

    static String getLabel(NavigationButton button) {
        return button == NavigationButton.WATCH_LATER
                ? str("revanced_change_start_page_entry_watch_later")
                : str("revanced_change_start_page_entry_" + button.name().toLowerCase(Locale.ROOT));
    }

    /** Settings controlled by this editor rather than individual switch preferences. */
    static boolean isVisibilitySetting(Setting<?> setting) {
        if (setting == Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON) return true;
        for (NavigationButton button : DEFAULT_ORDER) {
            if (setting == PendingVisibility.settingFor(button)) return true;
        }
        return false;
    }

    /**
     * Stages visibility edits until OK. Existing setting keys remain compatible with imports.
     * Create and Notifications are alternate layouts of the same native navigation slot.
     */
    static final class PendingVisibility {
        private final Map<BooleanSetting, Boolean> values = new LinkedHashMap<>();

        PendingVisibility() {
            for (NavigationButton button : DEFAULT_ORDER) {
                BooleanSetting setting = settingFor(button);
                values.put(setting, setting.get());
            }
            BooleanSetting swap = Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON;
            values.put(swap, swap.get());
        }

        private static BooleanSetting settingFor(NavigationButton button) {
            return switch (button) {
                case HOME -> Settings.HIDE_NAVIGATION_HOME_BUTTON;
                case SHORTS -> Settings.HIDE_NAVIGATION_SHORTS_BUTTON;
                case CREATE -> Settings.HIDE_NAVIGATION_CREATE_BUTTON;
                case SUBSCRIPTIONS -> Settings.HIDE_NAVIGATION_SUBSCRIPTIONS_BUTTON;
                case NOTIFICATIONS -> Settings.HIDE_NAVIGATION_NOTIFICATIONS_BUTTON;
                case LIBRARY -> Settings.HIDE_NAVIGATION_LIBRARY_BUTTON;
                case EXPLORE -> Settings.HIDE_NAVIGATION_EXPLORE_BUTTON;
                case SEARCH -> Settings.SHOW_SEARCH_BUTTON;
                case SETTINGS -> Settings.SHOW_SETTINGS_BUTTON;
                case WATCH_LATER -> Settings.SHOW_WATCH_LATER_BUTTON;
            };
        }

        private static boolean isShowSetting(NavigationButton button) {
            return button == NavigationButton.SEARCH || button == NavigationButton.SETTINGS
                    || button == NavigationButton.WATCH_LATER;
        }

        boolean isEnabled(NavigationButton button) {
            boolean swapped = Boolean.TRUE.equals(values.get(Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON));
            if ((button == NavigationButton.CREATE && swapped)
                    || (button == NavigationButton.NOTIFICATIONS && !swapped)) return false;
            boolean value = Boolean.TRUE.equals(values.get(settingFor(button)));
            return isShowSetting(button) == value;
        }

        void toggle(NavigationButton button) {
            boolean enabled = !isEnabled(button);
            values.put(settingFor(button), isShowSetting(button) == enabled);
            if (enabled && (button == NavigationButton.CREATE || button == NavigationButton.NOTIFICATIONS)) {
                values.put(Settings.SWITCH_CREATE_WITH_NOTIFICATIONS_BUTTON,
                        button == NavigationButton.NOTIFICATIONS);
            }
        }

        void reset() {
            values.replaceAll((setting, value) -> setting.defaultValue);
        }

        boolean save() {
            boolean changed = false;
            for (var entry : values.entrySet()) {
                if (!entry.getValue().equals(entry.getKey().get())) {
                    entry.getKey().save(entry.getValue());
                    changed = true;
                }
            }
            return changed;
        }
    }

    private static final class NavigationButtonAdapter extends BaseAdapter {
        private final Context context;
        private final List<NavigationButton> buttons;
        private final int foregroundColor;
        private final PendingVisibility visibility;

        private NavigationButtonAdapter(Context context, List<NavigationButton> buttons, PendingVisibility visibility) {
            this.context = context;
            this.buttons = buttons;
            this.visibility = visibility;
            this.foregroundColor = BaseThemeUtils.getAppForegroundColor();
        }

        @Override
        public int getCount() {
            return buttons.size();
        }

        @Override
        public NavigationButton getItem(int position) {
            return buttons.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).ordinal();
        }

        @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            NavigationButton button = getItem(position);
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Utils.dipToPixels(48));
            row.setPadding(Utils.dipToPixels(8), 0, Utils.dipToPixels(8), 0);

            TextView label = new TextView(context);
            boolean enabled = visibility.isEnabled(button);
            label.setText((enabled ? "✓  " : "    ") + getLabel(button));
            label.setAlpha(enabled ? 1.0f : 0.45f);
            row.setContentDescription(str(enabled ? "revanced_navigation_bar_order_hide"
                    : "revanced_navigation_bar_order_show", getLabel(button)));
            row.setOnClickListener(view -> {
                visibility.toggle(button);
                notifyDataSetChanged();
            });
            label.setTextColor(foregroundColor);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(label, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            if (!SUPPORTS_ADDITIONAL_ITEMS_AND_ORDER) return row;

            TextView handle = new TextView(context);
            handle.setText("═");
            handle.setTextColor(foregroundColor);
            handle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            handle.setGravity(Gravity.CENTER);
            handle.setMinWidth(Utils.dipToPixels(48));
            handle.setContentDescription(str("revanced_navigation_bar_order_drag_handle"));
            handle.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    ClipData clipData = ClipData.newPlainText("navigation_button", button.name());
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDragAndDrop(clipData, shadowBuilder, button, 0);
                }
                return true;
            });
            row.addView(handle, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            return row;
        }

        private void moveButton(NavigationButton button, int targetPosition, ListView listView) {
            int currentPosition = buttons.indexOf(button);
            if (currentPosition < 0 || currentPosition == targetPosition
                    || targetPosition < 0 || targetPosition >= buttons.size()) {
                return;
            }

            buttons.remove(currentPosition);
            buttons.add(targetPosition, button);
            notifyDataSetChanged();
            listView.setSelection(targetPosition);
        }
    }
}
