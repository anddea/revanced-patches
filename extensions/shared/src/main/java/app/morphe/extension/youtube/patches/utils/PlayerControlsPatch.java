/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.patches.utils;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;
import app.morphe.extension.youtube.patches.player.MiniplayerPatch;
import app.morphe.extension.youtube.patches.player.PlayerPatch;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public class PlayerControlsPatch {

    public static WeakReference<ImageView> fullscreenButtonRef = new WeakReference<>(null);

    /** ConstantState of the fullscreen button background the opacity was last applied to. */
    @Nullable
    private static Drawable.ConstantState sourceButtonBackgroundSnapshot;

    /**
     * The app assigns the circle background after the view exists, so this re-checks on every
     * pre-draw pass and only does work when the drawable is actually replaced.
     */
    private static void styleSourceButtonBackground(View sourceButton) {
        Drawable background = sourceButton.getBackground();
        if (background == null) return;

        // A null state cannot be tracked, so fall through and rely on mutate() being idempotent.
        Drawable.ConstantState state = background.getConstantState();
        if (state != null && state == sourceButtonBackgroundSnapshot) return;

        Drawable styled = PlayerPatch.applyControlButtonsBackgroundOpacity(background);
        if (styled != background) {
            sourceButton.setBackground(styled);
        }
        sourceButtonBackgroundSnapshot = styled.getConstantState();
    }

    /**
     * Only the modern layout marks these views. Preserve their full content height and match
     * the native icon's center without copying its insets into the scrollable button row.
     */
    private static void centerModernOverlay(View fullscreenButton) {
        if (!(fullscreenButton.getParent() instanceof ViewGroup container)) return;

        float offset = (fullscreenButton.getPaddingTop() - fullscreenButton.getPaddingBottom()) / 2f;
        for (String name : new String[]{"revanced_overlay_buttons_scroll_view", "timestamps_container", "time_bar_chapter_title_container"}) {
            View view = container.findViewById(ResourceUtils.getIdIdentifier(name));
            if (view != null && "morphe_modern_overlay".equals(view.getTag())) {
                view.setTranslationY(offset);
            }
        }
    }

    /**
     * Use one custom-button slot between icon centers, including the wider native fullscreen
     * slot. Translate the row as a unit so scrolling and internal button spacing stay intact.
     */
    private static void spaceOverlayButtons(View fullscreenButton) {
        if (!(fullscreenButton.getParent() instanceof ViewGroup container)) return;

        float fullscreenCenter = fullscreenButton.getX() + (fullscreenButton.getWidth()
                + fullscreenButton.getPaddingLeft() - fullscreenButton.getPaddingRight()) / 2f;
        View row = container.findViewById(ResourceUtils.getIdIdentifier("revanced_overlay_buttons_scroll_view"));
        if (row instanceof ViewGroup scroll && scroll.getChildCount() > 0
                && scroll.getChildAt(0) instanceof ViewGroup buttons) {
            for (int i = buttons.getChildCount() - 1; i >= 0; i--) {
                View button = buttons.getChildAt(i);
                if (button.getVisibility() == View.GONE || button.getWidth() == 0) continue;

                row.setTranslationX(fullscreenCenter - button.getWidth() / 2f - row.getRight());
                break;
            }
        }
    }

    private static boolean fullscreenButtonVisibilityCallbacksExist() {
        return false; // Modified during patching if needed.
    }

    /**
     * Injection point.
     */
    public static void setFullscreenCloseButton(ImageView imageButton) {
        fullscreenButtonRef = new WeakReference<>(imageButton);
        if (!ExtendedUtils.IS_21_29_OR_GREATER) {
            MiniplayerPatch.fixMinimalMiniplayerFullscreenButtonTint(imageButton);
        }
        Logger.printDebug(() -> "Fullscreen button set");

        imageButton.getViewTreeObserver().addOnPreDrawListener(() -> {
            try {
                styleSourceButtonBackground(imageButton);
                centerModernOverlay(imageButton);
                spaceOverlayButtons(imageButton);
            } catch (Exception ex) {
                Logger.printDebug(() -> "Could not update fullscreen button styling", ex);
            }
            return true;
        });

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

    /**
     * Injection point for player feature flags that select the modern layout.
     *
     * <p>The setting is only exposed by the patch on supported YouTube versions, but keeping the
     * method safe on every version lets the same extension code be shared by all patched builds.</p>
     */
    public static boolean useModernPlayerLayout(boolean original) {
        return !restoreOldPlayerButtons() && original;
    }

    /** Injection point for modern top-control layout feature flags. */
    public static boolean useModernPlayerTopControls(boolean original) {
        return !restoreOldPlayerButtons() && original;
    }

    /** Injection point for layout flags that are incompatible with the old player controls. */
    public static boolean allowModernPlayerLayoutFlags(boolean original) {
        return !restoreOldPlayerButtons() && original;
    }

    /**
     * Injection point for feature flags selecting YouTube's alternate bottom-controls layout.
     *
     * <p>The alternate layout is required for bold player controls. When bold icons are disabled,
     * or the user explicitly requests the old player buttons, keep the legacy layout.</p>
     */
    public static boolean usePlayerBottomControlsExploderLayout(boolean original) {
        return !restoreOldPlayerButtons();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean restoreOldPlayerButtons() {
        return Settings.RESTORE_OLD_PLAYER_BUTTONS.get()
                || !ExtendedUtils.IS_20_31_OR_GREATER
                || ExtendedUtils.isSpoofingToLessThan("20.31.00");
    }
}
