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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.Utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Manages the display and interaction of a floating "Search Lyrics" button overlay
 * within the Spotify application. It listens for track metadata updates and provides
 * buttons to search for lyrics on Google or dismiss the overlay.
 */
@SuppressWarnings("unused")
public class LyricsSearchManager {

    private static final String TAG = "LyricsSearchManager";
    private static final Object buttonLock = new Object();
    private static String currentSearchTitle = null;
    private static String currentSearchArtist = null;
    @SuppressLint("StaticFieldLeak")
    private static volatile View lyricsButtonContainer = null;

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
        if (titleObj instanceof String tempTitle) {
            if (!tempTitle.isEmpty()) {
                title = tempTitle;
            }
        }

        String artist = null;
        if (artistObj instanceof String tempArtist) {
            if (!tempArtist.isEmpty()) {
                artist = tempArtist;
            }
        }

        if (title != null && artist != null) {
            synchronized (buttonLock) {
                currentSearchTitle = title;
                currentSearchArtist = artist;
            }

            String finalTitle = title;
            String finalArtist = artist;
            Logger.printDebug(() -> TAG + "Updated track info: Title=[" + finalTitle + "], Artist=[" + finalArtist + "]");

            Utils.runOnMainThreadNowOrLater(LyricsSearchManager::showLyricsButton);
        } else {
            Logger.printInfo(() -> TAG + "safePrintMetadata received invalid title or artist. Removing button.");
            Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeLyricsButton);
        }
    }

    /**
     * Creates and displays the lyrics button overlay on the current Activity's DecorView.
     * This method MUST be called on the main UI thread.
     * If the button is already visible, this method does nothing.
     */
    @SuppressLint("SetTextI18n")
    private static void showLyricsButton() {
        Activity currentActivity = Utils.getActivity();
        if (currentActivity == null) {
            Logger.printInfo(() -> TAG + "Cannot show lyrics button: No current Activity available.");
            removeLyricsButtonInternal();
            return;
        }

        synchronized (buttonLock) {
            // If button is already showing, do nothing.
            if (lyricsButtonContainer != null && lyricsButtonContainer.getParent() != null) {
                Logger.printDebug(() -> TAG + "Lyrics button already visible.");
                return;
            }

            // If container existed but wasn't attached, clean up first
            if (lyricsButtonContainer != null) {
                Logger.printInfo(() -> TAG + "Button container existed but wasn't attached. Cleaning up.");
                removeLyricsButtonInternal();
            }

            Logger.printDebug(() -> TAG + "Creating and adding lyrics button overlay to DecorView...");

            // --- Create Container Layout ---
            FrameLayout container = new FrameLayout(currentActivity);

            // --- Create Horizontal Layout for Buttons ---
            LinearLayout buttonLayout = new LinearLayout(currentActivity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER_VERTICAL);

            int buttonWidthPx = Utils.dpToPx(40);
            int buttonHeightPx = Utils.dpToPx(40);

            // --- Create Search Button ---
            Button searchButton = new Button(currentActivity);
            searchButton.setText("Search Lyrics");
            searchButton.setAllCaps(false);
            searchButton.setTextColor(Color.BLACK);
            searchButton.setPadding(Utils.dpToPx(16), 0, Utils.dpToPx(16), 0);

            // Create rounded background drawable for search button
            GradientDrawable searchBg = new GradientDrawable();
            searchBg.setShape(GradientDrawable.RECTANGLE);
            searchBg.setColor(Color.parseColor("#1DB954")); // Spotify green
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

            // --- Create Dismiss Button ---
            ImageButton dismissButton = new ImageButton(currentActivity);
            dismissButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel); // Standard close icon
            dismissButton.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

            // Padding for the icon inside
            int paddingPx = Utils.dpToPx(16);
            dismissButton.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

            // Create circular background drawable for dismiss button
            GradientDrawable dismissBg = new GradientDrawable();
            dismissBg.setShape(GradientDrawable.OVAL);
            dismissBg.setColor(Color.parseColor("#202020"));
            dismissBg.setSize(buttonWidthPx, buttonHeightPx);
            dismissButton.setBackground(dismissBg);

            LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(
                    buttonWidthPx,
                    buttonHeightPx
            );
            dismissParams.setMarginStart(Utils.dpToPx(5)); // Margin between buttons
            dismissButton.setLayoutParams(dismissParams);

            dismissButton.setOnClickListener(v -> {
                Logger.printDebug(() -> TAG + "Dismiss button clicked.");
                removeLyricsButton();
            });

            // --- Add Buttons to Layout ---
            buttonLayout.addView(searchButton);
            buttonLayout.addView(dismissButton);

            // Add buttonLayout to the main container
            FrameLayout.LayoutParams buttonLayoutParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            container.addView(buttonLayout, buttonLayoutParams);

            // --- Add the Container to the Activity's Decor View ---
            ViewGroup decorView = (ViewGroup) currentActivity.getWindow().getDecorView();
            if (decorView == null) {
                Logger.printException(() -> TAG + "Could not get Decor View!");
                lyricsButtonContainer = null; // Clean up potentially created container
                return;
            }

            FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            containerParams.bottomMargin = Utils.dpToPx(50); // Margin from bottom

            try {
                decorView.addView(container, containerParams);
                lyricsButtonContainer = container; // Store reference only AFTER successful addView
                Logger.printInfo(() -> TAG + "Lyrics button overlay added to DecorView.");
            } catch (Exception e) {
                Logger.printException(() -> TAG + "Error adding lyrics button overlay to DecorView", e);
                lyricsButtonContainer = null; // Ensure reference is null if add failed
            }
        }
    }

    /**
     * Schedules the removal of the lyrics button overlay from the UI thread.
     */
    public static void removeLyricsButton() {
        Utils.runOnMainThreadNowOrLater(LyricsSearchManager::removeLyricsButtonInternal);
    }

    /**
     * Removes the lyrics button overlay from its parent view.
     * This method MUST be called on the main UI thread.
     */
    private static void removeLyricsButtonInternal() {
        synchronized (buttonLock) {
            View buttonToRemove = lyricsButtonContainer;
            if (buttonToRemove != null && buttonToRemove.getParent() instanceof ViewGroup parent) {
                Logger.printDebug(() -> TAG + "Removing lyrics button overlay from parent: " + parent.getClass().getName());
                try {
                    parent.removeView(buttonToRemove);
                    Logger.printInfo(() -> TAG + "Lyrics button overlay removed.");
                } catch (Exception e) {
                    Logger.printException(() -> TAG + "Error removing lyrics button overlay", e);
                } finally {
                    // Clear reference AFTER removal attempt
                    lyricsButtonContainer = null;
                }
            } else {
                // Already removed or never added properly, ensure reference is null
                lyricsButtonContainer = null;
            }
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Necessary when starting from non-Activity context

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
}
