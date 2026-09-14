/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.utils;

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class PlayerControlsPatch {

    public static WeakReference<ImageView> fullscreenButtonRef = new WeakReference<>(null);

    private static boolean fullscreenButtonVisibilityCallbacksExist() {
        return false; // Modified during patching if needed.
    }

    /**
     * Injection point.
     */
    public static void setFullscreenCloseButton(ImageView imageButton) {
        fullscreenButtonRef = new WeakReference<>(imageButton);
        Logger.printDebug(() -> "Fullscreen button set");

        if (!fullscreenButtonVisibilityCallbacksExist()) {
            return;
        }

        // Add a global listener, since the protected method
        // View#onVisibilityChanged() does not have any call backs.
        imageButton.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int lastVisibility = View.VISIBLE;

            @Override
            public void onGlobalLayout() {
                try {
                    final int visibility = imageButton.getVisibility();
                    if (lastVisibility != visibility) {
                        lastVisibility = visibility;

                        Logger.printDebug(() -> "fullscreen button visibility: "
                                + (visibility == View.VISIBLE ? "VISIBLE" :
                                visibility == View.GONE ? "GONE" : "INVISIBLE"));

                        fullscreenButtonVisibilityChanged(visibility == View.VISIBLE);
                    }
                } catch (Exception ex) {
                    Logger.printDebug(() -> "OnGlobalLayoutListener failure", ex);
                }
            }
        });
    }

    // noinspection EmptyMethod
    private static void fullscreenButtonVisibilityChanged(boolean isVisible) {
        // Code added during patching.
    }

    /**
     * Injection point.
     */
    public static String getPlayerTopControlsLayoutResourceName(String original) {
        return "default";
    }

    /**
     * Injection point.
     */
    public static boolean forcePlayerSeekbar(boolean original) {
        if (!original) {
            Logger.printDebug(() -> "Player seekbar feature flag is off");
        }
        return true;
    }
}
