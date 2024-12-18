package app.revanced.extension.youtube.patches.general;

import app.revanced.extension.shared.settings.BooleanSetting;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class DownloadActionsPatch extends VideoUtils {

    private static final BooleanSetting overrideVideoDownloadButton =
            Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON;

    private static final BooleanSetting overridePlaylistDownloadButton =
            Settings.OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON;

    /**
     * Injection point.
     * <p>
     * Called from the in app download hook,
     * for both the player action button (below the video)
     * and the 'Download video' flyout option for feed videos.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppVideoDownloadButtonOnClick(String videoId) {
        try {
            if (!overrideVideoDownloadButton.get()) {
                return false;
            }
            if (videoId == null || videoId.isEmpty()) {
                return false;
            }
            launchVideoExternalDownloader(videoId);

            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "inAppVideoDownloadButtonOnClick failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     * <p>
     * Called from the in app playlist download hook.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static String inAppPlaylistDownloadButtonOnClick(String playlistId) {
        try {
            if (!overridePlaylistDownloadButton.get()) {
                return playlistId;
            }
            if (playlistId == null || playlistId.isEmpty()) {
                return playlistId;
            }
            launchPlaylistExternalDownloader(playlistId);

            return "";
        } catch (Exception ex) {
            Logger.printException(() -> "inAppPlaylistDownloadButtonOnClick failure", ex);
        }
        return playlistId;
    }

    /**
     * Injection point.
     * <p>
     * Called from the 'Download playlist' flyout option.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppPlaylistDownloadMenuOnClick(String playlistId) {
        try {
            if (!overridePlaylistDownloadButton.get()) {
                return false;
            }
            if (playlistId == null || playlistId.isEmpty()) {
                return false;
            }
            launchPlaylistExternalDownloader(playlistId);

            return true;
        } catch (Exception ex) {
            Logger.printException(() -> "inAppPlaylistDownloadMenuOnClick failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static boolean overridePlaylistDownloadButtonVisibility() {
        return overridePlaylistDownloadButton.get();
    }

}
