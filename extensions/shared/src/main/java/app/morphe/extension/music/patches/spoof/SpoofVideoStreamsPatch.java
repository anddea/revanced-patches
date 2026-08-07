package app.morphe.extension.music.patches.spoof;

import static app.morphe.extension.music.settings.Settings.SPOOF_VIDEO_STREAMS_CLIENT_TYPE;
import static app.morphe.extension.shared.spoof.ClientType.ANDROID_MUSIC_NO_SDK;
import static app.morphe.extension.shared.spoof.ClientType.ANDROID_MUSIC_REEL;
import static app.morphe.extension.shared.spoof.ClientType.ANDROID_VR;
import static app.morphe.extension.shared.spoof.ClientType.TV_SABR;
import static app.morphe.extension.shared.spoof.ClientType.VISIONOS_1_02;

import java.util.List;

import app.morphe.extension.shared.spoof.ClientType;

@SuppressWarnings("unused")
public class SpoofVideoStreamsPatch {

    /**
     * Injection point.
     */
    public static void setClientOrderToUse() {
        List<ClientType> availableClients = List.of(
                TV_SABR,
                ANDROID_VR,
                VISIONOS_1_02,
                ANDROID_MUSIC_NO_SDK,
                ANDROID_MUSIC_REEL
        );

        app.morphe.extension.shared.spoof.SpoofVideoStreamsPatch.setClientsToUse(
                availableClients, SPOOF_VIDEO_STREAMS_CLIENT_TYPE.get());
    }
}
