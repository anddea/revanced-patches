package app.revanced.extension.youtube.patches.general;

import androidx.annotation.NonNull;

import com.facebook.litho.ComponentHost;

import java.util.Map;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.patches.general.requests.VideoDetailsRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class OpenChannelOfLiveAvatarPatch {
    private static final boolean CHANGE_LIVE_RING_CLICK_ACTION =
            Settings.CHANGE_LIVE_RING_CLICK_ACTION.get();

    private static volatile String videoId = "";

    /**
     * This key's value is the LithoView that opened the video (Live ring or Thumbnails).
     */
    private static final String ELEMENTS_SENDER_VIEW =
            "com.google.android.libraries.youtube.rendering.elements.sender_view";

    /**
     * If the video is open by clicking live ring, this key does not exists.
     */
    private static final String VIDEO_THUMBNAIL_VIEW_KEY =
            "VideoPresenterConstants.VIDEO_THUMBNAIL_VIEW_KEY";

    /**
     * Injection point.
     *
     * @param playbackStartDescriptorMap    map containing information about PlaybackStartDescriptor
     * @param newlyLoadedVideoId            id of the current video
     */
    public static void fetchChannelId(@NonNull Map<Object, Object> playbackStartDescriptorMap, String newlyLoadedVideoId) {
        try {
            if (!CHANGE_LIVE_RING_CLICK_ACTION) {
                return;
            }
            // Video id is empty
            if (newlyLoadedVideoId.isEmpty()) {
                return;
            }
            // Video was opened by clicking the thumbnail
            if (playbackStartDescriptorMap.containsKey(VIDEO_THUMBNAIL_VIEW_KEY)) {
                return;
            }
            // If the video was opened in the watch history, there is no VIDEO_THUMBNAIL_VIEW_KEY
            // In this case, check the view that opened the video (Live ring is litho)
            if (!(playbackStartDescriptorMap.get(ELEMENTS_SENDER_VIEW) instanceof ComponentHost componentHost)) {
                return;
            }
            // Child count of other litho Views such as Thumbnail and Watch history: 2
            // Child count of live ring: 1
            if (componentHost.getChildCount() != 1) {
                return;
            }
            // Fetch channel id
            videoId = newlyLoadedVideoId;
            VideoDetailsRequest.fetchRequestIfNeeded(newlyLoadedVideoId);
        } catch (Exception ex) {
            Logger.printException(() -> "fetchVideoInformation failure", ex);
        }
    }

    public static boolean openChannel() {
        try {
            if (!CHANGE_LIVE_RING_CLICK_ACTION) {
                return false;
            }
            // If it is not fetch, the video id is empty
            if (videoId.isEmpty()) {
                return false;
            }
            VideoDetailsRequest request = VideoDetailsRequest.getRequestForVideoId(videoId);
            if (request != null) {
                String channelId = request.getInfo();
                // Open the channel
                if (channelId != null) {
                    videoId = "";
                    VideoUtils.openChannel(channelId);
                    return true;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "openChannel failure", ex);
        }
        return false;
    }

}
