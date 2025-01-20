package app.revanced.extension.youtube.patches.general;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.patches.general.requests.VideoDetailsRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class OpenChannelOfLiveAvatarPatch {
    private static final boolean CHANGE_LIVE_RING_CLICK_ACTION =
            Settings.CHANGE_LIVE_RING_CLICK_ACTION.get();

    private static volatile String videoId = "";
    private static volatile boolean isCommentsPanelOpen = false;
    private static volatile boolean liveChannelAvatarClicked = false;

    public static void commentsPanelClosed() {
        isCommentsPanelOpen = false;
    }

    public static void commentsPanelOpen() {
        isCommentsPanelOpen = true;
    }

    public static void liveChannelAvatarClicked() {
        liveChannelAvatarClicked = true;
    }

    public static boolean openChannelOfLiveAvatar() {
        try {
            if (!CHANGE_LIVE_RING_CLICK_ACTION) {
                return false;
            }
            if (!liveChannelAvatarClicked) {
                return false;
            }
            if (isCommentsPanelOpen) {
                return false;
            }
            VideoDetailsRequest request = VideoDetailsRequest.getRequestForVideoId(videoId);
            if (request != null) {
                String channelId = request.getInfo();
                if (channelId != null) {
                    liveChannelAvatarClicked = false;
                    VideoUtils.openChannel(channelId);
                    return true;
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "openChannelOfLiveAvatar failure", ex);
        }
        return false;
    }

    public static void openChannelOfLiveAvatar(String newlyLoadedVideoId) {
        try {
            if (!CHANGE_LIVE_RING_CLICK_ACTION) {
                return;
            }
            if (!liveChannelAvatarClicked) {
                return;
            }
            if (isCommentsPanelOpen) {
                return;
            }
            if (newlyLoadedVideoId.isEmpty()) {
                return;
            }
            videoId = newlyLoadedVideoId;
            VideoDetailsRequest.fetchRequestIfNeeded(newlyLoadedVideoId);
        } catch (Exception ex) {
            Logger.printException(() -> "openChannelOfLiveAvatar failure", ex);
        }
    }

}
