package app.revanced.extension.shared.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;

import static app.revanced.extension.shared.utils.ResourceUtils.getLayoutIdentifier;
import static app.revanced.extension.shared.utils.StringRef.str;

/**
 * A custom preference that allows the user to select a color using either a hexadecimal color code or a color picker
 * dialog.
 * <p>
 * This preference extends {@link EditTextPreference} and displays a colored dot next to the title and the text field
 * to indicate the currently selected color.
 */
@SuppressWarnings({"unused", "deprecation"})
public class ColorPreference extends EditTextPreference {
    /** A {@link TextView} used to display a preview of the selected color next to the input field. */
    private TextView colorPreview;
    /** The currently selected color in RGB format (without alpha). */
    private int currentColor;
    /** The {@link StringSetting} object that this preference is associated with. */
    private StringSetting colorSetting;
    /** The original title of the preference (without the color dot). */
    private CharSequence originalTitle;

    /**
     * Constructor for creating a ColorPreference programmatically.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag.
     */
    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor for creating a ColorPreference programmatically.
     *
     * @param context The Context the view is running in.
     */
    public ColorPreference(Context context) {
        super(context);
        init();
    }

    /**
     * Initializes the preference by loading the color from settings, setting the input type of the edit text,
     * and updating the title with a colored dot.
     */
    private void init() {
        // Retrieve the StringSetting object associated with this preference using its key.
        colorSetting = (StringSetting) Setting.getSettingFromPath(getKey());

        // Store the original title.
        originalTitle = super.getTitle();

        // Set the input type of the EditText to accept uppercase hexadecimal characters.
        getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);

        // Load the color from the settings.
        loadFromSettings();

        // Update the title to include a colored dot representing the selected color.
        updateTitleWithColorDot();
    }

    /**
     * Loads the color from the associated {@link StringSetting}.
     * If the color is invalid, it resets the setting to the default value and loads the default color.
     */
    private void loadFromSettings() {
        try {
            // Try to set the color from the setting's current value.
            setColor(colorSetting.get());
        } catch (Exception ex) {
            // If an exception occurs (e.g., invalid color format), log the error, reset the setting to default, and load again.
            Logger.printException(() -> "Invalid color: " + colorSetting.get(), ex);
            colorSetting.resetToDefault();
            setColor(colorSetting.get());
        }
    }

    /**
     * Sets the currently selected color.
     *
     * @param colorString The color string in hexadecimal format (e.g., "#RRGGBB").
     * @throws IllegalArgumentException If the color string is invalid.
     */
    public void setColor(String colorString) throws IllegalArgumentException {
        // Parse the color string and extract the RGB components (ignoring alpha).
        currentColor = Color.parseColor(colorString) & 0xFFFFFF;

        // Save the color string to the associated StringSetting.
        colorSetting.save(String.format("#%06X", currentColor));

        // Update the title with the new color dot.
        updateTitleWithColorDot();
    }

    /**
     * Creates a layout containing a color preview and an {@link EditText} for entering a hexadecimal color code.
     *
     * @param context The Context to use for creating the layout.
     * @return A {@link LinearLayout} containing the color preview and the EditText.
     */
    private LinearLayout createColorInputLayout(Context context) {
        // Create a new LinearLayout with horizontal orientation and padding.
        LinearLayout layout = new LinearLayout(context);
        layout.setPadding(70, 0, 70, 0);

        // Create a TextView to display the color preview.
        colorPreview = new TextView(context);
        // Update the color preview with the currently selected color.
        updateColorPreview();
        // Set an OnClickListener on the color preview to open the color picker dialog.
        colorPreview.setOnClickListener(v -> showColorPickerDialog(context));
        // Add the color preview to the layout.
        layout.addView(colorPreview);

        // Get the EditText from the parent class.
        EditText editText = getEditText();
        // Remove the EditText from its current parent if it exists
        ViewParent parent = editText.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(editText);
        }
        // Set the initial text of the EditText to the current color in hexadecimal format.
        editText.setText(String.format("#%06X", currentColor));
        // Add a TextWatcher to the EditText to update the color preview in real-time.
        editText.addTextChangedListener(createColorTextWatcher());
        // Add the EditText to the layout.
        layout.addView(editText);

        // Return the created layout.
        return layout;
    }

    /**
     * Updates the color preview {@link TextView} with a colored dot representing the currently selected color.
     */
    private void updateColorPreview() {
        if (colorPreview != null) {
            colorPreview.setText(getColorDot(currentColor));
        }
    }

    /**
     * Shows the color picker dialog.
     *
     * @param context The Context to use for creating the dialog.
     */
    private void showColorPickerDialog(Context context) {
        // Store the original color in case the user cancels
        final int originalColor = currentColor;

        EditText editText = getEditText();
        String currentColorString = editText.getText().toString();
        int initialColor;
        try {
            initialColor = Color.parseColor(currentColorString);
        } catch (IllegalArgumentException e) {
            initialColor = currentColor;
        }

        // Create a RelativeLayout to hold the color picker view.
        RelativeLayout layout = new RelativeLayout(context);
        layout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));

        // Inflate the color picker layout.
        View dialogView = LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_color_picker"), layout);
        // Get the CustomColorPickerView from the inflated layout.
        CustomColorPickerView colorPickerView = dialogView.findViewById(ResourceUtils.getIdIdentifier("color_picker_view"));
        // Set the initial color of the color picker.
        colorPickerView.setColor(initialColor);

        // Create an AlertDialog with the color picker view.
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null) // Listener will be set later to prevent immediate closing.
                .setNegativeButton(android.R.string.cancel, null) // Listener will be set later.
                .create();

        // Set an OnShowListener for the dialog to configure button behavior.
        dialog.setOnShowListener(d -> {
            // Set a listener for color changes in the color picker view.
            colorPickerView.setOnColorChangedListener(color -> {
                // Update the EditText with the selected color in real-time.
                int selectedColor = color & 0xFFFFFF; // Mask out alpha for consistency.
                String hexColor = String.format("#%06X", selectedColor);
                editText.setText(hexColor);
                editText.setSelection(hexColor.length());
            });

            // Set an OnClickListener for the OK button.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                // Get the selected color from the color picker.
                int selectedColor = colorPickerView.getColor() & 0xFFFFFF; // Mask out alpha for consistency.
                String hexColor = String.format("#%06X", selectedColor);
                editText.setText(hexColor);
                editText.setSelection(hexColor.length());
                // Save the new color immediately from color picker
                // setColor(hexColor);
                dialog.dismiss();
            });

            // Set an OnClickListener for the Cancel button.
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                // Restore the original color and update the preview and EditText.
                editText.setText(String.format("#%06X", originalColor));
                currentColor = originalColor;
                updateColorPreview();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    /**
     * Creates a {@link TextWatcher} to monitor changes in the color input {@link EditText}.
     *
     * @return A TextWatcher that updates the color preview when the text changes.
     */
    private TextWatcher createColorTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used.
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Try to parse the color from the EditText and update the color preview.
                try {
                    String colorString = s.toString();
                    if (!colorString.isEmpty()) {
                        currentColor = Color.parseColor(colorString) & 0xFFFFFF; // Mask out alpha for consistency.
                        updateColorPreview();
                    }
                } catch (IllegalArgumentException ex) {
                    // Keep showing the previous valid color if an invalid color is entered.
                }
            }
        };
    }

    /**
     * Called when the dialog builder is being prepared.
     * This method sets the view of the dialog to the custom color input layout and adds a neutral button for resetting the color.
     *
     * @param builder The {@link AlertDialog.Builder} to prepare.
     */
    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        // Apply the EditText dialog theme.
        Utils.setEditTextDialogTheme(builder);

        // Set the custom view for the dialog.
        builder.setView(createColorInputLayout(builder.getContext()));

        // Add a neutral button for resetting the color to the default value, if setting exists.
        if (colorSetting != null) {
            builder.setNeutralButton(str("revanced_extended_settings_reset"), null);
        }
    }

    /**
     * Called when the dialog is about to be shown.
     * This method sets an {@link View.OnClickListener} for the neutral button to reset the color.
     *
     * @param state The state to pass to the superclass.
     */
    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Get the AlertDialog.
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog == null) return;

        // Set an OnClickListener for the neutral button.
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            try {
                // Reset the color to the default value.
                String defaultValue = colorSetting.defaultValue;
                EditText editText = getEditText();
                editText.setText(defaultValue);
                editText.setSelection(defaultValue.length());
                currentColor = Color.parseColor(defaultValue) & 0xFFFFFF;
                updateColorPreview();
            } catch (Exception ex) {
                Logger.printException(() -> "Reset color failure", ex);
            }
        });
    }

    /**
     * Called when the dialog is closed.
     * This method saves the color if the positive button was clicked, or resets to default if necessary.
     *
     * @param positiveResult True if the positive button was clicked, false otherwise.
     */
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        try {
            if (positiveResult) {
                // If the positive button was clicked, get the color from the EditText and save it.
                EditText editText = getEditText();
                String colorString = editText.getText().toString();
                if (!colorString.isEmpty()) {
                    setColor(colorString);
                } else {
                    // If the EditText is empty, reset to default.
                    colorSetting.resetToDefault();
                    loadFromSettings();
                }
            } else {
                // If dialog is cancelled (not just the color picker), reload the original color.
                loadFromSettings();
            }
        } catch (IllegalArgumentException ex) {
            // If an invalid color is entered, show a toast and reset to default.
            Utils.showToastShort(str("revanced_extended_reset_to_default_toast"));
            colorSetting.resetToDefault();
            loadFromSettings();
        } catch (Exception ex) {
            Logger.printException(() -> "Dialog closed failure", ex);
        }
        // Update the title with the color dot.
        updateTitleWithColorDot();
    }

    /**
     * Returns an HTML string representing a colored dot.
     *
     * @param color The color of the dot in RGB format (without alpha).
     * @return An HTML string with a colored dot.
     */
    private static String getColorDotHTML(int color) {
        color &= 0xFFFFFF; // Mask out alpha.
        return String.format("<big><font color=\"#%06X\">⬤</font></big>", color);
    }

    /**
     * Returns a {@link Spanned} object containing a colored dot.
     *
     * @param color The color of the dot in RGB format (without alpha).
     * @return A Spanned object with a colored dot.
     */
    public static Spanned getColorDot(int color) {
        return Html.fromHtml("<big>" + getColorDotHTML(color) + "</big>");
    }

    /**
     * Returns a {@link Spanned} object containing the original title with a colored dot prepended.
     *
     * @return A Spanned object with the title and a colored dot.
     */
    public Spanned getTitleWithColorDot() {
        return Html.fromHtml(getColorDotHTML(currentColor) + " " + originalTitle);
    }

    /**
     * Updates the title of the preference to include a colored dot representing the currently selected color.
     */
    private void updateTitleWithColorDot() {
        setTitle(getTitleWithColorDot());
    }
}
