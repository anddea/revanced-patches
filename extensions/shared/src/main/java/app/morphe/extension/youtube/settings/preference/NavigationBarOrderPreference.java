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

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.os.Build;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.Utils;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.shared.NavigationBar;
import app.morphe.extension.youtube.shared.NavigationBar.NavigationButton;

/**
 * Displays the known YouTube navigation items in a drag-and-drop order editor.
 *
 * <p>The setting is saved only when the dialog's OK button is pressed. Optional items are shown
 * as well, so one saved order can be reused when YouTube changes the active layout.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public class NavigationBarOrderPreference extends Preference {

    private static final List<NavigationButton> DEFAULT_ORDER = List.copyOf(NavigationBar.getDefaultNavigationButtonOrder());

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
        for (NavigationButton button : order) {
            if (summary.length() > 0) {
                summary.append(" → ");
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
        NavigationButtonAdapter adapter = new NavigationButtonAdapter(context, pendingOrder);

        ListView listView = getListView(context, adapter);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
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
                    Settings.NAVIGATION_BAR_ORDER.save(serializeOrder(pendingOrder));
                    updateSummary();
                },
                null,
                str("revanced_settings_reset"),
                () -> {
                    pendingOrder.clear();
                    pendingOrder.addAll(DEFAULT_ORDER);
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

    private static List<NavigationButton> getCurrentOrder() {
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

    private static String getLabel(NavigationButton button) {
        return str("revanced_change_start_page_entry_" + button.name().toLowerCase(Locale.ROOT));
    }

    private static final class NavigationButtonAdapter extends BaseAdapter {
        private final Context context;
        private final List<NavigationButton> buttons;
        private final int foregroundColor;

        private NavigationButtonAdapter(Context context, List<NavigationButton> buttons) {
            this.context = context;
            this.buttons = buttons;
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

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            NavigationButton button = getItem(position);
            LinearLayout row = new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(Utils.dipToPixels(48));
            row.setPadding(Utils.dipToPixels(8), 0, Utils.dipToPixels(8), 0);

            TextView label = new TextView(context);
            label.setText(getLabel(button));
            label.setTextColor(foregroundColor);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            row.addView(label, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        view.startDragAndDrop(clipData, shadowBuilder, button, 0);
                    } else {
                        view.startDrag(clipData, shadowBuilder, button, 0);
                    }
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
