package app.revanced.extension.shared.settings.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A custom color picker view that allows the user to select a color using a hue slider and a saturation-value selector.
 *
 * <p>
 * This view displays two main components for color selection:
 * <ul>
 *     <li><b>Hue Slider:</b> A vertical slider that allows the user to select the hue component of the color.</li>
 *     <li><b>Saturation-Value Selector:</b> A rectangular area that allows the user to select the saturation and value (brightness)
 *     components of the color based on the selected hue.</li>
 * </ul>
 * </p>
 *
 * <p>
 * The view uses {@link LinearGradient} and {@link ComposeShader} to create the color gradients for the hue slider and the
 * saturation-value selector. It also uses {@link Paint} to draw the selectors on the view.
 * </p>
 *
 * <p>
 * The selected color can be retrieved using {@link #getColor()} and can be set using {@link #setColor(int)}.
 * An {@link OnColorChangedListener} can be registered to receive notifications when the selected color changes.
 * </p>
 */
public class CustomColorPickerView extends View {

    /**
     * Interface definition for a callback to be invoked when the selected color changes.
     */
    public interface OnColorChangedListener {
        /**
         * Called when the selected color has changed.
         *
         * @param color The new selected color.
         */
        void onColorChanged(int color);
    }

    /** Paint object used to draw the hue slider. */
    private Paint huePaint;
    /** Paint object used to draw the saturation-value selector. */
    private Paint saturationValuePaint;
    /** Paint object used to draw the selectors (circles). */
    private Paint selectorPaint;

    /** Rectangle representing the bounds of the hue slider. */
    private RectF hueRect;
    /** Rectangle representing the bounds of the saturation-value selector. */
    private RectF saturationValueRect;

    /** Current hue value (0-360). */
    private float hue = 0f;
    /** Current saturation value (0-1). */
    private float saturation = 1f;
    /** Current value (brightness) value (0-1). */
    private float value = 1f;

    /** The currently selected color in ARGB format. */
    private int selectedColor = Color.HSVToColor(new float[]{hue, saturation, value});
    /** Radius of the selector circles. */
    private float selectorRadius;

    /** Listener to be notified when the selected color changes. */
    private OnColorChangedListener colorChangedListener;

    /**
     * Constructor for creating a CustomColorPickerView programmatically.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public CustomColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Initializes the paint objects and the rectangle bounds.
     */
    private void init() {
        // Initialize the paint for the hue slider with antialiasing enabled.
        huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Initialize the paint for the saturation-value selector with antialiasing enabled.
        saturationValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Initialize the paint for the selectors with antialiasing, stroke style, and a white color.
        selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectorPaint.setStyle(Paint.Style.STROKE);
        selectorPaint.setStrokeWidth(8f);
        selectorPaint.setColor(Color.WHITE);

        // Set the radius for the selector circles.
        selectorRadius = 15f;

        // Initialize the rectangle objects to be used for defining the bounds of the hue slider and saturation-value selector.
        hueRect = new RectF();
        saturationValueRect = new RectF();
    }

    /**
     * Called when the size of the view changes.
     * This method calculates and sets the bounds of the hue slider and saturation-value selector,
     * and creates the necessary shaders for the gradients.
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate padding based on the selector radius.
        float padding = selectorRadius * 2;

        // Set the bounds for the hue slider rectangle.
        hueRect.set(padding, padding, padding + 30, h - padding);

        // Set the bounds for the saturation-value selector rectangle.
        saturationValueRect.set(hueRect.right + padding, padding, w - padding, h - padding);

        // Create the hue gradient.
        // Generate an array of colors representing the full hue spectrum (0-360 degrees).
        int[] hueColors = new int[361];
        for (int i = 0; i <= 360; i++) {
            hueColors[i] = Color.HSVToColor(new float[]{i, 1f, 1f});
        }

        // Create a linear gradient for the hue slider.
        LinearGradient hueShader = new LinearGradient(
                hueRect.left, hueRect.top,
                hueRect.left, hueRect.bottom,
                hueColors,
                null,
                Shader.TileMode.CLAMP
        );

        // Set the shader for the hue paint.
        huePaint.setShader(hueShader);

        // Update the saturation-value shader based on the current hue.
        updateSaturationValueShader();
    }

    /**
     * Updates the shader for the saturation-value selector based on the currently selected hue.
     * This method creates a combined shader that blends a saturation gradient with a value gradient.
     */
    private void updateSaturationValueShader() {
        // Create a saturation-value gradient based on the current hue.
        // Calculate the start color (white with the selected hue) for the saturation gradient.
        int startColor = Color.HSVToColor(new float[]{hue, 0f, 1f});

        // Calculate the middle color (fully saturated color with the selected hue) for the saturation gradient.
        int midColor = Color.HSVToColor(new float[]{hue, 1f, 1f});

        // Create a linear gradient for the saturation from startColor to midColor.
        LinearGradient satShader = new LinearGradient(
                saturationValueRect.left, saturationValueRect.top,
                saturationValueRect.right, saturationValueRect.top,
                startColor,
                midColor,
                Shader.TileMode.CLAMP
        );

        // Create a linear gradient for the value (brightness) from white to black.
        LinearGradient valShader = new LinearGradient(
                saturationValueRect.left, saturationValueRect.top,
                saturationValueRect.left, saturationValueRect.bottom,
                Color.WHITE,
                Color.BLACK,
                Shader.TileMode.CLAMP
        );

        // Combine the saturation and value shaders using PorterDuff.Mode.MULTIPLY to create the final color.
        ComposeShader combinedShader = new ComposeShader(satShader, valShader, PorterDuff.Mode.MULTIPLY);

        // Set the combined shader for the saturation-value paint.
        saturationValuePaint.setShader(combinedShader);
    }

    /**
     * Draws the color picker view on the canvas.
     * This method draws the hue slider, the saturation-value selector, and the selectors (circles) on the canvas.
     *
     * @param canvas The canvas on which to draw.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the hue slider rectangle.
        canvas.drawRect(hueRect, huePaint);

        // Draw the saturation-value selector rectangle.
        canvas.drawRect(saturationValueRect, saturationValuePaint);

        // Draw the hue selector circle.
        // Calculate the Y position of the hue selector based on the current hue value.
        float hueSelectorY = hueRect.top + (hue / 360f) * hueRect.height();
        canvas.drawCircle(hueRect.centerX(), hueSelectorY, selectorRadius, selectorPaint);

        // Draw the saturation-value selector circle.
        // Calculate the X position of the saturation selector based on the current saturation value.
        float satSelectorX = saturationValueRect.left + saturation * saturationValueRect.width();

        // Calculate the Y position of the value selector based on the current value (inverted for visual representation).
        float valSelectorY = saturationValueRect.top + (1 - value) * saturationValueRect.height();
        canvas.drawCircle(satSelectorX, valSelectorY, selectorRadius, selectorPaint);
    }

    /**
     * Handles touch events on the view.
     * This method determines whether the touch event occurred within the hue slider or the saturation-value selector,
     * updates the corresponding values (hue, saturation, value), and invalidates the view to trigger a redraw.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility") //  performClick is not overridden, but not needed in this case.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Get the X and Y coordinates of the touch event.
        float x = event.getX();
        float y = event.getY();

        // Handle different touch actions.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Check if the touch event is within the hue slider.
                if (hueRect.contains(x, y)) {
                    // Update the hue value based on the touch position.
                    hue = Math.max(0f, Math.min(360f, (y - hueRect.top) / hueRect.height() * 360f));

                    // Update the saturation-value shader based on the new hue.
                    updateSaturationValueShader();
                }
                // Check if the touch event is within the saturation-value selector.
                else if (saturationValueRect.contains(x, y)) {
                    // Update the saturation value based on the touch position.
                    saturation = Math.max(0f, Math.min(1f, (x - saturationValueRect.left) / saturationValueRect.width()));

                    // Update the value (brightness) based on the touch position (inverted for visual representation).
                    value = Math.max(0f, Math.min(1f, 1 - (y - saturationValueRect.top) / saturationValueRect.height()));
                }

                // Convert the HSV values to an ARGB color.
                selectedColor = Color.HSVToColor(new float[]{hue, saturation, value});

                // Notify the listener if it's set.
                if (colorChangedListener != null) {
                    colorChangedListener.onColorChanged(selectedColor);
                }

                // Invalidate the view to trigger a redraw.
                invalidate();
                return true; // Indicate that the event was handled.
        }

        // Return false if the event was not handled.
        return super.onTouchEvent(event);
    }

    /**
     * Sets the currently selected color.
     *
     * @param color The color to set in ARGB format.
     */
    public void setColor(int color) {
        // Convert the ARGB color to HSV values.
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // Update the hue, saturation, and value.
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];

        // Update the selected color.
        selectedColor = color;

        // Update the saturation-value shader based on the new hue.
        updateSaturationValueShader();

        // Invalidate the view to trigger a redraw.
        invalidate();

        // Notify the listener if it's set.
        if (colorChangedListener != null) {
            colorChangedListener.onColorChanged(selectedColor);
        }
    }

    /**
     * Gets the currently selected color.
     *
     * @return The selected color in ARGB format.
     */
    public int getColor() {
        return selectedColor;
    }

    /**
     * Sets the listener to be notified when the selected color changes.
     *
     * @param listener The listener to set.
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.colorChangedListener = listener;
    }
}
