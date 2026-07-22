/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.spoof;

import static app.morphe.extension.shared.patches.AppCheckPatch.IS_YOUTUBE;
import static app.morphe.extension.shared.patches.AppCheckPatch.IS_YOUTUBE_MUSIC;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings({"ConstantLocale", "deprecation"})
public enum ClientType {
    /**
     * Video not playable: None.
     * AV1 codec available.
     */
    ANDROID_REEL_AUTH(
            3,
            "ANDROID",
            "com.google.android.youtube",
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT),
            Build.ID,
            // A hardcoded client version is used for YouTube Music.
            "20.47.62",
            null,
            IS_YOUTUBE,
            IS_YOUTUBE,
            true,
            false,
            true,
            true,
            false,
            "Android Reel auth"
    ),
    /**
     * Video not playable: Paid, Movie, Private, Age-restricted.
     * AV1 codec available.
     */
    ANDROID_REEL_NO_AUTH(
            ANDROID_REEL_AUTH.id,
            ANDROID_REEL_AUTH.clientName,
            Objects.requireNonNull(ANDROID_REEL_AUTH.packageName),
            ANDROID_REEL_AUTH.deviceMake,
            ANDROID_REEL_AUTH.deviceModel,
            ANDROID_REEL_AUTH.osName,
            ANDROID_REEL_AUTH.osVersion,
            Objects.requireNonNull(ANDROID_REEL_AUTH.androidSdkVersion),
            ANDROID_REEL_AUTH.buildID,
            ANDROID_REEL_AUTH.clientVersion,
            ANDROID_REEL_AUTH.clientPlatform,
            false,
            false,
            ANDROID_REEL_AUTH.supportsMultiAudioTracks,
            ANDROID_REEL_AUTH.supportsOAuth2,
            ANDROID_REEL_AUTH.supportsVRImmersiveMode,
            ANDROID_REEL_AUTH.requireSABR,
            ANDROID_REEL_AUTH.usePlayerEndpoint,
            "Android Reel no auth"
    ),
    /**
     * Video not playable in YouTube: All videos (This client requires login, but cannot log in with YouTube's access token).
     * Video not playable in YouTube Music: None.
     * Uses non adaptive bitrate.
     */
    ANDROID_MUSIC_NO_SDK(
            21,
            "ANDROID_MUSIC",
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.RELEASE,
            "7.12.52",
            null,
            "com.google.android.apps.youtube.music/7.12.52 (Linux; U; Android " + Build.VERSION.RELEASE + ") gzip",
            IS_YOUTUBE_MUSIC,
            true,
            false,
            false,
            false,
            "Android Music No SDK"
    ),
    /**
     * Video not playable: None.
     * For YouTube Music only.
     */
    ANDROID_MUSIC_REEL(
            21,
            "ANDROID_MUSIC",
            "com.google.android.apps.youtube.music",
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT),
            Build.ID,
            "9.05.52",
            null,
            IS_YOUTUBE_MUSIC,
            IS_YOUTUBE_MUSIC,
            false,
            false,
            false,
            true,
            false,
            "Android Music Reel"
    ),
    /**
     * Video not playable: Kids.
     * AV1 codec available.
     */
    // https://dumps.tadiphone.dev/dumps/oculus/eureka
    ANDROID_VR_1_74(
            28,
            "ANDROID_VR",
            "com.google.android.apps.youtube.vr.oculus",
            "Oculus",
            "Quest 3",
            "Android",
            "14",
            "34",
            "UP1A.231005.007.A1",
            "1.74.19",
            null,
            false,
            false,
            true,
            true,
            true,
            true,
            true,
            "Android VR 1.74"
    ),
    /**
     * Video not playable: Kids.
     * AV1 codec not available.
     */
    // https://dumps.tadiphone.dev/dumps/oculus/monterey
    ANDROID_VR_1_73(
            ANDROID_VR_1_74.id,
            ANDROID_VR_1_74.clientName,
            Objects.requireNonNull(ANDROID_VR_1_74.packageName),
            ANDROID_VR_1_74.deviceMake,
            "Quest",
            ANDROID_VR_1_74.osName,
            "10",
            "29",
            "QQ3A.200805.001",
            "1.73.24",
            ANDROID_VR_1_74.clientPlatform,
            ANDROID_VR_1_74.canLogin,
            ANDROID_VR_1_74.requireLogin,
            ANDROID_VR_1_74.supportsMultiAudioTracks,
            ANDROID_VR_1_74.supportsOAuth2,
            ANDROID_VR_1_74.supportsVRImmersiveMode,
            ANDROID_VR_1_74.requireSABR,
            ANDROID_VR_1_74.usePlayerEndpoint,
            "Android VR 1.73"
    ),
    /**
     * Video not playable: Livestream.
     * AV1 codec and HDR codec are not available, and the maximum resolution is 720p.
     */
    // https://dumps.tadiphone.dev/dumps/google/mustang
    ANDROID_CREATOR(
            14,
            "ANDROID_CREATOR",
            "com.google.android.apps.youtube.creator",
            "Google",
            "Pixel 10 Pro XL",
            "Android",
            "16",
            "36",
            "BD3A.251005.003.W3",
            "26.10.000",
            null,
            true,
            true,
            false,
            false,
            false,
            false,
            true,
            "Android Studio"
    ),
    /**
     * Video not playable: None.
     * AV1 codec available.
     */
    TV(7,
            "TVHTML5",
            "Samsung",
            "SmartTV",
            "Tizen",
            "2.4.0",
            "5.20150304",
            "TV",
            // Currently, it is the only User-Agent available for signed out among TV clients, but sign in is still required for certain IP bands or countries.
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/2.4.0 TV Safari/538.1",
            true,
            false,
            true,
            false,
            true,
            "TV"
    ),
    /**
     * Video not playable: Kids, Paid, Movie, Private, Age-restricted.
     * AV1 codec available.
     * May stop working at any time.
     */
    VISIONOS(101,
            "VISIONOS",
            "Apple",
            "RealityDevice17,1",
            "visionOS",
            "26.5.23O471",
            "1.03",
            null,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
            false,
            false,
            true,
            true,
            false,
            "visionOS"
    ),
    GET_CHANNEL_FROM_ID(
            ANDROID_REEL_AUTH.id,
            ANDROID_REEL_AUTH.clientName,
            Objects.requireNonNull(ANDROID_REEL_AUTH.packageName),
            ANDROID_REEL_AUTH.deviceMake,
            ANDROID_REEL_AUTH.deviceModel,
            ANDROID_REEL_AUTH.osName,
            ANDROID_REEL_AUTH.osVersion,
            Objects.requireNonNull(ANDROID_REEL_AUTH.androidSdkVersion),
            ANDROID_REEL_AUTH.buildID,
            ANDROID_REEL_AUTH.clientVersion,
            ANDROID_REEL_AUTH.clientPlatform,
            false,
            false,
            false,
            ANDROID_REEL_AUTH.supportsOAuth2,
            ANDROID_REEL_AUTH.supportsVRImmersiveMode,
            ANDROID_REEL_AUTH.requireSABR,
            ANDROID_REEL_AUTH.usePlayerEndpoint,
            "Get Channel From ID"
    ),
    SAVE_TO_WATCH_LATER(
            ANDROID_REEL_AUTH.id,
            ANDROID_REEL_AUTH.clientName,
            Objects.requireNonNull(ANDROID_REEL_AUTH.packageName),
            ANDROID_REEL_AUTH.deviceMake,
            ANDROID_REEL_AUTH.deviceModel,
            ANDROID_REEL_AUTH.osName,
            ANDROID_REEL_AUTH.osVersion,
            Objects.requireNonNull(ANDROID_REEL_AUTH.androidSdkVersion),
            ANDROID_REEL_AUTH.buildID,
            ANDROID_REEL_AUTH.clientVersion,
            ANDROID_REEL_AUTH.clientPlatform,
            true,
            true,
            false,
            ANDROID_REEL_AUTH.supportsOAuth2,
            ANDROID_REEL_AUTH.supportsVRImmersiveMode,
            ANDROID_REEL_AUTH.requireSABR,
            ANDROID_REEL_AUTH.usePlayerEndpoint,
            "Save To Watch Later"
    );

    public final int id;
    public final String clientName;
    @Nullable public final String packageName;
    public final String userAgent;
    public final String deviceMake;
    public final String deviceModel;
    public final String osName;
    public final String osVersion;
    @Nullable public final String androidSdkVersion;
    public final String buildID;
    public final String clientVersion;
    public final String clientPlatform;
    public final boolean canLogin;
    public final boolean requireLogin;
    public final boolean supportsOAuth2;
    public final boolean supportsMultiAudioTracks;
    public final boolean supportsVRImmersiveMode;
    public final boolean requireJS;
    public final boolean requireSABR;
    public final boolean usePlayerEndpoint;
    public final String friendlyName;

    ClientType(int id,
               String clientName,
               @NonNull String packageName,
               String deviceMake,
               String deviceModel,
               String osName,
               String osVersion,
               @NonNull String androidSdkVersion,
               @NonNull String buildId,
               String clientVersion,
               String clientPlatform,
               boolean canLogin,
               boolean requireLogin,
               boolean supportsMultiAudioTracks,
               boolean supportsOAuth2,
               boolean supportsVRImmersiveMode,
               boolean requireSABR,
               boolean usePlayerEndpoint,
               String friendlyName) {
        this.id = id;
        this.clientName = clientName;
        this.packageName = packageName;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.androidSdkVersion = androidSdkVersion;
        this.buildID = buildId;
        this.clientVersion = clientVersion;
        this.clientPlatform = clientPlatform;
        this.canLogin = canLogin;
        this.requireLogin = requireLogin;
        this.requireSABR = requireSABR;
        this.supportsMultiAudioTracks = supportsMultiAudioTracks;
        this.supportsOAuth2 = supportsOAuth2;
        this.supportsVRImmersiveMode = supportsVRImmersiveMode;
        this.usePlayerEndpoint = usePlayerEndpoint;
        this.friendlyName = friendlyName;

        Locale defaultLocale = Locale.getDefault();
        this.userAgent = String.format(Locale.ENGLISH,
                "%s/%s (Linux; U; Android %s; %s; %s; Build/%s)",
                packageName,
                clientVersion,
                osVersion,
                defaultLocale,
                deviceModel,
                buildId
        );

        requireJS = false;
    }

    ClientType(int id,
               String clientName,
               String deviceMake,
               String deviceModel,
               String osName,
               String osVersion,
               String clientVersion,
               String clientPlatform,
               String userAgent,
               boolean canLogin,
               boolean requireLogin,
               boolean supportsMultiAudioTracks,
               boolean supportsVRImmersiveMode,
               boolean requireJS,
               String friendlyName) {
        this.id = id;
        this.clientName = clientName;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.clientVersion = clientVersion;
        this.clientPlatform = clientPlatform;
        this.userAgent = userAgent;
        this.canLogin = canLogin;
        this.requireLogin = requireLogin;
        this.supportsMultiAudioTracks = supportsMultiAudioTracks;
        this.supportsVRImmersiveMode = supportsVRImmersiveMode;
        this.requireJS = requireJS;
        this.friendlyName = friendlyName;

        androidSdkVersion = null;
        buildID = null;
        packageName = null;
        requireSABR = false;
        supportsOAuth2 = false;
        usePlayerEndpoint = true;
    }
}
