package app.revanced.extension.spotify.misc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;
import app.revanced.extension.shared.utils.Utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Manages the display and interaction of a floating lyrics search overlay
 * within the Spotify application. Allows minimizing, expanding, and dragging.
 * The minimized state snaps to the left or right edge, preserving vertical position.
 * The expanded state always appears centered horizontally near the bottom.
 * It listens for track metadata updates and provides buttons to search for
 * lyrics on Google or minimize/expand the overlay.
 */
@SuppressWarnings("unused")
public class LyricsSearchManager {

    private static final String TAG = "LyricsSearchManager";
    private static final Object buttonLock = new Object();
    private static final int EDGE_PADDING_DP = 8; // Padding from screen edge when snapped
    private static final int DEFAULT_BOTTOM_MARGIN_DP = 50;
    private static String currentSearchTitle = null;
    private static String currentSearchArtist = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile FrameLayout lyricsButtonContainer = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile LinearLayout expandedLayout = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageButton minimizeButton = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile Button searchButton = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageButton expandButton = null;
    private static volatile OverlayState currentOverlayState = OverlayState.EXPANDED;
    private static volatile SnapEdge lastSnappedEdge = SnapEdge.RIGHT;
    private static volatile boolean isDragging = false;
    private static volatile float initialTouchX, initialTouchY;
    private static volatile int initialX, initialY;
    private static volatile int lastKnownTopMarginForMinimized = -1;

    /**
     * Safely updates the current track information and triggers the display
     * or removal of the lyrics button overlay based on the validity of the data.
     * This method is expected to be called when track metadata or player state changes.
     *
     * @param titleObj  The potential title of the track (expected String).
     * @param artistObj The potential artist of the track (expected String).
     */
    public static void processTrackMetadata(Object titleObj, Object artistObj) {
        String title = null;
        if (titleObj instanceof String tempTitle && !tempTitle.isEmpty()) {
            title = tempTitle;
        }

        String artist = null;
        if (artistObj instanceof String tempArtist && !tempArtist.isEmpty()) {
            artist = tempArtist;
        }

        if (title != null && artist != null) {
            synchronized (buttonLock) {
                boolean metadataChanged = !title.equals(currentSearchTitle) || !artist.equals(currentSearchArtist);
                currentSearchTitle = title;
                currentSearchArtist = artist;

                if (metadataChanged) {
                    String finalTitle = title;
                    String finalArtist = artist;
                    Logger.printDebug(() -> TAG + "Updated track info: Title=[" + finalTitle + "], Artist=[" + finalArtist + "]");
                    Utils.runOnMainThreadNowOrLater(LyricsSearchManager::showOrUpdateOverlay);
                } else {
                    Logger.printDebug(() -> TAG + "Track info same, ensuring overlay state.");
                    Utils.runOnMainThreadNowOrLater(LyricsSearchManager::ensureOverlayState);
                }
            }
        } else {
            Logger.printInfo(() -> TAG + "safePrintMetadata received invalid title or artist. Removing button.");
            Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeOverlay);
            synchronized (buttonLock) {
                lastSnappedEdge = SnapEdge.RIGHT;
                lastKnownTopMarginForMinimized = -1;
                currentOverlayState = OverlayState.EXPANDED;
            }
        }
    }

    /**
     * Ensures the overlay is in the correct state (visibility, position)
     * without necessarily recreating it. Called when metadata hasn't changed,
     * but we want to ensure it's displayed correctly (e.g., after rotation).
     */
    private static void ensureOverlayState() {
        synchronized (buttonLock) {
            if (lyricsButtonContainer != null && lyricsButtonContainer.getParent() != null) {
                Logger.printDebug(() -> TAG + "Ensuring overlay state is correct.");
                updateVisibilityBasedOnState();
                applyLayoutParamsForState();
            } else if (lyricsButtonContainer != null) {
                // Existed but not attached, re-attach using showOrUpdate
                Logger.printDebug(() -> TAG + "Ensure state found detached overlay, re-attaching.");
                showOrUpdateOverlay();
            } else {
                // Doesn't exist, maybe it was removed previously. Recreate if metadata is valid
                if (currentSearchTitle != null && currentSearchArtist != null) {
                    Logger.printDebug(() -> TAG + "Ensure state found no overlay, recreating.");
                    showOrUpdateOverlay();
                }
            }
        }
    }

    /**
     * Creates and displays/updates the overlay on the current Activity's DecorView.
     * This method MUST be called on the main UI thread.
     */
    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    private static void showOrUpdateOverlay() {
        Activity currentActivity = Utils.getActivity();
        if (currentActivity == null) {
            Logger.printInfo(() -> TAG + "Cannot show overlay: No current Activity available.");
            removeOverlayInternal();
            return;
        }

        synchronized (buttonLock) {
            if (lyricsButtonContainer != null && lyricsButtonContainer.getParent() == null) {
                Logger.printDebug(() -> TAG + "Overlay existed but wasn't attached. Re-attaching.");
                ViewGroup decorView = getDecorView(currentActivity);
                if (decorView != null) {
                    try {
                        FrameLayout.LayoutParams params = getLayoutParamsForCurrentState(decorView.getContext());
                        decorView.addView(lyricsButtonContainer, params);
                        Logger.printInfo(() -> TAG + "Lyrics overlay re-added to DecorView.");
                        updateVisibilityBasedOnState();
                    } catch (Exception e) {
                        Logger.printException(() -> TAG + "Error re-adding overlay to DecorView", e);
                        removeOverlayInternal();
                    }
                } else {
                    removeOverlayInternal();
                }
                return;

            } else if (lyricsButtonContainer != null) {

                Logger.printDebug(() -> TAG + "Overlay already visible. Ensuring state.");
                ensureOverlayState();
                return;
            }

            Logger.printDebug(() -> TAG + "Creating and adding lyrics overlay to DecorView...");

            lyricsButtonContainer = new FrameLayout(currentActivity);

            expandedLayout = new LinearLayout(currentActivity);
            expandedLayout.setOrientation(LinearLayout.HORIZONTAL);
            expandedLayout.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable expandedBg = new GradientDrawable();
            expandedBg.setColor(Color.argb(200, 40, 40, 40));
            expandedBg.setCornerRadius(Utils.dpToPx(25));
            expandedLayout.setBackground(expandedBg);
            expandedLayout.setPadding(Utils.dpToPx(5), Utils.dpToPx(5), Utils.dpToPx(5), Utils.dpToPx(5));

            int buttonHeightPx = Utils.dpToPx(40);
            int iconButtonSizePx = Utils.dpToPx(40);

            searchButton = new Button(currentActivity);
            searchButton.setText("Search Lyrics");
            searchButton.setAllCaps(false);
            searchButton.setTextColor(Color.BLACK);
            searchButton.setPadding(Utils.dpToPx(16), 0, Utils.dpToPx(16), 0);
            GradientDrawable searchBg = new GradientDrawable();
            searchBg.setShape(GradientDrawable.RECTANGLE);
            searchBg.setColor(Color.parseColor("#1ED760"));
            searchBg.setCornerRadius(Utils.dpToPx(20));
            searchButton.setBackground(searchBg);
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    buttonHeightPx
            );
            searchButton.setLayoutParams(searchParams);
            searchButton.setOnClickListener(v -> {
                Logger.printDebug(() -> TAG + "Search Lyrics button clicked.");
                synchronized (buttonLock) {
                    if (currentSearchTitle != null && currentSearchArtist != null) {
                        launchLyricsSearch(currentSearchTitle, currentSearchArtist);
                    } else {
                        Logger.printInfo(() -> TAG + "Search button clicked but title/artist info missing.");
                        Utils.showToastShort("Track info not available.");
                    }
                }
            });

            minimizeButton = new ImageButton(currentActivity);
            minimizeButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            minimizeButton.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            int paddingPx = Utils.dpToPx(8);
            minimizeButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            GradientDrawable minimizeBg = new GradientDrawable();
            minimizeBg.setShape(GradientDrawable.OVAL);
            minimizeBg.setColor(Color.parseColor("#404040"));
            minimizeBg.setSize(iconButtonSizePx, iconButtonSizePx);
            minimizeButton.setBackground(minimizeBg);
            LinearLayout.LayoutParams minimizeParams = new LinearLayout.LayoutParams(
                    iconButtonSizePx,
                    iconButtonSizePx
            );
            minimizeParams.setMarginStart(Utils.dpToPx(5));
            minimizeButton.setLayoutParams(minimizeParams);
            minimizeButton.setOnClickListener(v -> {
                Logger.printDebug(() -> TAG + "Minimize button clicked.");
                minimizeOverlay();
            });

            expandedLayout.addView(searchButton);
            expandedLayout.addView(minimizeButton);

            int searchIconId = ResourceUtils.getDrawableIdentifier("encore_icon_search_24");

            expandButton = new ImageButton(currentActivity);
            expandButton.setImageResource(searchIconId);
            expandButton.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            expandButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            GradientDrawable expandBg = new GradientDrawable();
            expandBg.setShape(GradientDrawable.OVAL);
            expandBg.setColor(Color.parseColor("#1ED760"));
            expandBg.setSize(iconButtonSizePx, iconButtonSizePx);
            expandButton.setBackground(expandBg);

            final ViewConfiguration viewConfig = ViewConfiguration.get(currentActivity);
            final int touchSlop = viewConfig.getScaledTouchSlop();

            expandButton.setOnTouchListener((view, event) -> {

                if (currentOverlayState != OverlayState.MINIMIZED) return false;

                final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lyricsButtonContainer.getLayoutParams();
                final ViewGroup parentView = getDecorView(Utils.getActivity());
                if (parentView == null) return false;

                final int action = event.getActionMasked();
                final int edgePaddingPx = Utils.dpToPx(EDGE_PADDING_DP);

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.leftMargin;
                        initialY = params.topMargin;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isDragging = false;
                        if (lyricsButtonContainer.getParent() != null) {
                            lyricsButtonContainer.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;

                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true;
                            Logger.printDebug(() -> TAG + "Dragging started.");
                        }

                        if (isDragging) {
                            int newY = initialY + (int) dy;
                            int newX = initialX + (int) dx;

                            // Clamp vertical position within parent bounds
                            int viewHeight = lyricsButtonContainer.getHeight();
                            newY = Math.max(edgePaddingPx, Math.min(newY, parentView.getHeight() - viewHeight - edgePaddingPx));

                            // Update margins directly for smooth dragging
                            params.topMargin = newY;
                            params.leftMargin = newX;
                            params.gravity = Gravity.NO_GRAVITY;
                            lyricsButtonContainer.setLayoutParams(params);
                            lastKnownTopMarginForMinimized = newY;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (lyricsButtonContainer.getParent() != null) {
                            lyricsButtonContainer.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        if (!isDragging) {
                            // Treat as a click -> Expand
                            Logger.printDebug(() -> TAG + "Expand button clicked (no drag).");
                            expandOverlay();
                        } else {
                            // Drag finished -> Snap to Edge horizontally, keep vertical pos
                            Logger.printDebug(() -> TAG + "Dragging finished. Snapping to edge.");
                            snapToNearestEdge(lyricsButtonContainer, parentView, params);
                        }
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        if (lyricsButtonContainer.getParent() != null) {
                            lyricsButtonContainer.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        isDragging = false;
                        // Snap back if dragging was cancelled
                        snapToNearestEdge(lyricsButtonContainer, parentView, params);
                        return true;
                }
                return false;
            });

            // --- Add Views to Container ---
            FrameLayout.LayoutParams expandedParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            expandedParams.gravity = Gravity.CENTER;
            lyricsButtonContainer.addView(expandedLayout, expandedParams);

            FrameLayout.LayoutParams minimizedParams = new FrameLayout.LayoutParams(
                    iconButtonSizePx, iconButtonSizePx);
            minimizedParams.gravity = Gravity.CENTER;
            lyricsButtonContainer.addView(expandButton, minimizedParams);

            // --- Add the Container to the Activity's Decor View ---
            ViewGroup decorView = getDecorView(currentActivity);
            if (decorView == null) {
                Logger.printException(() -> TAG + "Could not get Decor View!");
                removeOverlayInternal();
                return;
            }

            // Set initial state (EXPANDED by default now)
            FrameLayout.LayoutParams containerParams = getLayoutParamsForCurrentState(currentActivity);

            try {
                decorView.addView(lyricsButtonContainer, containerParams);
                Logger.printInfo(() -> TAG + "Lyrics overlay added to DecorView (Initial state: " + currentOverlayState + ").");
                updateVisibilityBasedOnState();
            } catch (Exception e) {
                Logger.printException(() -> TAG + "Error adding lyrics overlay to DecorView", e);
                removeOverlayInternal();
            }
        }
    }

    private static ViewGroup getDecorView(Activity activity) {
        if (activity == null || activity.getWindow() == null) return null;
        View decor = activity.getWindow().getDecorView();
        return (decor instanceof ViewGroup) ? (ViewGroup) decor : null;
    }

    /**
     * Gets appropriate LayoutParams based on the current state (Minimized/Snapped or Expanded/Centered-Bottom)
     */
    private static FrameLayout.LayoutParams getLayoutParamsForCurrentState(Context context) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int iconSizePx = Utils.dpToPx(40);
        int edgePaddingPx = Utils.dpToPx(EDGE_PADDING_DP);
        int defaultTopMarginMinimized = Utils.dpToPx(100);

        // Get screen width for snapping logic
        int parentWidth;
        ViewGroup decorView = getDecorView(Utils.getActivity());
        if (decorView != null && decorView.getWidth() > 0) {
            parentWidth = decorView.getWidth();
        } else if (context != null) {
            // Fallback to display metrics if DecorView width isn't available
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            parentWidth = displayMetrics.widthPixels;
        } else {
            // Last resort guess (avoid division by zero)
            parentWidth = 1080;
            int finalParentWidth1 = parentWidth;
            Logger.printInfo(() -> TAG + "Could not get accurate parent width, using default: " + finalParentWidth1);
        }

        if (currentOverlayState == OverlayState.MINIMIZED) {
            // --- MINIMIZED STATE ---
            // Position based on last known drag position (vertical) and snapped edge (horizontal)
            params.width = iconSizePx;
            params.height = iconSizePx;
            params.gravity = Gravity.NO_GRAVITY;

            // Use last known vertical position, or a default
            int currentTopMargin = (lastKnownTopMarginForMinimized != -1) ? lastKnownTopMarginForMinimized : defaultTopMarginMinimized;
            params.topMargin = Math.max(edgePaddingPx, currentTopMargin);

            // Snap horizontally to last known edge
            if (lastSnappedEdge == SnapEdge.LEFT) {
                params.leftMargin = edgePaddingPx;
                Logger.printDebug(() -> TAG + "Layout Params: Minimized, Snap LEFT, Top: " + params.topMargin);
            } else {
                params.leftMargin = Math.max(edgePaddingPx, parentWidth - iconSizePx - edgePaddingPx);
                Logger.printDebug(() -> TAG + "Layout Params: Minimized, Snap RIGHT (ParentW: " + parentWidth + "), Top: " + params.topMargin);
            }

            params.bottomMargin = 0;
        } else {
            // --- EXPANDED STATE ---
            // Always position centered horizontally near the bottom, ignore drag position
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = Utils.dpToPx(DEFAULT_BOTTOM_MARGIN_DP);

            // Clear margins as gravity handles positioning
            params.leftMargin = 0;
            params.topMargin = 0;

            Logger.printDebug(() -> TAG + "Layout Params: Expanded, Bottom Center");
        }

        return params;
    }

    /**
     * Snaps the container to the nearest edge (left or right) based on its center point, preserving vertical position
     */
    private static void snapToNearestEdge(FrameLayout container, ViewGroup parentView, FrameLayout.LayoutParams params) {
        if (container == null || parentView == null || params == null || currentOverlayState != OverlayState.MINIMIZED)
            return;

        int parentWidth = parentView.getWidth();
        if (parentWidth <= 0 && container.getContext() != null) {
            parentWidth = container.getContext().getResources().getDisplayMetrics().widthPixels;
        }
        if (parentWidth <= 0) {
            Logger.printInfo(() -> TAG + "Cannot snap, parent width is zero.");
            return;
        }

        int viewWidth = container.getWidth();

        int currentLeftMargin = params.leftMargin;
        int viewCenterX = currentLeftMargin + viewWidth / 2;
        int edgePaddingPx = Utils.dpToPx(EDGE_PADDING_DP);

        params.gravity = Gravity.NO_GRAVITY;

        params.topMargin = lastKnownTopMarginForMinimized;
        params.bottomMargin = 0;

        // Determine nearest edge
        if (viewCenterX < parentWidth / 2) {
            // Snap Left
            params.leftMargin = edgePaddingPx;
            lastSnappedEdge = SnapEdge.LEFT;
            Logger.printDebug(() -> TAG + "Snapped to LEFT edge. Top: " + params.topMargin);
        } else {
            // Snap Right
            params.leftMargin = Math.max(edgePaddingPx, parentWidth - viewWidth - edgePaddingPx);
            lastSnappedEdge = SnapEdge.RIGHT;
            Logger.printDebug(() -> TAG + "Snapped to RIGHT edge. Top: " + params.topMargin);
        }

        container.setLayoutParams(params);
    }

    /**
     * Re-applies the layout params based on the current state
     */
    private static void applyLayoutParamsForState() {
        Utils.runOnMainThreadNowOrLater(() -> {
            synchronized (buttonLock) {
                if (lyricsButtonContainer != null && lyricsButtonContainer.getParent() != null) {
                    Context context = lyricsButtonContainer.getContext();
                    FrameLayout.LayoutParams params = getLayoutParamsForCurrentState(context);
                    lyricsButtonContainer.setLayoutParams(params);
                    Logger.printDebug(() -> TAG + "Applied layout params for state: " + currentOverlayState);

                    lyricsButtonContainer.requestLayout();
                }
            }
        });
    }

    private static void minimizeOverlay() {
        synchronized (buttonLock) {
            if (currentOverlayState == OverlayState.MINIMIZED) return;
            Logger.printDebug(() -> TAG + "Minimizing overlay...");

            currentOverlayState = OverlayState.MINIMIZED;
            updateVisibilityBasedOnState();
            applyLayoutParamsForState();
        }
    }

    private static void expandOverlay() {
        synchronized (buttonLock) {
            if (currentOverlayState == OverlayState.EXPANDED) return;
            Logger.printDebug(() -> TAG + "Expanding overlay...");
            currentOverlayState = OverlayState.EXPANDED;
            updateVisibilityBasedOnState();
            applyLayoutParamsForState();
        }
    }

    private static void updateVisibilityBasedOnState() {
        if (lyricsButtonContainer == null) return;

        Utils.runOnMainThreadNowOrLater(() -> {
            synchronized (buttonLock) {
                if (expandedLayout == null || expandButton == null) return;

                if (currentOverlayState == OverlayState.EXPANDED) {
                    expandedLayout.setVisibility(View.VISIBLE);
                    expandButton.setVisibility(View.GONE);
                    lyricsButtonContainer.setOnTouchListener(null);
                } else {
                    expandedLayout.setVisibility(View.GONE);
                    expandButton.setVisibility(View.VISIBLE);
                }
                Logger.printDebug(() -> TAG + "Visibility updated for state: " + currentOverlayState);
            }
        });
    }

    /**
     * Schedules the removal of the lyrics overlay from the UI thread.
     */
    public static void removeOverlay() {
        Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeOverlayInternal);
    }

    /**
     * Removes the lyrics overlay from its parent view immediately.
     * This method MUST be called on the main UI thread.
     */
    private static void removeOverlayInternal() {
        synchronized (buttonLock) {
            View containerToRemove = lyricsButtonContainer;
            if (containerToRemove != null && containerToRemove.getParent() instanceof ViewGroup parent) {
                Logger.printDebug(() -> TAG + "Removing lyrics overlay from parent: " + parent.getClass().getName());
                try {
                    parent.removeView(containerToRemove);
                    Logger.printInfo(() -> TAG + "Lyrics overlay removed.");
                } catch (Exception e) {
                    Logger.printException(() -> TAG + "Error removing lyrics overlay", e);
                }
            } else if (containerToRemove != null) {
                Logger.printDebug(() -> TAG + "Overlay found but parent is null or not ViewGroup. Already removed?");
            }

            lyricsButtonContainer = null;
            expandedLayout = null;
            searchButton = null;
            minimizeButton = null;
            expandButton = null;
        }
    }

    /**
     * Launches a browser intent to search Google for lyrics of the given track.
     *
     * @param title  The track title.
     * @param artist The track artist.
     */
    private static void launchLyricsSearch(String title, String artist) {
        Context context = Utils.getContext();
        if (context == null) {
            Activity act = Utils.getActivity();
            if (act != null) context = act;
        }

        if (context == null) {
            Logger.printException(() -> TAG + "Cannot launch browser: Context is null.");
            Utils.showToastShort("Cannot open browser.");
            return;
        }

        try {
            String query = artist + " - " + title + " lyrics";
            String encodedQuery = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            }
            String url = "https://www.google.com/search?q=" + encodedQuery;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            Logger.printDebug(() -> TAG + "Launched browser for URL: " + url);

        } catch (android.content.ActivityNotFoundException e) {
            Logger.printException(() -> TAG + "Cannot launch browser: No activity found to handle ACTION_VIEW Intent.", e);
            Utils.showToastShort("No browser found.");
        } catch (Exception e) {
            Logger.printException(() -> TAG + "Failed to launch lyrics search due to an exception.", e);
            Utils.showToastShort("Failed to start search.");
        }
    }

    private enum OverlayState {
        EXPANDED, MINIMIZED
    }

    private enum SnapEdge {
        LEFT, RIGHT
    }
}
