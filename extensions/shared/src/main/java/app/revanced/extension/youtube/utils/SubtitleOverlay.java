package app.revanced.extension.youtube.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.youtube.settings.Settings;

import java.util.Objects;

import static app.revanced.extension.shared.utils.StringRef.str;
import static app.revanced.extension.shared.utils.Utils.showToastLong;

/**
 * Basic conceptual implementation of a subtitle overlay using WindowManager.
 * WARNING: Requires SYSTEM_ALERT_WINDOW permission on Android M+
 * Needs significant refinement for production use (layout, positioning, error handling).
 */
@SuppressWarnings("deprecation")
public class SubtitleOverlay {

    private final Context context;
    private final WindowManager windowManager;
    @Nullable private View overlayView;
    @Nullable private TextView subtitleTextView;
    private final WindowManager.LayoutParams params;
    private boolean isShowing = false;
    private final Handler mainHandler;

    public SubtitleOverlay(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100; // Offset from the absolute bottom
    }

    private int getOverlayType() {
        // Choose appropriate overlay type based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Requires SYSTEM_ALERT_WINDOW permission
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            // Deprecated but works on older versions (might still need permission)
            // TYPE_SYSTEM_ALERT or TYPE_PHONE based on needs/permissions
            return WindowManager.LayoutParams.TYPE_PHONE; // Often requires permission
        }
    }

    private void ensureViewInflated() {
        if (overlayView == null) {
            try {
                int layoutId = ResourceUtils.getLayoutIdentifier("revanced_subtitle_overlay_layout");
                if (layoutId == 0) {
                    throw new RuntimeException("Layout 'revanced_subtitle_overlay_layout' not found.");
                }
                LayoutInflater inflater = LayoutInflater.from(context);
                overlayView = inflater.inflate(layoutId, null);

                int textViewId = ResourceUtils.getIdIdentifier("subtitle_text_view");
                if (textViewId == 0) {
                    throw new RuntimeException("TextView with ID 'subtitle_text_view' not found in layout.");
                }
                subtitleTextView = Objects.requireNonNull(overlayView).findViewById(textViewId);

                if (subtitleTextView != null) {
                    subtitleTextView.setBackgroundColor(Color.parseColor("#CC000000"));
                    subtitleTextView.setTextColor(Color.WHITE);
                    subtitleTextView.setPadding(16, 8, 16, 8);
                    subtitleTextView.setGravity(Gravity.CENTER);
                    subtitleTextView.setTextSize(Settings.GEMINI_TRANSCRIBE_SUBTITLES_FONT_SIZE.get());
                }

                if (overlayView != null) {
                    overlayView.setOnClickListener(v -> {
                        Logger.printDebug(() -> "Subtitle overlay clicked, hiding.");
                        hide();
                    });
                }
            } catch (Exception e) {
                Logger.printException(() -> "Failed to inflate subtitle overlay view", e);
                overlayView = null;
                subtitleTextView = null;
            }
        }
    }

    /** Displays the overlay window */
    public void show() {
        mainHandler.post(() -> {
            if (isShowing) return;
            ensureViewInflated();
            if (overlayView == null || windowManager == null) {
                Logger.printException(() -> "Cannot show overlay: View or WindowManager is null.");
                return;
            }
            try {
                windowManager.addView(overlayView, params);
                isShowing = true;
                Logger.printDebug(() -> "Subtitle overlay added to window.");
            } catch (WindowManager.BadTokenException e) {
                Logger.printException(()-> "WindowManager BadTokenException - Can't add overlay view. Check SYSTEM_ALERT_WINDOW permission?", e);
                showToastLong(str("revanced_gemini_transcribe_bad_token_exception"));
            } catch (Exception e) {
                Logger.printException(() -> "Failed to add subtitle overlay view", e);
            }
        });
    }

    /** Hides the overlay window */
    public void hide() {
        mainHandler.post(() -> {
            if (!isShowing || overlayView == null || windowManager == null) return;
            try {
                windowManager.removeView(overlayView);
                isShowing = false;
                Logger.printDebug(() -> "Subtitle overlay removed from window.");
            } catch (IllegalArgumentException e) {
                Logger.printInfo(() -> "Attempted to remove overlay view that was not attached?");
                isShowing = false;
            } catch (Exception e) {
                Logger.printException(() -> "Failed to remove subtitle overlay view", e);
            }
        });
    }

    /** Updates the text displayed in the subtitle overlay */
    public void updateText(@Nullable final String text) {
        mainHandler.post(() -> {
            if (!isShowing || subtitleTextView == null) return;
            // Update visibility based on text presence
            if (text == null || text.isEmpty()) {
                subtitleTextView.setVisibility(View.INVISIBLE);
            } else {
                subtitleTextView.setText(text);
                subtitleTextView.setVisibility(View.VISIBLE);
            }
        });
    }

    // Optional: Method to fully release resources if needed
    public void destroy() {
        hide();
        overlayView = null;
        subtitleTextView = null;
        Logger.printDebug(()-> "Subtitle overlay resources released.");
    }
}
