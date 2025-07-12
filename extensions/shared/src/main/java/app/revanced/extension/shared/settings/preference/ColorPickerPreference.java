package app.revanced.extension.shared.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.preference.Preference;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.shared.settings.StringSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.utils.ThemeUtils;

import java.util.Locale;
import java.util.regex.Pattern;

import static app.revanced.extension.shared.utils.StringRef.str;

/**
 * A custom preference that allows the user to select a color using a hexadecimal color code,
 * with a preview that opens a color picker when clicked.
 * <p>
 * Clicking the preference opens a dialog with a hex text field, color preview, and color picker.
 * Clicking the color preview in the preference list directly opens the same dialog.
 */
@SuppressWarnings({"unused", "deprecation"})
public class ColorPickerPreference extends Preference {
    /**
     * Character to show the color appearance.
     */
    public static final String COLOR_DOT_STRING = "⬤";

    /**
     * Length of a valid color string of format #RRGGBB.
     */
    public static final int COLOR_STRING_LENGTH = 7;

    /**
     * Matches everything that is not a hex number/letter.
     */
    private static final Pattern PATTERN_NOT_HEX = Pattern.compile("[^0-9A-Fa-f]");

    private TextView colorPreviewDot;
    private int currentColor;
    private StringSetting colorSetting;

    // Dialog UI elements
    private ColorPickerView mColorPickerView;
    private EditText mHexEditText;
    private TextView dialogColorPreview;

    // Flags to prevent listener feedback loops
    private boolean mIsPickerUpdatingEditText = false;
    private boolean mIsEditTextUpdatingPicker = false;

    /**
     * Constructor for creating a ColorPickerPreference programmatically.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag.
     */
    public ColorPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Constructor for creating a ColorPickerPreference programmatically.
     *
     * @param context The Context the view is running in.
     */
    public ColorPickerPreference(Context context) {
        super(context);
        init();
    }

    /**
     * Removes non-valid hex characters, converts to all uppercase,
     * and adds # character to the start if not present.
     */
    public static String cleanupColorCodeString(String colorString) {
        // Remove non-hex chars, convert to uppercase, and ensure correct length
        String result = "#" + PATTERN_NOT_HEX.matcher(colorString)
                .replaceAll("").toUpperCase(Locale.ROOT);

        if (result.length() < COLOR_STRING_LENGTH) {
            return result;
        }

        return result.substring(0, COLOR_STRING_LENGTH);
    }

    /**
     * @param color RGB color, without an alpha channel.
     * @return #RRGGBB hex color string
     */
    public static String getColorString(@ColorInt int color) {
        String colorString = String.format("#%06X", color & 0x00FFFFFF);
        if ((color & 0xFF000000) != 0) {
            // Likely a bug somewhere.
            Logger.printException(() -> "getColorString: color has alpha channel: " + colorString);
        }
        return colorString;
    }

    /**
     * Creates a Spanned object for a colored dot using SpannableString.
     *
     * @param color The RGB color (without alpha).
     * @return A Spanned object with the colored dot.
     */
    public static Spanned getColorDot(@ColorInt int color) {
        SpannableString spannable = new SpannableString(COLOR_DOT_STRING);
        spannable.setSpan(new ForegroundColorSpan(color | 0xFF000000), 0, COLOR_DOT_STRING.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new RelativeSizeSpan(1.5f), 0, 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    /**
     * Initializes the preference by loading the color from settings.
     */
    private void init() {
        colorSetting = (StringSetting) Setting.getSettingFromPath(getKey());
        if (colorSetting == null) {
            Logger.printException(() -> "Could not find color setting for: " + getKey());
        }
        loadFromSettings();
    }

    /**
     * Loads the color from the associated {@link StringSetting}.
     * If the color is invalid, it resets the setting to the default value and loads the default color.
     */
    private void loadFromSettings() {
        try {
            currentColor = Color.parseColor(colorSetting.get()) & 0x00FFFFFF;
            updateColorDot();
        } catch (Exception e) {
            Logger.printException(() -> "Invalid color: " + colorSetting.get(), e);
            colorSetting.resetToDefault();
            currentColor = Color.parseColor(colorSetting.get()) & 0x00FFFFFF;
            updateColorDot();
        }
    }

    /**
     * Sets the currently selected color.
     *
     * @param colorString The color string in hexadecimal format (e.g., "#RRGGBB").
     * @throws IllegalArgumentException If the color string is invalid.
     */
    public void setColor(String colorString) throws IllegalArgumentException {
        try {
            currentColor = Color.parseColor(colorString) & 0x00FFFFFF;
            colorSetting.save(getColorString(currentColor));
            updateColorDot();

            if (mHexEditText != null) {
                String hexFormatted = getColorString(currentColor);
                if (!mHexEditText.getText().toString().equalsIgnoreCase(hexFormatted)) {
                    mHexEditText.setText(hexFormatted);
                    mHexEditText.setSelection(hexFormatted.length());
                }
            }
        } catch (IllegalArgumentException ex) {
            Logger.printDebug(() -> "Parse color error: " + colorString, ex);
            Utils.showToastShort(str("revanced_extended_settings_color_invalid"));
            throw ex;
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
        int borderColor = ThemeUtils.isDarkModeEnabled() ? Color.parseColor("#E0E0E0") : Color.parseColor("#333333");
        drawable.setStroke(2, borderColor);
        return drawable;
    }

    /**
     * Updates the color preview dot by setting its text to a colored dot
     * created with the current color and forces a redraw.
     */
    private void updateColorDot() {
        if (colorPreviewDot != null) {
            colorPreviewDot.setBackground(createColorDrawable());
            colorPreviewDot.invalidate(); // Force redraw
        }
    }

    /**
     * Updates the dialog color preview TextView with a colored dot.
     */
    private void updateDialogColorPreview() {
        if (dialogColorPreview != null) {
            dialogColorPreview.setText(getColorDot(currentColor));
        }
    }

    /**
     * Binds the view to initialize the color preview dot and add it to the widget frame.
     * Sets up the Ascertain the dot's size, click listener, and updates its appearance.
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
     * Creates a TextWatcher to monitor changes in the EditText for color input.
     *
     * @return A TextWatcher that updates the color preview on valid input.
     */
    private TextWatcher createColorTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable edit) {
                if (mIsPickerUpdatingEditText || mColorPickerView == null) return;

                mIsEditTextUpdatingPicker = true;
                try {
                    String colorString = edit.toString();
                    String sanitizedColorString = cleanupColorCodeString(colorString);
                    if (!sanitizedColorString.equals(colorString)) {
                        edit.replace(0, colorString.length(), sanitizedColorString);
                        return;
                    }

                    if (sanitizedColorString.length() != COLOR_STRING_LENGTH) {
                        // User is still typing out the color.
                        return;
                    }

                    final int newColor = Color.parseColor(colorString) & 0x00FFFFFF;
                    if (currentColor != newColor) {
                        Logger.printDebug(() -> "afterTextChanged: " + sanitizedColorString);
                        currentColor = newColor;
                        updateDialogColorPreview();
                        mColorPickerView.setColor(newColor | 0xFF000000);
                    }
                } catch (Exception ex) {
                    Logger.printException(() -> "afterTextChanged failure", ex);
                } finally {
                    mIsEditTextUpdatingPicker = false;
                }
            }
        };
    }

    /**
     * Handles the click event by displaying a custom color picker dialog.
     * The dialog allows the user to select a new color via a color picker view or hex input.
     * Includes options to save, cancel, or reset to the default color.
     */
    @Override
    protected void onClick() {
        final int originalColor = currentColor & 0x00FFFFFF;
        Context context = getContext();

        // Inflate color picker view.
        View colorPicker = LayoutInflater.from(context).inflate(
                ResourceUtils.getLayoutIdentifier("revanced_color_picker"), null);
        mColorPickerView = colorPicker.findViewById(
                ResourceUtils.getIdIdentifier("revanced_color_picker_view"));
        mColorPickerView.setInitialColor(currentColor | 0xFF000000);

        // Horizontal layout for preview and EditText.
        LinearLayout inputLayout = new LinearLayout(context);
        inputLayout.setOrientation(LinearLayout.HORIZONTAL);

        dialogColorPreview = new TextView(context);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        previewParams.setMargins(Utils.dipToPixels(15), 0, Utils.dipToPixels(10), 0);
        dialogColorPreview.setLayoutParams(previewParams);
        inputLayout.addView(dialogColorPreview);
        updateDialogColorPreview();

        mHexEditText = new EditText(context);
        mHexEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mHexEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(COLOR_STRING_LENGTH)});
        mHexEditText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        String currentColorString = getColorString(currentColor);
        mHexEditText.setText(currentColorString);
        mHexEditText.setSelection(currentColorString.length());
        inputLayout.addView(mHexEditText);

        // Add a dummy view to take up remaining horizontal space.
        View paddingView = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
        );
        paddingView.setLayoutParams(params);
        inputLayout.addView(paddingView);

        // Create content container for color picker and input layout.
        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.addView(colorPicker);
        contentContainer.addView(inputLayout);

        // Create ScrollView to wrap the content container.
        ScrollView contentScrollView = new ScrollView(context);
        contentScrollView.setVerticalScrollBarEnabled(false);
        contentScrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams scrollViewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        contentScrollView.setLayoutParams(scrollViewParams);
        contentScrollView.addView(contentContainer);

        // Create custom dialog.
        Pair<Dialog, LinearLayout> dialogPair = Utils.createCustomDialog(
                context,
                getTitle() != null ? getTitle().toString() : str("revanced_extended_settings_color_picker"),
                null,
                null,
                null,
                () -> {
                    // OK button action.
                    try {
                        String colorString = mHexEditText.getText().toString();
                        if (colorString.length() != COLOR_STRING_LENGTH) {
                            Utils.showToastShort(str("revanced_extended_settings_color_invalid"));
                            return;
                        }
                        setColor(colorString);
                    } catch (IllegalArgumentException ex) {
                        Utils.showToastShort(str("revanced_extended_settings_color_invalid"));
                    } catch (Exception ex) {
                        Logger.printException(() -> "OK button failure", ex);
                    }
                },
                () -> {
                    // Cancel button action.
                    currentColor = originalColor;
                    updateColorDot();
                },
                str("revanced_extended_settings_reset_color"),
                () -> {
                    // Neutral button action.
                    try {
                        final int defaultColor = Color.parseColor(colorSetting.defaultValue) & 0x00FFFFFF;
                        mColorPickerView.setColor(defaultColor | 0xFF000000);
                    } catch (Exception ex) {
                        Logger.printException(() -> "Reset button failure", ex);
                    }
                },
                false
        );

        // Add the ScrollView to the dialog's main layout.
        LinearLayout dialogMainLayout = dialogPair.second;
        dialogMainLayout.addView(contentScrollView, dialogMainLayout.getChildCount() - 1);

        // Set up color picker listener with debouncing.
        mColorPickerView.setOnColorChangedListener(color -> {
            if (mIsEditTextUpdatingPicker || mHexEditText == null) return;

            mIsPickerUpdatingEditText = true;
            String newHex = getColorString(color & 0x00FFFFFF);
            Logger.printDebug(() -> "onColorChanged: " + newHex);
            currentColor = color & 0x00FFFFFF;
            mHexEditText.setText(newHex);
            mHexEditText.setSelection(newHex.length());
            updateDialogColorPreview();
            mIsPickerUpdatingEditText = false;
        });

        mHexEditText.addTextChangedListener(createColorTextWatcher());

        // Configure and show the dialog.
        Dialog dialog = dialogPair.first;
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnCancelListener(d -> {
            currentColor = originalColor;
            updateColorDot();
        });

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
            lp.width = (int) (displayMetrics.widthPixels * 0.9f);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(lp);
        }

        dialog.show();
    }
}
