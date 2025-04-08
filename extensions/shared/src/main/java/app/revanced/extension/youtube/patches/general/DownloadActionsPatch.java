package app.revanced.extension.youtube.patches.general;

import static app.revanced.extension.youtube.utils.VideoUtils.launchPlaylistExternalDownloader;
import static app.revanced.extension.youtube.utils.VideoUtils.launchVideoExternalDownloader;

import android.view.View;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.patches.utils.PlaylistPatch;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class DownloadActionsPatch {

    private static final boolean OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON =
            Settings.OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON.get();

    private static final boolean OVERRIDE_VIDEO_DOWNLOAD_BUTTON =
            Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON.get();

    private static final boolean OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER =
            OVERRIDE_VIDEO_DOWNLOAD_BUTTON && Settings.OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER.get();

    private static final String ELEMENTS_SENDER_VIEW =
            "com.google.android.libraries.youtube.rendering.elements.sender_view";

    /**
     * Injection point.
     * <p>
     * Called from the in app download hook,
     * for both the player action button (below the video)
     * and the 'Download video' flyout option for feed videos.
     * <p>
     * Appears to always be called from the main thread.
     */
    public static boolean inAppVideoDownloadButtonOnClick(@Nullable Map<Object, Object> map, Object offlineVideoEndpointOuterClass,
                                                          @Nullable String videoId) {
        try {
            if (OVERRIDE_VIDEO_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(videoId)) {
                if (OVERRIDE_VIDEO_DOWNLOAD_BUTTON_QUEUE_MANAGER) {
                    if (map != null && map.get(ELEMENTS_SENDER_VIEW) instanceof View view) {
                        PlaylistPatch.setContext(view.getContext());
                    }
                    PlaylistPatch.prepareDialogBuilder(videoId);
                } else {
                    launchVideoExternalDownloader(videoId);
                }

                return true;
            }
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
            if (OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(playlistId)) {
                launchPlaylistExternalDownloader(playlistId);
                return "";
            }
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
            if (OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON && StringUtils.isNotEmpty(playlistId)) {
                launchPlaylistExternalDownloader(playlistId);
                return true;
            }
        } catch (Exception ex) {
            Logger.printException(() -> "inAppPlaylistDownloadMenuOnClick failure", ex);
        }
        return false;
    }

    /**
     * Injection point.
     */
    public static boolean overridePlaylistDownloadButtonVisibility() {
        return OVERRIDE_PLAYLIST_DOWNLOAD_BUTTON;
    }

}
