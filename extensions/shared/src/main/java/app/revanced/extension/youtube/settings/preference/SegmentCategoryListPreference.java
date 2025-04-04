package app.revanced.extension.youtube.settings.preference;

import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory.applyOpacityToColor;

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

import java.util.Locale;
import java.util.Objects;

import app.revanced.extension.shared.settings.preference.CustomColorPickerView;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.sponsorblock.objects.CategoryBehaviour;
import app.revanced.extension.youtube.sponsorblock.objects.SegmentCategory;

@SuppressWarnings({"unused", "deprecation"})
public class SegmentCategoryListPreference extends ListPreference {
    private SegmentCategory category;
    private TextView colorDotView;
    private EditText colorEditText;
    private EditText opacityEditText;
    /**
     * #RRGGBB
     */
    private int categoryColor;
    /**
     * [0, 1]
     */
    private float categoryOpacity;
    private int selectedDialogEntryIndex;

    private void init() {
        final SegmentCategory segmentCategory = SegmentCategory.byCategoryKey(getKey());
        category = Objects.requireNonNull(segmentCategory);

        // Edit: Using preferences to sync together multiple pieces
        // of code is messy and should be rethought.
        setKey(segmentCategory.behaviorSetting.key);
        setDefaultValue(segmentCategory.behaviorSetting.defaultValue);

        final boolean isHighlightCategory = category == SegmentCategory.HIGHLIGHT;
        setEntries(isHighlightCategory
                ? CategoryBehaviour.getBehaviorDescriptionsWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorDescriptions());
        setEntryValues(isHighlightCategory
                ? CategoryBehaviour.getBehaviorKeyValuesWithoutSkipOnce()
                : CategoryBehaviour.getBehaviorKeyValues());

        updateTitleFromCategory();
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
        final int originalColor = Color.parseColor(category.getColorString()) & 0xFFFFFF;

        String currentColorString = colorEditText.getText().toString();
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
                colorEditText.setText(hexColor);
                colorEditText.setSelection(hexColor.length());
            });

            // Set an OnClickListener for the OK button.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Get the selected color from the color picker.
                int selectedColor = colorPickerView.getColor() & 0xFFFFFF; // Mask out alpha for consistency.
                String hexColor = String.format("#%06X", selectedColor);
                colorEditText.setText(hexColor);
                colorEditText.setSelection(hexColor.length());
                dialog.dismiss();
            });

            // Set an OnClickListener for the Cancel button.
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                // Restore the original color and update the preview and EditText.
                colorEditText.setText(String.format("#%06X", originalColor));
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

            categoryColor = category.getColorNoOpacity();
            categoryOpacity = category.getOpacity();

            Context context = builder.getContext();
            GridLayout gridLayout = new GridLayout(context);
            gridLayout.setPadding(70, 0, 70, 0); // Padding for the entire layout.
            gridLayout.setColumnCount(3);
            gridLayout.setRowCount(2);

            GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
            gridParams.rowSpec = GridLayout.spec(0); // First row.
            gridParams.columnSpec = GridLayout.spec(0); // First column.
            TextView colorTextLabel = new TextView(context);
            colorTextLabel.setText(str("revanced_sb_color_dot_label"));
            colorTextLabel.setLayoutParams(gridParams);
            gridLayout.addView(colorTextLabel);

            gridParams = new GridLayout.LayoutParams();
            gridParams.rowSpec = GridLayout.spec(0); // First row.
            gridParams.columnSpec = GridLayout.spec(1); // Second column.
            gridParams.setMargins(0, 0, 10, 0);
            colorDotView = new TextView(context);
            colorDotView.setLayoutParams(gridParams);
            colorDotView.setOnClickListener(v -> showColorPickerDialog(context));
            gridLayout.addView(colorDotView);
            updateCategoryColorDot();

            gridParams = new GridLayout.LayoutParams();
            gridParams.rowSpec = GridLayout.spec(0); // First row.
            gridParams.columnSpec = GridLayout.spec(2); // Third column.
            colorEditText = new EditText(context);
            colorEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            colorEditText.setTextLocale(Locale.US);
            colorEditText.setText(category.getColorString());
            colorEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable edit) {
                    try {
                        String colorString = edit.toString();
                        final int colorStringLength = colorString.length();

                        if (!colorString.startsWith("#")) {
                            edit.insert(0, "#"); // Recursively calls back into this method.
                            return;
                        }

                        final int maxColorStringLength = 7; // #RRGGBB
                        if (colorStringLength > maxColorStringLength) {
                            edit.delete(maxColorStringLength, colorStringLength);
                            return;
                        }

                        categoryColor = Color.parseColor(colorString);
                        updateCategoryColorDot();
                    } catch (IllegalArgumentException ex) {
                        // Ignore.
                    }
                }
            });
            colorEditText.setLayoutParams(gridParams);
            gridLayout.addView(colorEditText);

            gridParams = new GridLayout.LayoutParams();
            gridParams.rowSpec = GridLayout.spec(1); // Second row.
            gridParams.columnSpec = GridLayout.spec(0, 1); // First and second column.
            TextView opacityLabel = new TextView(context);
            opacityLabel.setText(str("revanced_sb_color_opacity_label"));
            opacityLabel.setLayoutParams(gridParams);
            gridLayout.addView(opacityLabel);

            gridParams = new GridLayout.LayoutParams();
            gridParams.rowSpec = GridLayout.spec(1); // Second row.
            gridParams.columnSpec = GridLayout.spec(2); // Third column.
            opacityEditText = new EditText(context);
            opacityEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            opacityEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable edit) {
                    try {
                        String editString = edit.toString();
                        final int opacityStringLength = editString.length();

                        final int maxOpacityStringLength = 4; // [0.00, 1.00]
                        if (opacityStringLength > maxOpacityStringLength) {
                            edit.delete(maxOpacityStringLength, opacityStringLength);
                            return;
                        }

                        final float opacity = opacityStringLength == 0
                                ? 0
                                : Float.parseFloat(editString);
                        if (opacity < 0) {
                            categoryOpacity = 0;
                            edit.replace(0, opacityStringLength, "0");
                            return;
                        } else if (opacity > 1.0f) {
                            categoryOpacity = 1;
                            edit.replace(0, opacityStringLength, "1.0");
                            return;
                        } else if (!editString.endsWith(".")) {
                            // Ignore "0." and "1." until the user finishes entering a valid number.
                            categoryOpacity = opacity;
                        }

                        updateCategoryColorDot();
                    } catch (NumberFormatException ex) {
                        // Should never happen.
                        Logger.printException(() -> "Could not parse opacity string", ex);
                    }
                }
            });
            opacityEditText.setLayoutParams(gridParams);
            gridLayout.addView(opacityEditText);
            updateOpacityText();

            builder.setView(gridLayout);
            builder.setTitle(category.title.toString());

            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> onClick(dialog, DialogInterface.BUTTON_POSITIVE));
            builder.setNeutralButton(str("revanced_sb_reset_color"), (dialog, which) -> {
                try {
                    category.resetColorAndOpacity();
                    updateTitleFromCategory();
                    Utils.showToastShort(str("revanced_sb_color_reset"));
                } catch (Exception ex) {
                    Logger.printException(() -> "setNeutralButton failure", ex);
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);

            selectedDialogEntryIndex = findIndexOfValue(getValue());
            builder.setSingleChoiceItems(getEntries(), selectedDialogEntryIndex,
                    (dialog, which) -> selectedDialogEntryIndex = which);
        } catch (Exception ex) {
            Logger.printException(() -> "onPrepareDialogBuilder failure", ex);
        }
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try {
            if (positiveResult && selectedDialogEntryIndex >= 0 && getEntryValues() != null) {
                String value = getEntryValues()[selectedDialogEntryIndex].toString();
                if (callChangeListener(value)) {
                    setValue(value);
                    category.setBehaviour(Objects.requireNonNull(CategoryBehaviour.byReVancedKeyValue(value)));
                    SegmentCategory.updateEnabledCategories();
                }
                try {
                    String colorString = colorEditText.getText().toString();
                    if (!colorString.equals(category.getColorString()) || categoryOpacity != category.getOpacity()) {
                        category.setColor(colorString);
                        category.setOpacity(categoryOpacity);
                        Utils.showToastShort(str("revanced_sb_color_changed"));
                    }
                } catch (IllegalArgumentException ex) {
                    Utils.showToastShort(str("revanced_sb_color_invalid"));
                }

                updateTitleFromCategory();
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onDialogClosed failure", ex);
        }
    }

    private void applyOpacityToCategoryColor() {
        categoryColor = applyOpacityToColor(categoryColor, categoryOpacity);
    }

    private void updateTitleFromCategory() {
        categoryColor = category.getColorNoOpacity();
        categoryOpacity = category.getOpacity();
        applyOpacityToCategoryColor();

        setTitle(category.getTitleWithColorDot(categoryColor));
    }

    private void updateCategoryColorDot() {
        applyOpacityToCategoryColor();

        colorDotView.setText(SegmentCategory.getCategoryColorDot(categoryColor));
    }

    private void updateOpacityText() {
        opacityEditText.setText(String.format(Locale.US, "%.2f", categoryOpacity));
    }
}