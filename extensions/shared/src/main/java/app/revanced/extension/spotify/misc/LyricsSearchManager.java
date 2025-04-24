package app.revanced.extension.spotify.misc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
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
 * Provides buttons to search lyrics on Google and Songtell.
 */
@SuppressWarnings("unused")
public class LyricsSearchManager {

    private static final String TAG = "LyricsSearch";
    private static final Object buttonLock = new Object();

    // --- Configuration ---
    private static final int EDGE_PADDING_DP = 8;
    private static final int DEFAULT_BOTTOM_MARGIN_DP = 50;
    private static final int ICON_BUTTON_SIZE_DP = 40;
    private static final int BUTTON_HEIGHT_DP = 40;
    private static final int EXPANDED_LAYOUT_PADDING_DP = 5;
    private static final int BUTTON_INTERNAL_PADDING_DP = 8;
    private static final int BUTTON_CORNER_RADIUS_DP = 20; // For main search button
    private static final int ICON_BUTTON_MARGIN_DP = 5; // Margin between icons

    private static final String COLOR_PRIMARY_ACCENT = "#1ED760"; // Spotify Green
    private static final String COLOR_DARK_BACKGROUND = "#282828"; // Dark Gray bg (ARGB: 200, 40, 40, 40)
    private static final String COLOR_ICON_BACKGROUND = "#404040"; // Slightly lighter gray for icon bg
    private static final String COLOR_TEXT_ON_ACCENT = "#000000"; // Black text on green button
    private static final String COLOR_ICON_TINT = "#B3B3B3"; // White icons

    // --- State Variables ---
    private static String currentSearchTitle = null;
    private static String currentSearchArtist = null;

    @SuppressLint("StaticFieldLeak")
    private static volatile FrameLayout lyricsButtonContainer = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile LinearLayout expandedLayout = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile Button googleSearchButton = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageButton songtellButton = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageButton minimizeButton = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile ImageButton expandButton = null;

    private static volatile OverlayState currentOverlayState = OverlayState.MINIMIZED;
    private static volatile SnapEdge lastSnappedEdge = SnapEdge.RIGHT;
    private static volatile boolean isDragging = false;
    private static volatile float initialTouchX, initialTouchY;
    private static volatile int initialX, initialY;
    private static volatile int lastKnownTopMarginForMinimized = -1;
    private static volatile int screenHeight = -1;

    /**
     * Processes track metadata updates. Shows, hides, or updates the overlay content.
     * Ensures the overlay is correctly displayed even after app restarts from recents.
     *
     * @param titleObj  The track title (expected String).
     * @param artistObj The track artist (expected String).
     */
    public static void processTrackMetadata(Object titleObj, Object artistObj) {
        String title = titleObj instanceof String tempTitle && !tempTitle.isEmpty() ? tempTitle : null;
        String artist = artistObj instanceof String tempArtist && !tempArtist.isEmpty() ? tempArtist : null;

        synchronized (buttonLock) {
            boolean hasValidMetadata = title != null && artist != null;
            boolean metadataChanged = hasValidMetadata && (!title.equals(currentSearchTitle) || !artist.equals(currentSearchArtist));

            if (hasValidMetadata) {
                currentSearchTitle = title;
                currentSearchArtist = artist;

                Logger.printDebug(() -> TAG + ": Valid metadata received. Title=[" + currentSearchTitle + "], Artist=[" + currentSearchArtist + "]. Ensuring overlay.");
                Utils.runOnMainThreadNowOrLater(LyricsSearchManager::showOrUpdateOverlay);
            } else {
                // Metadata is invalid or missing
                if (currentSearchTitle != null || currentSearchArtist != null) {
                    Logger.printInfo(() -> TAG + ": Received invalid title or artist. Removing overlay.");
                    currentSearchTitle = null;
                    currentSearchArtist = null;
                    Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeOverlayInternal);
                } else {
                    // Already removed or never shown, do nothing
                    Logger.printDebug(() -> TAG + ": Received invalid metadata, overlay likely already hidden.");
                }
            }
        }
    }

    /**
     * Creates, updates, or re-attaches the overlay to the current Activity's DecorView.
     * Handles cases where the overlay might exist but be detached (e.g., after activity recreation).
     * This method MUST be called on the main UI thread.
     */
    @SuppressLint({"SetTextI18n", "ClickableViewAccessibility"})
    private static void showOrUpdateOverlay() {
        Activity currentActivity = Utils.getActivity();
        if (currentActivity == null) {
            Logger.printInfo(() -> TAG + ": Cannot show overlay: No current Activity available.");
            return;
        }

        ViewGroup decorView = getDecorView(currentActivity);
        if (decorView == null) {
            Logger.printException(() -> TAG + ": Cannot show overlay: Failed to get DecorView.");
            return;
        }

        synchronized (buttonLock) {
            if (screenHeight <= 0) {
                DisplayMetrics displayMetrics = currentActivity.getResources().getDisplayMetrics();
                screenHeight = displayMetrics.heightPixels;
                Logger.printDebug(() -> TAG + ": Screen height initialized: " + screenHeight);
            }

            boolean needsCreation = lyricsButtonContainer == null;
            boolean needsReattaching = !needsCreation && (lyricsButtonContainer.getParent() != decorView || !lyricsButtonContainer.isAttachedToWindow());

            if (needsReattaching) {
                Logger.printDebug(() -> TAG + ": Overlay exists but needs re-attaching.");
                removeOverlayInternal();
                needsCreation = true;
            }

            if (needsCreation) {
                Logger.printDebug(() -> TAG + ": Creating and adding lyrics overlay to DecorView...");
                createOverlayViews(currentActivity);
                if (lyricsButtonContainer == null) return;

                // --- Add the Container to the Activity's Decor View ---
                FrameLayout.LayoutParams containerParams = getLayoutParamsForCurrentState(currentActivity);
                try {
                    decorView.addView(lyricsButtonContainer, containerParams);
                    Logger.printInfo(() -> TAG + ": Lyrics overlay added to DecorView (State: " + currentOverlayState + ")");
                    updateVisibilityBasedOnState();
                } catch (Exception e) {
                    Logger.printException(() -> TAG + ": Error adding lyrics overlay to DecorView", e);
                    removeOverlayInternal();
                }
            } else {
                // Already exists and attached, just ensure state
                Logger.printDebug(() -> TAG + ": Overlay already attached. Ensuring state.");
                ensureOverlayState();
            }
        }
    }

    /**
     * Creates all the views for the overlay (container, expanded layout, buttons).
     * This method assumes it's called within the buttonLock synchronized block.
     * Must be called on the Main thread.
     *
     * @param context Context for creating views.
     */
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private static void createOverlayViews(Context context) {
        try {
            lyricsButtonContainer = new FrameLayout(context);

            // --- Expanded Layout ---
            expandedLayout = new LinearLayout(context);
            expandedLayout.setOrientation(LinearLayout.HORIZONTAL);
            expandedLayout.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable expandedBg = new GradientDrawable();
            expandedBg.setColor(Color.parseColor(COLOR_DARK_BACKGROUND));
            expandedBg.setCornerRadius(Utils.dpToPx(BUTTON_CORNER_RADIUS_DP * 2));
            expandedLayout.setBackground(expandedBg);
            int expandedPaddingPx = Utils.dpToPx(EXPANDED_LAYOUT_PADDING_DP);
            expandedLayout.setPadding(expandedPaddingPx, expandedPaddingPx, expandedPaddingPx, expandedPaddingPx);

            int buttonHeightPx = Utils.dpToPx(BUTTON_HEIGHT_DP);
            int iconButtonSizePx = Utils.dpToPx(ICON_BUTTON_SIZE_DP);
            int iconPaddingPx = Utils.dpToPx(BUTTON_INTERNAL_PADDING_DP);
            int iconMarginPx = Utils.dpToPx(ICON_BUTTON_MARGIN_DP);

            // --- Google Search Button ---
            googleSearchButton = new Button(context);
            googleSearchButton.setText("Search Lyrics");
            googleSearchButton.setAllCaps(false);
            googleSearchButton.setTextColor(Color.parseColor(COLOR_TEXT_ON_ACCENT));
            googleSearchButton.setPadding(Utils.dpToPx(16), 0, Utils.dpToPx(16), 0);
            GradientDrawable searchBg = new GradientDrawable();
            searchBg.setShape(GradientDrawable.RECTANGLE);
            searchBg.setColor(Color.parseColor(COLOR_PRIMARY_ACCENT));
            searchBg.setCornerRadius(Utils.dpToPx(BUTTON_CORNER_RADIUS_DP));
            googleSearchButton.setBackground(searchBg);
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, buttonHeightPx);
            googleSearchButton.setLayoutParams(searchParams);
            googleSearchButton.setOnClickListener(v -> handleSearchClick(SearchProvider.GOOGLE));

            // --- Songtell Button ---
            int meaningIconId = ResourceUtils.getDrawableIdentifier("encore_icon_enhance_24");
            if (meaningIconId == 0) meaningIconId = android.R.drawable.ic_menu_search;

            songtellButton = createCircularImageButton(context,
                    meaningIconId, iconButtonSizePx, iconPaddingPx, COLOR_ICON_BACKGROUND, COLOR_ICON_TINT);
            songtellButton.setOnClickListener(v -> handleSearchClick(SearchProvider.SONGTELL));
            LinearLayout.LayoutParams songtellParams = new LinearLayout.LayoutParams(iconButtonSizePx, iconButtonSizePx);
            songtellParams.setMarginStart(iconMarginPx);
            songtellButton.setLayoutParams(songtellParams);

            // --- Minimize Button (X) ---
            int minimizeIconId = ResourceUtils.getDrawableIdentifier("encore_icon_x_24");
            if (minimizeIconId == 0) minimizeIconId = android.R.drawable.ic_menu_close_clear_cancel;

            minimizeButton = createCircularImageButton(context,
                    minimizeIconId, iconButtonSizePx, iconPaddingPx, COLOR_ICON_BACKGROUND, COLOR_ICON_TINT);
            minimizeButton.setOnClickListener(v -> minimizeOverlay());
            LinearLayout.LayoutParams minimizeParams = new LinearLayout.LayoutParams(iconButtonSizePx, iconButtonSizePx);
            minimizeParams.setMarginStart(iconMarginPx);
            minimizeButton.setLayoutParams(minimizeParams);

            // Add buttons to expanded layout in order
            expandedLayout.addView(googleSearchButton);
            expandedLayout.addView(songtellButton);
            expandedLayout.addView(minimizeButton);

            // --- Expand Button (Floating Search Icon) ---
            int searchIconId = ResourceUtils.getDrawableIdentifier("encore_icon_search_24");
            if (searchIconId == 0) searchIconId = android.R.drawable.ic_menu_search;

            expandButton = createCircularImageButton(context,
                    searchIconId,
                    iconButtonSizePx, iconPaddingPx, COLOR_PRIMARY_ACCENT, COLOR_TEXT_ON_ACCENT);

            // Add touch listener for dragging/expanding
            final ViewConfiguration viewConfig = ViewConfiguration.get(context);
            final int touchSlop = viewConfig.getScaledTouchSlop();

            expandButton.setOnTouchListener((view, event) -> {
                // Only allow interaction when minimized
                if (currentOverlayState != OverlayState.MINIMIZED || lyricsButtonContainer == null) return false;

                final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lyricsButtonContainer.getLayoutParams();
                final ViewGroup parentView = (ViewGroup) lyricsButtonContainer.getParent();
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
                        parentView.requestDisallowInterceptTouchEvent(true);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - initialTouchX;
                        float dy = event.getRawY() - initialTouchY;

                        if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            isDragging = true;
                            Logger.printDebug(() -> TAG + ": Dragging started.");
                        }

                        if (isDragging) {
                            int newY = initialY + (int) dy;
                            int newX = initialX + (int) dx;

                            int viewHeight = lyricsButtonContainer.getHeight();
                            if (viewHeight <= 0)
                                viewHeight = Utils.dpToPx(ICON_BUTTON_SIZE_DP);
                            newY = Math.max(edgePaddingPx, Math.min(newY, parentView.getHeight() - viewHeight - edgePaddingPx));

                            params.topMargin = newY;
                            params.leftMargin = newX;
                            params.gravity = Gravity.NO_GRAVITY;
                            lyricsButtonContainer.setLayoutParams(params);

                            lastKnownTopMarginForMinimized = newY;
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        parentView.requestDisallowInterceptTouchEvent(false);
                        if (isDragging) {
                            // Drag finished -> Snap to nearest edge horizontally
                            Logger.printDebug(() -> TAG + ": Dragging finished. Snapping to edge.");
                            snapToNearestEdge(lyricsButtonContainer, parentView, params);
                        } else {
                            // Treat as a click -> Expand
                            Logger.printDebug(() -> TAG + ": Expand button clicked (no drag).");
                            expandOverlay();
                        }
                        isDragging = false;
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        parentView.requestDisallowInterceptTouchEvent(false);
                        if (isDragging) {
                            // Snap back if dragging was cancelled mid-drag
                            Logger.printDebug(() -> TAG + ": Dragging cancelled. Snapping to edge.");
                            snapToNearestEdge(lyricsButtonContainer, parentView, params);
                        }
                        isDragging = false;
                        return true;
                }
                return false;
            });

            // --- Add Views to Container FrameLayout ---
            // Expanded layout sits inside the container
            FrameLayout.LayoutParams expandedParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            expandedParams.gravity = Gravity.CENTER;
            lyricsButtonContainer.addView(expandedLayout, expandedParams);

            // Minimized button also sits inside, centered (visibility toggled later)
            FrameLayout.LayoutParams minimizedParams = new FrameLayout.LayoutParams(iconButtonSizePx, iconButtonSizePx);
            minimizedParams.gravity = Gravity.CENTER;
            lyricsButtonContainer.addView(expandButton, minimizedParams);

            // Set initial position if not previously set
            if (lastKnownTopMarginForMinimized == -1 && screenHeight > 0) {
                int buttonSize = Utils.dpToPx(ICON_BUTTON_SIZE_DP);
                // Position roughly 2x bottom margin + button size up from bottom
                int safeBottomMargin = Utils.dpToPx(DEFAULT_BOTTOM_MARGIN_DP * 2);
                lastKnownTopMarginForMinimized = screenHeight - buttonSize - safeBottomMargin;
                Logger.printDebug(() -> TAG + ": Initial minimized Y position set to: " + lastKnownTopMarginForMinimized);
            }

        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Failed to create overlay views", e);
            // Ensure cleanup if something goes wrong during creation
            removeOverlayInternal();
        }
    }

    /**
     * Helper to create a circular ImageButton with consistent styling.
     */
    private static ImageButton createCircularImageButton(Context context, int iconResId, int sizePx, int paddingPx, String bgColor, String tintColor) {
        ImageButton button = new ImageButton(context);
        if (iconResId != 0) {
            button.setImageResource(iconResId);
        }
        button.setColorFilter(Color.parseColor(tintColor), PorterDuff.Mode.SRC_IN);
        button.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.parseColor(bgColor));
        background.setSize(sizePx, sizePx); // Ensure the oval is a circle
        button.setBackground(background);

        return button;
    }

    /**
     * Ensures the overlay is in the correct state (visibility, position)
     * without changing the current minimized/expanded state. Called when overlay exists.
     */
    private static void ensureOverlayState() {
        synchronized (buttonLock) {
            if (lyricsButtonContainer != null && lyricsButtonContainer.isAttachedToWindow()) {
                Logger.printDebug(() -> TAG + ": Ensuring overlay state is correct (Visibility).");
                updateVisibilityBasedOnState();
            } else {
                // Not attached or null, attempt to show/update which handles re-attachment
                Logger.printDebug(() -> TAG + ": Ensure state found detached/null overlay, triggering show/update.");
                showOrUpdateOverlay();
            }
        }
    }

    private static ViewGroup getDecorView(Activity activity) {
        if (activity == null || activity.getWindow() == null) return null;
        View decor = activity.getWindow().getDecorView();
        if (decor instanceof FrameLayout) {
            return (ViewGroup) decor;
        } else if (decor instanceof ViewGroup) {
            Logger.printInfo(() -> TAG + ": DecorView is not a FrameLayout, but a " + decor.getClass().getSimpleName());
            return (ViewGroup) decor;
        }
        Logger.printException(() -> TAG + ": DecorView is not a ViewGroup!");
        return null;
    }

    /**
     * Gets appropriate LayoutParams based on the current state (Minimized/Snapped or Expanded/Centered-Bottom).
     */
    private static FrameLayout.LayoutParams getLayoutParamsForCurrentState(Context context) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int iconSizePx = Utils.dpToPx(ICON_BUTTON_SIZE_DP);
        int edgePaddingPx = Utils.dpToPx(EDGE_PADDING_DP);

        if (lastKnownTopMarginForMinimized <= edgePaddingPx) {
            if (screenHeight > 0) {
                int safeBottomMargin = Utils.dpToPx(DEFAULT_BOTTOM_MARGIN_DP * 2);
                lastKnownTopMarginForMinimized = Math.max(edgePaddingPx, screenHeight - iconSizePx - safeBottomMargin);
            } else {
                lastKnownTopMarginForMinimized = Utils.dpToPx(400); // Fallback if screen height unknown
            }
            Logger.printDebug(() -> TAG + ": Default minimized Y position recalculated: " + lastKnownTopMarginForMinimized);
        }

        if (currentOverlayState == OverlayState.MINIMIZED) {
            // --- MINIMIZED STATE ---
            params.width = iconSizePx;
            params.height = iconSizePx;
            params.gravity = Gravity.NO_GRAVITY;

            // Use saved vertical position (clamped)
            params.topMargin = Math.max(edgePaddingPx, lastKnownTopMarginForMinimized);

            // Get parent width for horizontal snapping
            int parentWidth = getParentWidth(context);

            // Snap horizontally to last known edge
            if (lastSnappedEdge == SnapEdge.LEFT) {
                params.leftMargin = edgePaddingPx;
            } else {
                params.leftMargin = Math.max(edgePaddingPx, parentWidth - iconSizePx - edgePaddingPx);
            }
            Logger.printDebug(() -> TAG + ": Layout Params: Minimized, Snap " + lastSnappedEdge + ", Top: " + params.topMargin + ", Left: " + params.leftMargin);

        } else {
            // --- EXPANDED STATE ---
            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            params.bottomMargin = Utils.dpToPx(DEFAULT_BOTTOM_MARGIN_DP);

            // Reset margins as gravity handles positioning
            params.leftMargin = 0;
            params.topMargin = 0;
            Logger.printDebug(() -> TAG + ": Layout Params: Expanded, Bottom Center, BottomMargin: " + params.bottomMargin);
        }

        return params;
    }

    /**
     * Safely gets the parent width, falling back to screen width if needed.
     */
    private static int getParentWidth(Context context) {
        ViewGroup decorView = getDecorView(Utils.getActivity());
        if (decorView != null && decorView.getWidth() > 0) {
            return decorView.getWidth();
        } else if (context != null) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            Logger.printInfo(() -> TAG + ": Using screen width as parent width fallback.");
            return displayMetrics.widthPixels;
        } else {
            Logger.printException(() -> TAG + ": Could not get parent width, using default 1080.");
            return 1080; // Last resort
        }
    }

    /**
     * Snaps the container to the nearest edge (left or right) after dragging, preserving vertical position.
     */
    private static void snapToNearestEdge(FrameLayout container, ViewGroup parentView, FrameLayout.LayoutParams params) {
        if (container == null || parentView == null || params == null || currentOverlayState != OverlayState.MINIMIZED)
            return;

        int parentWidth = parentView.getWidth();
        if (parentWidth <= 0) {
            parentWidth = getParentWidth(container.getContext());
            if (parentWidth <= 0) {
                Logger.printException(() -> TAG + ": Cannot snap, parent width is zero or unavailable.");
                return;
            }
        }

        int viewWidth = container.getWidth();
        if (viewWidth <= 0) viewWidth = Utils.dpToPx(ICON_BUTTON_SIZE_DP);

        int currentLeftMargin = params.leftMargin;
        int viewCenterX = currentLeftMargin + viewWidth / 2;
        int edgePaddingPx = Utils.dpToPx(EDGE_PADDING_DP);

        params.gravity = Gravity.NO_GRAVITY;

        // Determine nearest edge based on center point
        if (viewCenterX < parentWidth / 2) {
            params.leftMargin = edgePaddingPx;
            lastSnappedEdge = SnapEdge.LEFT;
        } else {
            params.leftMargin = Math.max(edgePaddingPx, parentWidth - viewWidth - edgePaddingPx);
            lastSnappedEdge = SnapEdge.RIGHT;
        }
        Logger.printDebug(() -> TAG + ": Snapped to " + lastSnappedEdge + " edge. Top: " + params.topMargin + ", Left: " + params.leftMargin);

        container.setLayoutParams(params);
    }

    /**
     * Re-applies the layout params based on the current state, updating position/size.
     * Should be called after state change (minimize/expand) or when position needs refresh.
     */
    private static void applyLayoutParamsForState() {
        Utils.runOnMainThreadNowOrLater(() -> {
            synchronized (buttonLock) {
                if (lyricsButtonContainer != null && lyricsButtonContainer.getParent() != null) {
                    Context context = lyricsButtonContainer.getContext();
                    if (context != null) {
                        FrameLayout.LayoutParams params = getLayoutParamsForCurrentState(context);
                        lyricsButtonContainer.setLayoutParams(params);
                        Logger.printDebug(() -> TAG + ": Applied layout params for state: " + currentOverlayState);
                        lyricsButtonContainer.requestLayout();
                    } else {
                        Logger.printException(() -> TAG + ": Cannot apply layout params, context is null.");
                    }
                } else {
                    Logger.printDebug(() -> TAG + ": Cannot apply layout params, container or parent is null.");
                }
            }
        });
    }

    private static void minimizeOverlay() {
        synchronized (buttonLock) {
            if (currentOverlayState == OverlayState.MINIMIZED || lyricsButtonContainer == null) return;
            Logger.printDebug(() -> TAG + ": Minimizing overlay...");

            currentOverlayState = OverlayState.MINIMIZED;
            updateVisibilityBasedOnState();
            applyLayoutParamsForState();
        }
    }

    private static void expandOverlay() {
        synchronized (buttonLock) {
            if (currentOverlayState == OverlayState.EXPANDED || lyricsButtonContainer == null) return;
            Logger.printDebug(() -> TAG + ": Expanding overlay...");

            currentOverlayState = OverlayState.EXPANDED;
            updateVisibilityBasedOnState();
            applyLayoutParamsForState();
        }
    }

    /**
     * Updates visibility of internal components based on the current OverlayState.
     */
    private static void updateVisibilityBasedOnState() {
        if (lyricsButtonContainer == null) return;

        Utils.runOnMainThreadNowOrLater(() -> {
            synchronized (buttonLock) {
                if (expandedLayout == null || expandButton == null) {
                    Logger.printException(() -> TAG + ": Cannot update visibility, views are null.");
                    return;
                }

                final boolean isExpanded = currentOverlayState == OverlayState.EXPANDED;

                expandedLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                expandButton.setVisibility(isExpanded ? View.GONE : View.VISIBLE);

                lyricsButtonContainer.setOnTouchListener(null);

                Logger.printDebug(() -> TAG + ": Visibility updated for state: " + currentOverlayState);
            }
        });
    }

    /**
     * Schedules the removal of the lyrics overlay from the UI thread.
     * Public entry point for removal.
     */
    public static void removeOverlay() {
        Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeOverlayInternal);
    }

    /**
     * Removes the lyrics overlay from its parent view immediately and nullifies view references.
     * This method MUST be called on the main UI thread.
     */
    private static void removeOverlayInternal() {
        synchronized (buttonLock) {
            FrameLayout containerToRemove = lyricsButtonContainer;

            lyricsButtonContainer = null;
            expandedLayout = null;
            googleSearchButton = null;
            songtellButton = null;
            minimizeButton = null;
            expandButton = null;
            // Do NOT reset state like currentOverlayState, lastKnownTopMarginForMinimized, lastSnappedEdge
            // to preserve user's last known state and position.
            // Do NOT reset currentSearchTitle/Artist here, let processTrackMetadata manage them.

            if (containerToRemove != null && containerToRemove.getParent() instanceof ViewGroup parent) {
                Logger.printDebug(() -> TAG + ": Removing lyrics overlay from parent: " + parent.getClass().getName());
                try {
                    parent.removeView(containerToRemove);
                    Logger.printInfo(() -> TAG + ": Lyrics overlay removed successfully.");
                } catch (Exception e) {
                    // This might happen if the parent is already gone or view is detached
                    Logger.printException(() -> TAG + ": Error removing lyrics overlay view, might be already detached.", e);
                }
            } else if (containerToRemove != null) {
                Logger.printDebug(() -> TAG + ": Overlay container found but has no parent. Already removed or detached.");
            } else {
                Logger.printDebug(() -> TAG + ": removeOverlayInternal called but container was already null.");
            }
        }
    }

    /**
     * Handles click events for search buttons
     */
    private static void handleSearchClick(SearchProvider provider) {
        Logger.printDebug(() -> TAG + ": " + provider + " search button clicked.");
        synchronized (buttonLock) {
            if (currentSearchTitle != null && currentSearchArtist != null) {
                String query = currentSearchArtist + " - " + currentSearchTitle;
                String url;
                switch (provider) {
                    case SONGTELL:
                        url = "https://songtell.com/search?q=";
                        break;
                    case GOOGLE:
                    default:
                        url = "https://www.google.com/search?q=";
                        query += " lyrics";
                        break;
                }
                launchWebSearch(url, query);
            } else {
                Logger.printException(() -> TAG + ": Search button clicked but title/artist info missing.");
                Utils.showToastShort("Track info not available.");
            }
        }
    }

    /**
     * Launches a browser intent for the given base URL and query.
     *
     * @param baseUrl Base search URL (e.g., "<a href="https://www.google.com/search?q=">https://www.google.com/search?q=</a>")
     * @param query   The search term (will be URL-encoded).
     */
    private static void launchWebSearch(String baseUrl, String query) {
        Context context = Utils.getContext(); // Try application context first
        if (context == null) {
            Activity act = Utils.getActivity(); // Fallback to activity context
            if (act != null) context = act;
        }

        if (context == null) {
            Logger.printException(() -> TAG + ": Cannot launch browser: Context is null.");
            Utils.showToastShort("Cannot open browser.");
            return;
        }

        try {
            String encodedQuery = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            }

            String url = baseUrl + encodedQuery;

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            Logger.printDebug(() -> TAG + ": Launched browser for URL: " + url);

        } catch (ActivityNotFoundException e) {
            Logger.printException(() -> TAG + ": Cannot launch browser: No activity found to handle ACTION_VIEW Intent.", e);
            Utils.showToastShort("No browser found.");
        } catch (Exception e) {
            Logger.printException(() -> TAG + ": Failed to launch web search due to an unexpected exception.", e);
            Utils.showToastShort("Failed to start search.");
        }
    }

    /**
     * Enum for different search providers
     */
    private enum SearchProvider {GOOGLE, SONGTELL}

    /**
     * Represents the visibility state of the overlay
     */
    private enum OverlayState {EXPANDED, MINIMIZED}

    /**
     * Represents the edge the minimized overlay snaps to
     */
    private enum SnapEdge {LEFT, RIGHT}
}
