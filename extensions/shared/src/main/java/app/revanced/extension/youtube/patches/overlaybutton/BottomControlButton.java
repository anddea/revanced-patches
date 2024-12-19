package app.revanced.extension.youtube.patches.overlaybutton;

import static app.revanced.extension.shared.utils.ResourceUtils.getAnimation;
import static app.revanced.extension.shared.utils.ResourceUtils.getInteger;
import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.getChildView;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Objects;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

public abstract class BottomControlButton {
    private static final Animation fadeIn;
    private static final Animation fadeOut;
    private static final Animation fadeOutImmediate;

    private final ColorFilter cf =
            new PorterDuffColorFilter(Color.parseColor("#fffffc79"), PorterDuff.Mode.SRC_ATOP);

    private final WeakReference<ImageView> buttonRef;
    private final BooleanSetting setting;
    private final BooleanSetting primaryInteractionSetting;
    private final BooleanSetting secondaryInteractionSetting;
    protected boolean isVisible;

    static {
        fadeIn = getAnimation("fade_in");
        // android.R.integer.config_shortAnimTime, 200
        fadeIn.setDuration(getInteger("fade_duration_fast"));

        fadeOut = getAnimation("fade_out");
        // android.R.integer.config_mediumAnimTime, 400
        fadeOut.setDuration(getInteger("fade_overlay_fade_duration"));

        fadeOutImmediate = getAnimation("abc_fade_out");
        // android.R.integer.config_shortAnimTime, 200
        fadeOutImmediate.setDuration(getInteger("fade_duration_fast"));
    }

    @NonNull
    public static Animation getButtonFadeIn() {
        return fadeIn;
    }

    @NonNull
    public static Animation getButtonFadeOut() {
        return fadeOut;
    }

    @NonNull
    public static Animation getButtonFadeOutImmediate() {
        return fadeOutImmediate;
    }

    public BottomControlButton(@NonNull ViewGroup bottomControlsViewGroup, @NonNull String imageViewButtonId, @NonNull BooleanSetting booleanSetting,
                               @NonNull View.OnClickListener onClickListener, @Nullable View.OnLongClickListener longClickListener) {
        this(bottomControlsViewGroup, imageViewButtonId, booleanSetting, null, null, onClickListener, longClickListener);
    }

    @SuppressWarnings("unused")
    public BottomControlButton(@NonNull ViewGroup bottomControlsViewGroup, @NonNull String imageViewButtonId, @NonNull BooleanSetting booleanSetting, @Nullable BooleanSetting primaryInteractionSetting,
                               @NonNull View.OnClickListener onClickListener, @Nullable View.OnLongClickListener longClickListener) {
        this(bottomControlsViewGroup, imageViewButtonId, booleanSetting, primaryInteractionSetting, null, onClickListener, longClickListener);
    }

    public BottomControlButton(@NonNull ViewGroup bottomControlsViewGroup, @NonNull String imageViewButtonId, @NonNull BooleanSetting booleanSetting,
                               @Nullable BooleanSetting primaryInteractionSetting, @Nullable BooleanSetting secondaryInteractionSetting,
                               @NonNull View.OnClickListener onClickListener, @Nullable View.OnLongClickListener longClickListener) {
        Logger.printDebug(() -> "Initializing button: " + imageViewButtonId);

        setting = booleanSetting;

        // Create the button.
        ImageView imageView = Objects.requireNonNull(getChildView(bottomControlsViewGroup, imageViewButtonId));
        imageView.setOnClickListener(onClickListener);
        this.primaryInteractionSetting = primaryInteractionSetting;
        this.secondaryInteractionSetting = secondaryInteractionSetting;
        if (primaryInteractionSetting != null) {
            imageView.setSelected(primaryInteractionSetting.get());
        }
        if (secondaryInteractionSetting != null) {
            setColorFilter(imageView, secondaryInteractionSetting.get());
        }
        if (longClickListener != null) {
            imageView.setOnLongClickListener(longClickListener);
        }
        imageView.setVisibility(View.GONE);
        buttonRef = new WeakReference<>(imageView);
    }

    public void changeActivated(boolean activated) {
        ImageView imageView = buttonRef.get();
        if (imageView == null)
            return;
        imageView.setActivated(activated);
    }

    public void changeSelected(boolean selected) {
        ImageView imageView = buttonRef.get();
        if (imageView == null || primaryInteractionSetting == null)
            return;

        if (imageView.getColorFilter() == cf) {
            Utils.showToastShort(str("revanced_overlay_button_not_allowed_warning"));
            return;
        }

        imageView.setSelected(selected);
        primaryInteractionSetting.save(selected);
    }

    public void changeColorFilter() {
        ImageView imageView = buttonRef.get();
        if (imageView == null) return;
        if (primaryInteractionSetting == null || secondaryInteractionSetting == null)
            return;

        imageView.setSelected(true);
        primaryInteractionSetting.save(true);

        final boolean newValue = !secondaryInteractionSetting.get();
        secondaryInteractionSetting.save(newValue);
        setColorFilter(imageView, newValue);
    }

    public void setColorFilter(ImageView imageView, boolean selected) {
        if (selected)
            imageView.setColorFilter(cf);
        else
            imageView.clearColorFilter();
    }

    public void setVisibility(boolean visible, boolean animation) {
        ImageView imageView = buttonRef.get();
        if (imageView == null || isVisible == visible) return;
        isVisible = visible;

        imageView.clearAnimation();
        if (visible && setting.get()) {
            imageView.setVisibility(View.VISIBLE);
            if (animation) imageView.startAnimation(fadeIn);
            return;
        }
        if (imageView.getVisibility() == View.VISIBLE) {
            if (animation) imageView.startAnimation(fadeOut);
            imageView.setVisibility(View.GONE);
        }
    }

    public void setVisibilityNegatedImmediate() {
        ImageView imageView = buttonRef.get();
        if (imageView == null) return;
        if (!setting.get()) return;

        imageView.clearAnimation();
        imageView.startAnimation(fadeOutImmediate);
        imageView.setVisibility(View.GONE);
    }
}
