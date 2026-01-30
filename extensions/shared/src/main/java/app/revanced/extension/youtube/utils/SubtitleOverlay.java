package app.revanced.extension.youtube.utils;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import androidx.annotation.Nullable;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.Settings;

import java.util.Objects;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.dipToPixels;
import static app.revanced.extension.shared.utils.Utils.showToastLong;

/**
 * Manages a subtitle overlay that can be dragged vertically and snaps to screen edges.
 * Requires SYSTEM_ALERT_WINDOW permission on Android M+.
 * <p>
 * This class creates a draggable overlay window containing a TextView for displaying subtitles.
 * The overlay can be moved vertically by the user and will snap to the top or bottom of the screen
 * when released. It supports dynamic text updates and handles orientation changes.
 */
@SuppressWarnings("deprecation")
public class SubtitleOverlay {
    /**
     * Margin in density-independent pixels (DP) for the overlay's positioning.
     */
    private static final int MARGIN_DP = 16;

    /**
     * Corner radius in DP for the subtitle TextView's background.
     */
    private static final int CORNER_RADIUS_DP = 12;

    /**
     * Duration of the snap animation in milliseconds.
     */
    private static final int SNAP_ANIMATION_DURATION_MS = 300;

    /**
     * Tension for the overshoot interpolator used in snap animations.
     */
    private static final float SNAP_OVERSHOOT_TENSION = 1.5f;

    /**
     * The Activity context used to access system services and inflate layouts.
     */
    final Activity context;

    /**
     * WindowManager instance for managing the overlay window.
     */
    private final WindowManager windowManager;

    /**
     * Layout parameters for positioning and sizing the overlay window.
     */
    private final WindowManager.LayoutParams layoutParams;

    /**
     * Handler for posting tasks to the main thread.
     */
    private final Handler mainHandler;

    /**
     * The root view of the subtitle overlay, or null if not initialized.
     */
    @Nullable
    private View overlayView;

    /**
     * TextView for displaying subtitle text, or null if not initialized.
     */
    @Nullable
    private TextView subtitleTextView;

    /**
     * Indicates whether the overlay is currently visible.
     */
    private boolean isShowing = false;

    /**
     * Indicates whether the overlay is positioned at the bottom of the screen.
     */
    private boolean isAtBottom = true;

    // region Dragging state

    /**
     * Indicates whether the overlay is being dragged by the user.
     */
    private boolean isDragging = false;

    /**
     * Offset between the touch point and the top of the overlay during dragging.
     */
    private float dragOffset;

    /**
     * Gesture detector for handling touch events like taps and long presses.
     */
    private GestureDetector gestureDetector;

    /**
     * Key for storing the subtitle overlay position in SharedPreferences.
     */
    private static final String PREF_NAME = "SubtitleOverlayPrefs";
    private static final String KEY_IS_AT_BOTTOM = "is_at_bottom";

    // endregion Dragging state

    /**
     * Constructs a new SubtitleOverlay instance.
     * Initializes the context, WindowManager, Handler, and layout parameters for the overlay.
     */
    public SubtitleOverlay() {
        this.context = Utils.getActivity();
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Configure layout parameters for the overlay window
        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        layoutParams.y = 0;
    }

    /**
     * Determines the appropriate WindowManager layout type based on the Android version.
     *
     * @return The WindowManager layout type (TYPE_APPLICATION_PANEL for API 26+, TYPE_PHONE otherwise).
     */
    private int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    /**
     * Retrieves the height of the visible display area.
     *
     * @return The screen height in pixels.
     */
    private int getScreenHeight() {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(dm);
        return dm.heightPixels;
    }

    /**
     * Retrieves the width of the screen.
     *
     * @return The screen width in pixels.
     */
    private int getScreenWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * Helper to get the height of the system navigation bar using official WindowInsets APIs.
     */
    private int getNavigationBarHeight() {
        // API 30+ (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return windowManager.getCurrentWindowMetrics()
                    .getWindowInsets()
                    .getInsets(WindowInsets.Type.navigationBars())
                    .bottom;
        }
        // API 23+ (Android +)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (context != null && context.getWindow() != null) {
                WindowInsets insets = context.getWindow().getDecorView().getRootWindowInsets();
                if (insets != null) {
                    return insets.getSystemWindowInsetBottom();
                }
            }
        }

        // Fallback for very old devices or if insets are not yet available:
        // Calculate the difference between the physical screen height and the usable screen height.
        DisplayMetrics realMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(realMetrics);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        if (realMetrics.heightPixels > displayMetrics.heightPixels) {
            return realMetrics.heightPixels - displayMetrics.heightPixels;
        }

        return 0;
    }

    /**
     * Checks if the device is in portrait orientation.
     *
     * @return True if the screen height is greater than the width, false otherwise.
     */
    private boolean isPortrait() {
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels > dm.widthPixels;
    }

    /**
     * Initializes the overlay view by inflating the layout and configuring the TextView.
     * <p>
     * This method is called lazily when the overlay is first shown to avoid unnecessary resource usage.
     */
    private void initializeOverlayView() {
        if (overlayView != null) return;

        try {
            // Load the layout resource for the subtitle overlay
            int layoutId = ResourceUtils.getLayoutIdentifier("revanced_subtitle_overlay_layout");
            if (layoutId == 0) {
                throw new RuntimeException("Layout 'revanced_subtitle_overlay_layout' not found.");
            }
            LayoutInflater inflater = LayoutInflater.from(context);
            overlayView = inflater.inflate(layoutId, null);

            // Locate the TextView within the layout
            int textViewId = ResourceUtils.getIdIdentifier("subtitle_text_view");
            if (textViewId == 0) {
                throw new RuntimeException("TextView with ID 'subtitle_text_view' not found.");
            }

            subtitleTextView = Objects.requireNonNull(overlayView).findViewById(textViewId);
            configureTextView();

            // Force initial measurement to ensure correct sizing
            int widthSpec = View.MeasureSpec.makeMeasureSpec(getScreenWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            overlayView.measure(widthSpec, heightSpec);
            overlayView.layout(0, 0, overlayView.getMeasuredWidth(), overlayView.getMeasuredHeight());

            // Set up touch and gesture handling
            setupGestureDetection();
        } catch (Exception e) {
            Logger.printException(() -> "Failed to initialize subtitle overlay view", e);
            overlayView = null;
            subtitleTextView = null;
        }
    }

    /**
     * Configures the TextView's appearance, including background, padding, and text properties.
     */
    private void configureTextView() {
        if (subtitleTextView == null) return;

        // Set up a semi-transparent black background with rounded corners
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#CC000000")); // Semi-transparent black
        background.setCornerRadius(dipToPixels(CORNER_RADIUS_DP));
        subtitleTextView.setBackground(background);

        // Configure padding and text appearance
        int padding = dipToPixels(8);
        int horizontalPadding = dipToPixels(16);
        subtitleTextView.setPadding(horizontalPadding, padding, horizontalPadding, padding);
        subtitleTextView.setTextColor(Color.WHITE);
        subtitleTextView.setGravity(Gravity.CENTER);
        subtitleTextView.setTextSize(Settings.GEMINI_TRANSCRIBE_SUBTITLES_FONT_SIZE.get());
    }

    /**
     * Sets up gesture detection for handling taps and long presses on the overlay.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupGestureDetection() {
        if (overlayView == null) return;
        gestureDetector = new GestureDetector(context, new OverlayGestureListener());

        // Attach touch listener to handle gestures and dragging
        overlayView.setOnTouchListener((view, event) -> {
            gestureDetector.onTouchEvent(event);
            if (isDragging) handleDragEvent(event);
            return true;
        });
    }

    /**
     * Handles drag events to move the overlay vertically.
     *
     * @param event The MotionEvent triggered by user interaction.
     */
    private void handleDragEvent(MotionEvent event) {
        if (!isDragging || overlayView == null) return;

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                // Calculate new Y position, constrained within screen bounds
                float newAbsoluteTopY = event.getRawY() - dragOffset;
                int screenHeight = getScreenHeight();
                int overlayHeight = overlayView.getHeight();
                int minY = dipToPixels(MARGIN_DP);
                int bottomMargin = dipToPixels(MARGIN_DP) + getNavigationBarHeight();
                int maxY = screenHeight - overlayHeight - bottomMargin;

                newAbsoluteTopY = Math.max(minY, Math.min(newAbsoluteTopY, maxY));

                layoutParams.y = (int) newAbsoluteTopY;
                updateOverlayPosition();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Stop dragging and snap to the nearest edge
                isDragging = false;
                snapToEdge();

                // Animate a slight scale-down effect
                overlayView.setPivotX(overlayView.getWidth() / 2f);
                overlayView.setPivotY(overlayView.getHeight() / 2f);
                ValueAnimator backAnim = ValueAnimator.ofFloat(1.05f, 1.0f);
                backAnim.setDuration(200);
                backAnim.setInterpolator(new OvershootInterpolator(2f));
                backAnim.addUpdateListener(anim -> {
                    float scale = (float) anim.getAnimatedValue();
                    overlayView.setScaleX(scale);
                    overlayView.setScaleY(scale);
                });
                backAnim.start();

                Logger.printDebug(() -> "Stopped dragging subtitle overlay.");
                break;
        }
    }

    /**
     * Updates the overlay's position in the WindowManager.
     */
    private void updateOverlayPosition() {
        if (overlayView == null || !isShowing) return;
        try {
            windowManager.updateViewLayout(overlayView, layoutParams);
        } catch (Exception e) {
            Logger.printException(() -> "Failed to update overlay position", e);
        }
    }

    /**
     * Snaps the overlay to the top or bottom edge of the screen with an animation.
     */
    private void snapToEdge() {
        if (overlayView == null || !isShowing) return;

        int screenHeight = getScreenHeight();
        int overlayHeight = overlayView.getHeight();
        int overlayAbsoluteTopY = layoutParams.y;
        int overlayCenterY = overlayAbsoluteTopY + overlayHeight / 2;

        // Determine whether to snap to the top or bottom based on the overlay's center
        boolean snapToTop = overlayCenterY < screenHeight / 2;

        // Calculate target position, accounting for margins and orientation
        int margin = dipToPixels(MARGIN_DP);
        int targetAbsoluteTopY;

        if (snapToTop) {
            targetAbsoluteTopY = margin + (isPortrait() ? dipToPixels(24) : 0);
        } else {
            targetAbsoluteTopY = screenHeight - overlayHeight - margin - getNavigationBarHeight();
        }

        // Animate the snapping motion
        ValueAnimator animator = ValueAnimator.ofInt(overlayAbsoluteTopY, targetAbsoluteTopY);
        animator.setDuration(SNAP_ANIMATION_DURATION_MS);
        animator.setInterpolator(new OvershootInterpolator(SNAP_OVERSHOOT_TENSION));
        animator.addUpdateListener(animation -> {
            layoutParams.y = (int) animation.getAnimatedValue();
            updateOverlayPosition();
        });

        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                layoutParams.y = targetAbsoluteTopY;
                updateOverlayPosition();
                isAtBottom = !snapToTop;
                savePosition();
                Logger.printDebug(() -> "Subtitle overlay snapped to " + (snapToTop ? "top" : "bottom"));
            }

            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationCancel(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    /**
     * Displays the subtitle overlay window.
     * <p>
     * Initializes the overlay view if not already created and adds it to the WindowManager.
     * The overlay is positioned based on the saved preference (top or bottom).
     */
    public void show() {
        mainHandler.post(() -> {
            if (isShowing) return;
            initializeOverlayView();
            if (overlayView == null || windowManager == null) {
                Logger.printException(() -> "Cannot show overlay: View or WindowManager is null.");
                return;
            }
            try {
                isAtBottom = loadPosition();
                int screenHeight = getScreenHeight();
                int overlayHeight = overlayView.getHeight();
                int margin = dipToPixels(MARGIN_DP);

                // Set initial position based on isAtBottom
                if (isAtBottom) {
                    layoutParams.y = screenHeight - overlayHeight - margin - getNavigationBarHeight();
                } else {
                    layoutParams.y = margin + (isPortrait() ? dipToPixels(24) : 0);
                }

                windowManager.addView(overlayView, layoutParams);
                isShowing = true;
                Logger.printDebug(() -> "Subtitle overlay added to window at " + (isAtBottom ? "bottom" : "top") + " position.");
            } catch (WindowManager.BadTokenException e) {
                Logger.printException(() -> "WindowManager BadTokenException - Check SYSTEM_ALERT_WINDOW permission?", e);
                showToastLong(str("revanced_gemini_transcribe_bad_token_exception"));
            } catch (Exception e) {
                Logger.printException(() -> "Failed to add subtitle overlay view", e);
            }
        });
    }

    /**
     * Hides the subtitle overlay window.
     * <p>
     * Removes the overlay view from the WindowManager if it is currently showing.
     */
    public void hide() {
        mainHandler.post(() -> {
            if (!isShowing || overlayView == null || windowManager == null) return;
            try {
                windowManager.removeView(overlayView);
                isShowing = false;
                Logger.printDebug(() -> "Subtitle overlay removed from window.");
            } catch (IllegalArgumentException e) {
                // This can happen if the activity is destroyed while the overlay is showing.
                Logger.printInfo(() -> "Attempted to remove unattached overlay view.");
                isShowing = false;
            } catch (Exception e) {
                Logger.printException(() -> "Failed to remove subtitle overlay view", e);
            }
        });
    }

    /**
     * Updates the text displayed in the subtitle overlay.
     *
     * @param text The text to display, or null/empty to hide the TextView.
     */
    public void updateText(@Nullable final String text) {
        mainHandler.post(() -> {
            if (!isShowing || subtitleTextView == null || overlayView == null) return;

            // Store the current height to detect changes
            int oldHeight = overlayView.getHeight();

            // Update text and visibility
            if (text == null || text.isEmpty()) {
                subtitleTextView.setVisibility(View.INVISIBLE);
            } else {
                subtitleTextView.setText(text);
                subtitleTextView.setVisibility(View.VISIBLE);
            }

            // Re-measure the view to account for text changes
            int widthSpec = View.MeasureSpec.makeMeasureSpec(getScreenWidth(), View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            overlayView.measure(widthSpec, heightSpec);
            overlayView.layout(0, 0, overlayView.getMeasuredWidth(), overlayView.getMeasuredHeight());

            // Adjust position if height changes and the overlay is at the bottom
            int newHeight = overlayView.getMeasuredHeight();
            if (oldHeight != newHeight) {
                if (isAtBottom) {
                    int margin = dipToPixels(MARGIN_DP);
                    layoutParams.y = getScreenHeight() - newHeight - margin - getNavigationBarHeight();
                    updateOverlayPosition();
                }
            }
        });
    }

    /**
     * Destroys the subtitle overlay and releases resources.
     * <p>
     * Hides the overlay and nullifies references to the view and TextView.
     */
    public void destroy() {
        mainHandler.post(() -> {
            hide();
            overlayView = null;
            subtitleTextView = null;
            Logger.printDebug(() -> "Subtitle overlay resources released.");
        });
    }

    /**
     * Returns the root view of the subtitle overlay.
     *
     * @return The overlay view, or null if not initialized.
     */
    @Nullable
    public View getOverlayView() {
        return overlayView;
    }

    /**
     * Saves the current subtitle position to SharedPreferences.
     */
    private void savePosition() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_AT_BOTTOM, isAtBottom).apply();
        Logger.printDebug(() -> "Saved subtitle position: isAtBottom = " + isAtBottom);
    }

    /**
     * Loads the saved subtitle position from SharedPreferences.
     *
     * @return True if the overlay should be positioned at the bottom, false for top.
     */
    private boolean loadPosition() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean defaultPosition = true; // Default to bottom if no preference is saved
        boolean loadedPosition = prefs.getBoolean(KEY_IS_AT_BOTTOM, defaultPosition);
        Logger.printDebug(() -> "Loaded subtitle position: isAtBottom = " + loadedPosition);
        return loadedPosition;
    }

    /**
     * Gesture listener for handling tap and long-press events on the subtitle overlay.
     */
    private class OverlayGestureListener extends GestureDetector.SimpleOnGestureListener {
        /**
         * Called when a touch event starts.
         *
         * @param e The MotionEvent.
         * @return True to indicate the event was handled.
         */
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        /**
         * Called when a single tap is released.
         * Hides the overlay on tap.
         *
         * @param e The MotionEvent.
         * @return True to indicate the event was handled.
         */
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            Logger.printDebug(() -> "Subtitle overlay clicked, hiding.");
            hide();
            return true;
        }

        /**
         * Called on a long press.
         * Initiates dragging mode and applies a slight scale-up animation.
         *
         * @param e The MotionEvent.
         */
        @Override
        public void onLongPress(MotionEvent e) {
            if (!isShowing || overlayView == null) return;
            isDragging = true;
            dragOffset = e.getRawY() - layoutParams.y;

            // Apply a scale-up animation to indicate dragging
            overlayView.setPivotX(overlayView.getWidth() / 2f);
            overlayView.setPivotY(overlayView.getHeight() / 2f);
            ValueAnimator scaleAnim = ValueAnimator.ofFloat(1.0f, 1.05f);
            scaleAnim.setDuration(200);
            scaleAnim.setInterpolator(new OvershootInterpolator(2f));
            scaleAnim.addUpdateListener(anim -> {
                float scale = (float) anim.getAnimatedValue();
                overlayView.setScaleX(scale);
                overlayView.setScaleY(scale);
            });
            scaleAnim.start();
            Logger.printDebug(() -> "Started dragging subtitle overlay.");
        }
    }
}
