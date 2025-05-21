package app.revanced.extension.shared.settings.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.Preference;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.utils.ThemeUtils;

import static app.revanced.extension.shared.utils.StringRef.str;

/**
 * A custom preference that allows the user to select a color using a hexadecimal color code,
 * with a preview that opens a color picker when clicked.
 * <p>
 * Clicking the preference opens a dialog with a hex text field and color picker.
 * Clicking the color preview in the preference list directly opens the same dialog.
 */
@SuppressWarnings({"unused", "deprecation"})
public class ColorPreference extends Preference {
    private TextView colorPreviewDot;
    private int currentColor;
    private StringSetting colorSetting;

    // Dialog UI elements
    private CustomColorPickerView mColorPickerView;
    private EditText mHexEditText;

    // Flags to prevent listener feedback loops
    private boolean mIsPickerUpdatingEditText = false;
    private boolean mIsEditTextUpdatingPicker = false;

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
     * Initializes the preference by loading the color from settings.
     */
    private void init() {
        colorSetting = (StringSetting) Setting.getSettingFromPath(getKey());
        loadFromSettings();
    }

    /**
     * Loads the color from the associated {@link StringSetting}.
     * If the color is invalid, it resets the setting to the default value and loads the default color.
     */
    private void loadFromSettings() {
        try {
            setColor(colorSetting.get());
        } catch (Exception e) {
            Logger.printException(() -> "Invalid color: " + colorSetting.get(), e);
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
        currentColor = Color.parseColor(colorString) & 0xFFFFFF;
        colorSetting.save(String.format("#%06X", currentColor));
        updateColorDot();

        if (mHexEditText != null) {
            String hexFormatted = String.format("#%06X", currentColor);
            if (!mHexEditText.getText().toString().equalsIgnoreCase(hexFormatted)) {
                mHexEditText.setText(hexFormatted);
            }
        }
    }

    /**
     * Creates a GradientDrawable with an oval shape, filled with the current color and a border.
     * The border color is determined based on the current theme (dark or light).
     *
     * @return A configured GradientDrawable with the specified color and border.
     */
    private GradientDrawable createColorDrawable() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(currentColor | 0xFF000000);
        int borderColor = ThemeUtils.isDarkTheme() ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333");
        drawable.setStroke(2, borderColor);
        return drawable;
    }

    /**
     * Updates the color preview dot by setting its background to a new GradientDrawable
     * created with the current color and forces a redraw.
     */
    private void updateColorDot() {
        if (colorPreviewDot != null) {
            colorPreviewDot.setBackground(createColorDrawable());
            colorPreviewDot.invalidate(); // Force redraw
        }
    }

    /**
     * Binds the view to initialize the color preview dot and add it to the widget frame.
     * Sets up the dot's size, click listener, and updates its appearance.
     *
     * @param view The view to bind.
     */
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (colorPreviewDot == null) {
            colorPreviewDot = new TextView(getContext());
            int size = (int) (24 * getContext().getResources().getDisplayMetrics().density); // 24dp
            colorPreviewDot.setMinimumWidth(size);
            colorPreviewDot.setMinimumHeight(size);
            colorPreviewDot.setOnClickListener(v -> onClick());
        }

        // Remove colorPreviewDot from its current parent, if it has one
        if (colorPreviewDot.getParent() != null) {
            ((ViewGroup) colorPreviewDot.getParent()).removeView(colorPreviewDot);
        }

        updateColorDot();

        ViewGroup widgetFrame = view.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            widgetFrame.removeAllViews();
            widgetFrame.addView(colorPreviewDot);
            widgetFrame.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handles the click event by displaying a color picker dialog.
     * The dialog allows the user to select a new color via a color picker view or hex input.
     * Includes options to save, cancel, or reset to the default color.
     */
    @Override
    protected void onClick() {
        int originalColor = currentColor;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(ResourceUtils.getLayoutIdentifier("revanced_color_picker"), null);

        mColorPickerView = dialogView.findViewById(ResourceUtils.getIdIdentifier("color_picker_view"));
        FrameLayout editTextContainer = dialogView.findViewById(ResourceUtils.getIdIdentifier("color_picker_hex"));

        mHexEditText = new EditText(getContext());
        mHexEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mHexEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        editTextContainer.addView(mHexEditText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        String initialColorHex = String.format("#%06X", currentColor & 0xFFFFFF);
        mHexEditText.setText(initialColorHex);
        mHexEditText.setSelection(initialColorHex.length());
        mColorPickerView.setInitialColor(currentColor | 0xFF000000);

        mColorPickerView.setOnColorChangedListener(newColor -> {
            if (mIsEditTextUpdatingPicker || mHexEditText == null) return;

            mIsPickerUpdatingEditText = true;
            String newHex = String.format("#%06X", newColor & 0xFFFFFF);
            if (!mHexEditText.getText().toString().equalsIgnoreCase(newHex)) {
                mHexEditText.setText(newHex);
                mHexEditText.setSelection(newHex.length());
            }
            mIsPickerUpdatingEditText = false;
        });

        mHexEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (mIsPickerUpdatingEditText || mColorPickerView == null) return;

                mIsEditTextUpdatingPicker = true;
                try {
                    String hexString = s.toString();
                    if (hexString.matches("^#([A-Fa-f0-9]{6})$")) {
                        int parsedColor = Color.parseColor(hexString);
                        if ((mColorPickerView.getColor() & 0xFFFFFF) != (parsedColor & 0xFFFFFF)) {
                            mColorPickerView.setColor(parsedColor | 0xFF000000);
                        }
                    }
                } catch (IllegalArgumentException ignored) {}
                mIsEditTextUpdatingPicker = false;
            }
        });

        Utils.setEditTextDialogTheme(builder);
        builder.setView(dialogView);

        if (colorSetting != null) {
            builder.setNeutralButton(str("revanced_extended_settings_reset"), null);
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            lp.width = (int) (displayMetrics.widthPixels * 0.9f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String colorStringToSave = mHexEditText.getText().toString();
            try {
                if (!colorStringToSave.isEmpty()) {
                    if (colorStringToSave.matches("^#([A-Fa-f0-9]{6})$")) {
                        setColor(colorStringToSave);
                    } else {
                        throw new IllegalArgumentException("Invalid color format: " + colorStringToSave);
                    }
                } else {
                    Utils.showToastShort(str("revanced_color_invalid_toast"));
                    setColor(String.format("#%06X", originalColor));
                }
                dialog.dismiss();
            } catch (IllegalArgumentException ex) {
                Utils.showToastShort(str("revanced_color_invalid_toast"));
                setColor(String.format("#%06X", originalColor));
                dialog.dismiss();
            } catch (Exception ex) {
                Logger.printException(() -> "Dialog closed failure", ex);
                Utils.showToastShort(str("revanced_color_invalid_toast"));
                setColor(String.format("#%06X", originalColor));
                dialog.dismiss();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            loadFromSettings();
            dialog.dismiss();
        });

        if (colorSetting != null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                try {
                    String defaultValue = colorSetting.defaultValue;
                    if (mHexEditText != null) {
                        mHexEditText.setText(defaultValue);
                        mHexEditText.setSelection(defaultValue.length());
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "Reset color failure", ex);
                }
            });
        }

        mColorPickerView.requestFocus();
    }
}
