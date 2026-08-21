/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Portions of this file are modified by anddea:
 * Copyright (C) 2026 anddea
 * https://github.com/anddea/revanced-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.shared.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
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

import java.util.Locale;
import java.util.Objects;

import app.morphe.extension.shared.ui.CustomDialog;
import app.morphe.extension.shared.utils.BaseThemeUtils;
import app.morphe.extension.shared.utils.ResourceType;
import app.morphe.extension.shared.utils.ResourceUtils;

/**
 * A {@link CustomDialogListPreference} that previews the adaptive layers of each bundled icon.
 *
 * <p>The patch copies the project-owned icon layers to stable resource names using the
 * {@code morphe_adaptive_background_} and {@code morphe_adaptive_foreground_} prefixes. The
 * original entry falls back to the unmodified launcher resource.</p>
 */
@SuppressWarnings({"unused", "deprecation"})
public class IconListPreference extends CustomDialogListPreference {

    public static final int LAYOUT_MORPHE_ICON_LIST_ITEM = ResourceUtils.getLayoutIdentifier(
            "morphe_icon_list_item");
    public static final int ID_MORPHE_ITEM_ICON = ResourceUtils.getIdIdentifier("morphe_item_icon");

    private static final float ICON_SIZE_DP = 48f;
    private static final float ICON_CORNER_RADIUS_FRACTION = 0.22f;

    @Nullable
    private static String originalLauncherIconName;

    /** Called by the injected branding hook before settings are opened. */
    public static void setOriginalLauncherIconName(@Nullable String name) {
        originalLauncherIconName = name;
    }

    @Nullable
    private Drawable[] iconDrawables;

    public IconListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public IconListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IconListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IconListPreference(Context context) {
        super(context);
    }

    @NonNull
    protected Drawable[] resolveIconDrawables() {
        CharSequence[] values = getEntryValues();
        if (values == null) return new Drawable[0];

        Context context = getContext();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int sizePx = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, ICON_SIZE_DP, metrics));
        float cornerRadius = sizePx * ICON_CORNER_RADIUS_FRACTION;

        Drawable[] drawables = new Drawable[values.length];
        for (int i = 0; i < values.length; i++) {
            String suffix = values[i].toString().toLowerCase(Locale.US);
            drawables[i] = buildIconDrawable(context, suffix, sizePx, cornerRadius);
        }
        return drawables;
    }

    @Nullable
    private static Drawable buildIconDrawable(
            Context context, String suffix, int sizePx, float cornerRadius) {
        try {
            int backgroundId = resolveResourceId("morphe_adaptive_background_" + suffix);
            int foregroundId = resolveResourceId("morphe_adaptive_foreground_" + suffix);

            Drawable background = backgroundId == 0 ? null : context.getDrawable(backgroundId);
            Drawable foreground = foregroundId == 0 ? null : context.getDrawable(foregroundId);
            boolean adaptive = background != null || foreground != null;

            Drawable source;
            if (background != null && foreground != null) {
                source = new LayerDrawable(new Drawable[]{background, foreground});
            } else {
                source = Objects.requireNonNullElseGet(
                        foreground,
                        () -> Objects.requireNonNullElseGet(
                                background,
                                () -> resolveOriginalIconDrawable(context)));
            }

            return renderToRounded(context, source, sizePx, cornerRadius, adaptive);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Drawable resolveOriginalIconDrawable(Context context) {
        try {
            if (originalLauncherIconName != null && !originalLauncherIconName.isEmpty()) {
                int resourceId = ResourceUtils.getIdentifier(
                        originalLauncherIconName, ResourceType.MIPMAP);
                if (resourceId != 0) {
                    Drawable drawable = context.getDrawable(resourceId);
                    if (drawable != null) return drawable;
                }
            }
        } catch (Exception ignored) {
            // Fall through to the package manager's current icon.
        }
        return context.getPackageManager().getApplicationIcon(context.getApplicationInfo());
    }

    private static int resolveResourceId(String name) {
        int resourceId = ResourceUtils.getIdentifier(name, ResourceType.DRAWABLE);
        return resourceId != 0
                ? resourceId
                : ResourceUtils.getIdentifier(name, ResourceType.MIPMAP);
    }

    @NonNull
    private static Drawable renderToRounded(
            Context context,
            Drawable source,
            int sizePx,
            float cornerRadius,
            boolean adaptive) {
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Path clip = new Path();
        clip.addRoundRect(0, 0, sizePx, sizePx, cornerRadius, cornerRadius, Path.Direction.CW);
        canvas.clipPath(clip);

        if (adaptive) {
            // The source layers use a 108dp canvas with a 72dp safe zone.
            int scaledSize = Math.round(sizePx * (108f / 72f));
            int offset = (scaledSize - sizePx) / 2;
            source.setBounds(-offset, -offset, sizePx + offset, sizePx + offset);
        } else {
            source.setBounds(0, 0, sizePx, sizePx);
        }
        source.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @Override
    protected void showDialog(Bundle state) {
        if (iconDrawables == null) {
            iconDrawables = resolveIconDrawables();
        }

        boolean hasIcon = false;
        for (Drawable drawable : iconDrawables) {
            if (drawable != null) {
                hasIcon = true;
                break;
            }
        }
        if (!hasIcon || LAYOUT_MORPHE_ICON_LIST_ITEM == 0 || ID_MORPHE_ITEM_ICON == 0) {
            super.showDialog(state);
            return;
        }

        Context context = getContext();
        CharSequence[] entries = getEntriesForDialog();
        CharSequence[] entryValues = getEntryValues();
        if (entries == null || entryValues == null || entries.length != entryValues.length) {
            super.showDialog(state);
            return;
        }

        ListView listView = new ListView(context);
        listView.setId(android.R.id.list);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        IconListPreferenceAdapter adapter = new IconListPreferenceAdapter(
                context,
                LAYOUT_MORPHE_ICON_LIST_ITEM,
                entries,
                entryValues,
                getValue(),
                iconDrawables);
        listView.setAdapter(adapter);

        String currentValue = getValue();
        if (currentValue != null) {
            for (int i = 0; i < entryValues.length; i++) {
                if (currentValue.equals(entryValues[i].toString())) {
                    listView.setItemChecked(i, true);
                    listView.setSelection(i);
                    break;
                }
            }
        }

        Pair<Dialog, LinearLayout> dialogPair = CustomDialog.create(
                context,
                getTitle() == null ? "" : getTitle().toString(),
                null,
                null,
                null,
                null,
                this::clearHighlightedEntriesForDialog,
                null,
                null,
                true);

        Dialog dialog = dialogPair.first;
        dialog.setOnDismissListener(ignored -> clearHighlightedEntriesForDialog());

        LinearLayout mainLayout = dialogPair.second;
        mainLayout.addView(listView, mainLayout.getChildCount() - 1,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selected = entryValues[position].toString();
            if (callChangeListener(selected)) {
                setValue(selected);
                if (getStaticSummary() == null) {
                    CharSequence[] originalEntries = getEntries();
                    if (originalEntries != null && position < originalEntries.length) {
                        setSummary(originalEntries[position]);
                    }
                }
                adapter.setSelectedValue(selected);
                adapter.notifyDataSetChanged();
            }
            clearHighlightedEntriesForDialog();
            dialog.dismiss();
        });

        dialog.show();
    }

    /** Adapter that places a rendered icon beside the standard RVX checkmark and label. */
    public static class IconListPreferenceAdapter extends ArrayAdapter<CharSequence> {
        private static class ViewHolder {
            ImageView checkIcon;
            View placeholder;
            ImageView itemIcon;
            TextView itemText;
        }

        private final int layoutResourceId;
        private final CharSequence[] entryValues;
        private final Drawable[] iconDrawables;
        private String selectedValue;

        IconListPreferenceAdapter(
                Context context,
                int resource,
                CharSequence[] entries,
                CharSequence[] entryValues,
                String selectedValue,
                Drawable[] iconDrawables) {
            super(context, resource, entries);
            this.layoutResourceId = resource;
            this.entryValues = entryValues;
            this.selectedValue = selectedValue;
            this.iconDrawables = iconDrawables;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view = convertView;
            ViewHolder holder;

            if (view == null) {
                view = LayoutInflater.from(getContext()).inflate(layoutResourceId, parent, false);
                holder = new ViewHolder();
                holder.checkIcon = view.findViewById(CustomDialogListPreference.ID_REVANCED_CHECK_ICON);
                holder.placeholder = view.findViewById(
                        CustomDialogListPreference.ID_REVANCED_CHECK_ICON_PLACEHOLDER);
                holder.itemIcon = view.findViewById(ID_MORPHE_ITEM_ICON);
                holder.itemText = view.findViewById(CustomDialogListPreference.ID_REVANCED_ITEM_TEXT);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            holder.itemText.setText(getItem(position));
            holder.itemText.setTextColor(BaseThemeUtils.getAppForegroundColor());

            boolean selected = entryValues[position].toString().equals(selectedValue);
            holder.checkIcon.setVisibility(selected ? View.VISIBLE : View.GONE);
            holder.checkIcon.setColorFilter(BaseThemeUtils.getAppForegroundColor());
            holder.placeholder.setVisibility(selected ? View.GONE : View.VISIBLE);

            Drawable icon = position < iconDrawables.length ? iconDrawables[position] : null;
            if (icon != null) {
                holder.itemIcon.setImageDrawable(icon);
                holder.itemIcon.setVisibility(View.VISIBLE);
            } else {
                holder.itemIcon.setVisibility(View.INVISIBLE);
            }

            return view;
        }

        void setSelectedValue(String value) {
            selectedValue = value;
        }
    }
}
