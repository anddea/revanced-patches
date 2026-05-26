package app.morphe.extension.shared.ui;

import static app.morphe.extension.shared.settings.preference.ColorPickerPreference.DISABLED_ALPHA;
import static app.morphe.extension.shared.utils.BaseThemeUtils.getAppForegroundColor;
import static app.morphe.extension.shared.utils.BaseThemeUtils.isDarkModeEnabled;
import static app.morphe.extension.shared.utils.Utils.dipToPixels;

import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.annotation.ColorInt;

public class ColorDot {
    private static final int STROKE_WIDTH = dipToPixels(1.5f); // Stroke width in dp.
    private static final int STROKE_ALPHA = 0x80; // 50% opacity.

    /**
     * Creates a circular drawable with a main fill and a stroke.
     * Stroke adapts to dark/light theme so the preview stays visible against the app background.
     */
    public static GradientDrawable createColorDotDrawable(@ColorInt int color) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        circle.setStroke(STROKE_WIDTH, (getAppForegroundColor() & 0x00FFFFFF) | (STROKE_ALPHA << 24));

        return circle;
    }

    /**
     * Applies the color dot drawable to the target view.
     */
    public static void applyColorDot(View targetView, @ColorInt int color, boolean enabled) {
        if (targetView == null) return;
        targetView.setBackground(createColorDotDrawable(color));
        targetView.setAlpha(enabled ? 1.0f : DISABLED_ALPHA);
        if (!isDarkModeEnabled()) {
            targetView.setClipToOutline(true);
            targetView.setElevation(dipToPixels(2));
        }
    }
}
