package app.morphe.extension.youtube.patches.general;

import static app.morphe.extension.shared.utils.StringRef.str;

import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.facebook.litho.ComponentHost;

import java.util.Map;

import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.youtube.patches.general.requests.ChannelIdRequest;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class OpenChannelOfLiveAvatarPatch {
    private static final boolean CHANGE_LIVE_RING_CLICK_ACTION =
            Settings.CHANGE_LIVE_RING_CLICK_ACTION.get();

    /**
     * If you change the language in the app settings, a string from another language may be used.
     * In this case, restarting the app will solve it.
     */
    private static final String liveRingDescription = str("revanced_live_ring_description");

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
     * @param playbackStartDescriptorMap map containing information about PlaybackStartDescriptor
     * @param newlyLoadedVideoId         id of the current video
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
            // Check content description (accessibility labels) of the live ring.
            final CharSequence contentDescription = componentHost.getContentDescription();
            if (contentDescription == null) {
                return;
            }
            final boolean match = liveRingDescription.equals(contentDescription.toString());
            Logger.printDebug(() -> "resource description: '" + liveRingDescription + "', litho description: '" + contentDescription + "', match: " + match);
            if (!match) {
                // Sometimes it may not match:
                // 1. In some languages, accessibility label is not provided.
                // 2. Language has changed in the app settings, and the app has not restarted.
                // In this case, fallback with the legacy method.

                // Child count of other litho Views such as Thumbnail and Watch history: 2
                // Child count of live ring: 1
                if (componentHost.getChildCount() != 1) {
                    return;
                }
                // Play all button in playlist cannot be filtered with the above conditions
                // Check the ViewGroup tree
                if (!(componentHost.getChildAt(0) instanceof ComponentHost liveRingViewGroup)) {
                    return;
                }
                if (!(liveRingViewGroup.getChildAt(0) instanceof ImageView)) {
                    return;
                }
            }
            // Fetch channel id
            videoId = newlyLoadedVideoId;
            ChannelIdRequest.fetchRequestIfNeeded(newlyLoadedVideoId);
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
            ChannelIdRequest request = ChannelIdRequest.getRequestForVideoId(videoId);
            if (request != null) {
                String channelId = request.getChannelId();
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
