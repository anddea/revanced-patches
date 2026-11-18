package app.morphe.extension.music.patches.ads;

import android.view.View;
import android.view.ViewGroup;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.utils.Logger;

@SuppressWarnings("unused")
public class MusicAdsPatch {
    private static final boolean HIDE_MUSIC_ADS = Settings.HIDE_MUSIC_ADS.get();

    public static boolean hideMusicAds() {
        return HIDE_MUSIC_ADS;
    }

    public static boolean hideMusicAds(boolean original) {
        return !HIDE_MUSIC_ADS && original;
    }

    /**
     * Injection point.
     * <p>
     * When casting, the cast device loads and plays ads on its own, so they cannot be blocked.
     * The app still draws its ad overlay (ad title, progress text and skip button) over the player.
     * <p>
     * The app shows and hides the overlay by changing the visibility of the root view only,
     * so hiding the views inside the root keeps the overlay hidden.
     *
     * @param overlayRoot Root view of the inflated default_ad_overlay layout.
     */
    public static void hideAdOverlay(ViewGroup overlayRoot) {
        try {
            if (!Settings.HIDE_MUSIC_ADS.get()) {
                return;
            }
            for (int i = 0, count = overlayRoot.getChildCount(); i < count; i++) {
                overlayRoot.getChildAt(i).setVisibility(View.GONE);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "hideAdOverlay failure", ex);
        }
    }
}
