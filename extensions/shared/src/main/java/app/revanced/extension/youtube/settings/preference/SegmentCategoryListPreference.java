package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.preference.ListPreference;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.util.Objects;

import app.revanced.extension.shared.settings.preference.CustomColorPickerView;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour;
import app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory;

@SuppressWarnings({"unused", "deprecation"})
public class SegmentCategoryListPreference extends ListPreference {
    private SegmentCategory mCategory;
    private EditText mEditText;
    private int mClickedDialogEntryIndex;
    private TextView colorDotView;

    private void init() {
        final SegmentCategory segmentCategory = SegmentCategory.byCategoryKey(getKey());
        final boolean isHighlightCategory = segmentCategory == SegmentCategory.HIGHLIGHT;
        mCategory = Objects.requireNonNull(segmentCategory);
        // Edit: Using preferences to sync together multiple pieces
        // of code together is messy and should be rethought.
        setKey(segmentCategory.behaviorSetting.key);
        setDefaultValue(segmentCategory.behaviorSetting.defaultValue);

        setEntries(isHighlightCategory
                ? CategoryBehaviour.getBehaviorDescriptionsWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorDescriptions());
        setEntryValues(isHighlightCategory
                ? CategoryBehaviour.getBehaviorKeyValuesWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorKeyValues());
        updateTitle();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SegmentCategoryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SegmentCategoryListPreference(Context context) {
        super(context);
        init();
    }

    private void showColorPickerDialog(Context context) {
        // Store the original color in case the user cancels
        final int originalColor = Color.parseColor(mCategory.colorString()) & 0xFFFFFF;

        String currentColorString = mEditText.getText().toString();
        int initialColor;
        try {
            initialColor = Color.parseColor(currentColorString);
        } catch (IllegalArgumentException e) {
            initialColor = originalColor;
        }

        // Create a RelativeLayout to hold the color picker view.
        RelativeLayout layout = new RelativeLayout(context);
        layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // Inflate the color picker layout.
        View dialogView = LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_color_picker"), layout);
        // Get the CustomColorPickerView from the inflated layout.
        CustomColorPickerView colorPickerView = dialogView.findViewById(ResourceUtils.getIdIdentifier("color_picker_view"));
        // Set the initial color of the color picker.
        colorPickerView.setInitialColor(initialColor);

        // Create an AlertDialog with the color picker view.
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)  // Listener will be set later.
                .setNegativeButton(android.R.string.cancel, null); // Listener will be set later

        // Apply the dynamic theme for the dialog.
        Utils.setEditTextDialogTheme(builder);

        AlertDialog dialog = builder.create();

        // Prevent the dialog from closing when touched outside.
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(d -> {
            // Set a listener for color changes in the color picker view.
            colorPickerView.setOnColorChangedListener(color -> {
                // Update the EditText with the selected color in real-time.
                int selectedColor = color & 0xFFFFFF; // Mask out alpha for consistency.
                String hexColor = String.format("#%06X", selectedColor);
                mEditText.setText(hexColor);
                mEditText.setSelection(hexColor.length());
            });

            // Set an OnClickListener for the OK button.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Get the selected color from the color picker.
                int selectedColor = colorPickerView.getColor() & 0xFFFFFF; // Mask out alpha for consistency.
                String hexColor = String.format("#%06X", selectedColor);
                mEditText.setText(hexColor);
                mEditText.setSelection(hexColor.length());
                dialog.dismiss();
            });

            // Set an OnClickListener for the Cancel button.
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                // Restore the original color and update the preview and EditText.
                mEditText.setText(String.format("#%06X", originalColor));
                colorDotView.setText(SegmentCategory.getCategoryColorDot(originalColor));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        try {
            Utils.setEditTextDialogTheme(builder);
            super.onPrepareDialogBuilder(builder);

            Context context = builder.getContext();
            TableLayout table = new TableLayout(context);
            table.setOrientation(LinearLayout.HORIZONTAL);
            table.setPadding(70, 0, 70, 0);

            TableRow row = new TableRow(context);

            TextView colorTextLabel = new TextView(context);
            colorTextLabel.setText(str("revanced_sb_color_dot_label"));
            row.addView(colorTextLabel);

            colorDotView = new TextView(context);
            colorDotView.setText(mCategory.getCategoryColorDot());
            colorDotView.setPadding(30, 0, 30, 0);
            colorDotView.setOnClickListener(v -> showColorPickerDialog(context));
            row.addView(colorDotView);

            mEditText = new EditText(context);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            mEditText.setText(mCategory.colorString());
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
            builder.setTitle(mCategory.title.toString());

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> onClick(dialog, DialogInterface.BUTTON_POSITIVE));
            builder.setNeutralButton(str("revanced_sb_reset_color"), (dialog, which) -> {
                try {
                    mCategory.resetColor();
                    updateTitle();
                    Utils.showToastShort(str("revanced_sb_color_reset"));
                } catch (Exception ex) {
                    Logger.printException(() -> "setNeutralButton failure", ex);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            mClickedDialogEntryIndex = findIndexOfValue(getValue());
            builder.setSingleChoiceItems(getEntries(), mClickedDialogEntryIndex, (dialog, which) -> mClickedDialogEntryIndex = which);
        } catch (Exception ex) {
            Logger.printException(() -> "onPrepareDialogBuilder failure", ex);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try {
            if (positiveResult && mClickedDialogEntryIndex >= 0 && getEntryValues() != null) {
                String value = getEntryValues()[mClickedDialogEntryIndex].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                    mCategory.setBehaviour(Objects.requireNonNull(CategoryBehaviour.byReVancedKeyValue(value)));
                    SegmentCategory.updateEnabledCategories();
                }
                String colorString = mEditText.getText().toString();
                try {
                    if (!colorString.equals(mCategory.colorString())) {
                        mCategory.setColor(colorString);
                        Utils.showToastShort(str("revanced_sb_color_changed"));
                    }
                } catch (IllegalArgumentException ex) {
                    Utils.showToastShort(str("revanced_sb_color_invalid"));
                }
                updateTitle();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onDialogClosed failure", ex);
        }
    }

    private void updateTitle() {
        setTitle(mCategory.getTitleWithColorDot());
        setEnabled(Settings.SB_ENABLED.get());
    }
}