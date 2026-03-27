package app.morphe.extension.music.settings.preference;

import static app.morphe.extension.music.utils.ExtendedUtils.getDialogBuilder;
import static app.morphe.extension.shared.utils.StringRef.str;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Objects;

import app.morphe.extension.music.sponsorblock.objects.CategoryBehaviour;
import app.morphe.extension.music.sponsorblock.objects.SegmentCategory;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.Utils;

public class SponsorBlockCategoryPreference {
    private static final String[] CategoryBehaviourEntries = {str("revanced_sb_skip_automatically"), str("revanced_sb_skip_ignore")};
    private static final CategoryBehaviour[] CategoryBehaviourEntryValues = {CategoryBehaviour.SKIP_AUTOMATICALLY, CategoryBehaviour.IGNORE};
    private static int mClickedDialogEntryIndex;


    public static void showDialog(Activity baseActivity, String categoryString) {
        try {
            SegmentCategory category = Objects.requireNonNull(SegmentCategory.byCategoryKey(categoryString));
            final AlertDialog.Builder builder = getDialogBuilder(baseActivity);
            TableLayout table = new TableLayout(baseActivity);
            table.setOrientation(LinearLayout.HORIZONTAL);
            table.setPadding(70, 0, 150, 0);

            TableRow row = new TableRow(baseActivity);

            TextView colorTextLabel = new TextView(baseActivity);
            colorTextLabel.setText(str("revanced_sb_color_dot_label"));
            row.addView(colorTextLabel);

            TextView colorDotView = new TextView(baseActivity);
            colorDotView.setText(category.getCategoryColorDot());
            colorDotView.setPadding(30, 0, 30, 0);
            row.addView(colorDotView);

            final EditText mEditText = new EditText(baseActivity);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mEditText.setText(category.colorString());
            mEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        String colorString = s.toString();
                        if (!colorString.startsWith("#")) {
                            s.insert(0, "#"); // recursively calls back into this method
                            return;
                        }
                        if (colorString.length() > 7) {
                            s.delete(7, colorString.length());
                            return;
                        }
                        final int color = Color.parseColor(colorString);
                        colorDotView.setText(SegmentCategory.getCategoryColorDot(color));
                    } catch (IllegalArgumentException ex) {
                        // ignore
                    }
                }
            });
            mEditText.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(mEditText);

            table.addView(row);
            builder.setView(table);
            builder.setTitle(category.title.toString());

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                category.behaviour = CategoryBehaviourEntryValues[mClickedDialogEntryIndex];
                category.setBehaviour(category.behaviour);
                SegmentCategory.updateEnabledCategories();

                String colorString = mEditText.getText().toString();
                try {
                    if (!colorString.equals(category.colorString())) {
                        category.setColor(colorString);
                        Utils.showToastShort(str("revanced_sb_color_changed"));
                    }
                } catch (IllegalArgumentException ex) {
                    Utils.showToastShort(str("revanced_sb_color_invalid"));
                }
            });
            builder.setNeutralButton(str("revanced_sb_reset_color"), (dialog, which) -> {
                try {
                    category.resetColor();
                    Utils.showToastShort(str("revanced_sb_color_reset"));
                } catch (Exception ex) {
                    Logger.printException(() -> "setNeutralButton failure", ex);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            final int index = Arrays.asList(CategoryBehaviourEntryValues).indexOf(category.behaviour);
            mClickedDialogEntryIndex = Math.max(index, 0);

            builder.setSingleChoiceItems(CategoryBehaviourEntries, mClickedDialogEntryIndex,
                    (dialog, id) -> mClickedDialogEntryIndex = id);
            builder.show();
        } catch (Exception ex) {
            Logger.printException(() -> "dialogBuilder failure", ex);
        }
    }
}
