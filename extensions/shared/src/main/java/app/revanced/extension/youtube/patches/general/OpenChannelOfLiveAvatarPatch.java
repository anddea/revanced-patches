package app.revanced.extension.youtube.patches.general;

import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.patches.general.requests.VideoDetailsRequest;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class OpenChannelOfLiveAvatarPatch {
    private static final boolean CHANGE_LIVE_RING_CLICK_ACTION =
            Settings.CHANGE_LIVE_RING_CLICK_ACTION.get();

    private static final AtomicBoolean engagementPanelOpen = new AtomicBoolean(false);
    private static volatile boolean liveChannelAvatarClicked = false;
    private static volatile String videoId = "";

    public static void showEngagementPanel(@Nullable Object object) {
        engagementPanelOpen.set(object != null);
    }

    public static void hideEngagementPanel() {
        engagementPanelOpen.compareAndSet(true, false);
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
            if (engagementPanelOpen.get()) {
                return false;
            }
            VideoDetailsRequest request = VideoDetailsRequest.getRequestForVideoId(videoId);
            if (request != null) {
                String channelId = request.getInfo();
                if (channelId != null) {
                    videoId = "";
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
            if (engagementPanelOpen.get()) {
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
