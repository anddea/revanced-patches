/*
 * Copyright (C) 2026 lavinhoque33
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Licensed under the GNU General Public License v3.0.
 * Written by lavinhoque33, 2026-10-20.
 */

package app.morphe.extension.youtube.patches.general.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.function.IntConsumer;

import app.morphe.extension.shared.utils.Utils;

/**
 * Icons and dialogs shared by the offline library and player, drawn like the rest of the RVX screens.
 */
final class OfflineUi {
    private OfflineUi() {
    }

    /**
     * @param name Name of a drawable shipped by the patch, without the {@code revanced_offline_} prefix.
     * @return The drawable tinted with {@code color}.
     */
    static Drawable icon(Context context, String name, int color) {
        Drawable drawable = context.getDrawable(
                Utils.getResourceIdentifierOrThrow(context, "revanced_offline_" + name, "drawable"));
        if (drawable != null) {
            drawable = drawable.mutate();
            drawable.setTint(color);
        }
        return drawable;
    }

    /**
     * Shows the choices as a list with a check mark behind the selected one.
     *
     * @param title Optional dialog title.
     */
    static void showChoices(Activity activity, @Nullable String title, String[] labels, int selected,
                            int foreground, IntConsumer onChosen) {
        final int padding = Utils.dipToPixels(24);
        final int checkSize = Utils.dipToPixels(24);

        ListView list = new ListView(activity);
        list.setDivider(null);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return labels.length;
            }

            @Override
            public Object getItem(int position) {
                return labels[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, @Nullable View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setMinimumHeight(Utils.dipToPixels(52));
                row.setPadding(padding, 0, padding, 0);

                TextView text = new TextView(activity);
                text.setText(labels[position]);
                text.setTextColor(foreground);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                ImageView check = new ImageView(activity);
                check.setImageDrawable(icon(activity, "check", foreground));
                check.setVisibility(position == selected ? View.VISIBLE : View.INVISIBLE);
                row.addView(check, new LinearLayout.LayoutParams(checkSize, checkSize));
                return row;
            }
        });

        AlertDialog.Builder builder = Utils.getDialogBuilder(activity).setView(list);
        if (title != null) builder.setTitle(title);
        AlertDialog dialog = builder.create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            dialog.dismiss();
            onChosen.accept(position);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(dialogBackground(foreground));
        }
    }

    private static Drawable dialogBackground(int foreground) {
        // Dark text on a light dialog and light text on a dark one.
        final boolean darkText = Color.luminance(foreground) < 0.5f;
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(Utils.dipToPixels(28));
        shape.setColor(darkText ? 0xFFF1F1F1 : 0xFF282828);
        return shape;
    }
}
