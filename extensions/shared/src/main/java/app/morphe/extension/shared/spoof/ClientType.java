/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

package app.morphe.extension.shared.spoof;

import static app.morphe.extension.shared.patches.AppCheckPatch.IS_YOUTUBE_MUSIC;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

@SuppressWarnings({"ConstantLocale"})
public enum ClientType {
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
     * AV1 codec not available.
     */
    ANDROID_VR_SABR(
            28,
            "ANDROID_VR",
            "com.google.android.apps.youtube.vr.pico",
            "Pico",
            "A8110", // PICO 4.
            "Android",
            "10",
            "29",
            "5.13.7",
            "1.73.21",
            null,
            false,
            true,
            true,
            true,
            true,
            true,
            true,
            "Android VR"
    ),
    /**
     * Same as {@code ANDROID_VR_SABR} but supports dash streams.
     */
    ANDROID_VR_DASH(
            ANDROID_VR_SABR.id,
            ANDROID_VR_SABR.clientName,
            ANDROID_VR_SABR.packageName,
            ANDROID_VR_SABR.deviceMake,
            ANDROID_VR_SABR.deviceModel,
            ANDROID_VR_SABR.osName,
            ANDROID_VR_SABR.osVersion,
            ANDROID_VR_SABR.androidSdkVersion,
            ANDROID_VR_SABR.buildID,
            "1.64.34",
            ANDROID_VR_SABR.clientPlatform,
            ANDROID_VR_SABR.canLogin,
            ANDROID_VR_SABR.requireLogin,
            false,
            ANDROID_VR_SABR.supportsOAuth2,
            ANDROID_VR_SABR.supportsVRImmersiveMode,
            false,
            ANDROID_VR_SABR.usePlayerEndpoint,
            "Android VR Downgraded"
    ),
    /**
     * Same as {@code ANDROID_VR_SABR} but supports AV1 codec.
     */
    ANDROID_XR_SABR(
            ANDROID_VR_SABR.id,
            ANDROID_VR_SABR.clientName,
            "com.google.android.apps.youtube.xr",
            "Samsung",
            "SM-I610", // Galaxy XR.
            ANDROID_VR_SABR.osName,
            "14",
            "34",
            "UML1.250710.002.A1",
            "1.73.21",
            ANDROID_VR_SABR.clientPlatform,
            ANDROID_VR_SABR.canLogin,
            ANDROID_VR_SABR.requireLogin,
            ANDROID_VR_SABR.supportsMultiAudioTracks,
            ANDROID_VR_SABR.supportsOAuth2,
            ANDROID_VR_SABR.supportsVRImmersiveMode,
            ANDROID_VR_SABR.requireSABR,
            ANDROID_VR_SABR.usePlayerEndpoint,
            "Android XR"
    ),
    /**
     * Same as {@code ANDROID_XR_SABR} but supports dash streams.
     */
    ANDROID_XR_DASH(
            ANDROID_XR_SABR.id,
            ANDROID_XR_SABR.clientName,
            ANDROID_XR_SABR.packageName,
            ANDROID_XR_SABR.deviceMake,
            ANDROID_XR_SABR.deviceModel,
            ANDROID_XR_SABR.osName,
            ANDROID_XR_SABR.osVersion,
            ANDROID_XR_SABR.androidSdkVersion,
            ANDROID_XR_SABR.buildID,
            "1.69.27",
            ANDROID_XR_SABR.clientPlatform,
            ANDROID_XR_SABR.canLogin,
            ANDROID_XR_SABR.requireLogin,
            false,
            ANDROID_XR_SABR.supportsOAuth2,
            ANDROID_XR_SABR.supportsVRImmersiveMode,
            false,
            ANDROID_XR_SABR.usePlayerEndpoint,
            "Android XR Downgraded"
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
     * Video not playable: Livestream.
     * AV1 codec available.
     */
    TV_SABR(
            7,
            "TVHTML5",
            "Sony",
            "PS4",
            "PlayStation 4",
            "",
            "7.20260707.07.00",
            "GAME_CONSOLE",
            "Mozilla/5.0 (PS4; Leanback Shell) Gecko/20100101 Firefox/65.0 LeanbackShell/01.00.01.75 Sony PS4/ (PS4, , no, CH)",
            true,
            false,
            true,
            false,
            true,
            false,
            true,
            "TV"
    ),
    /**
     * Same as {@code TV_SABR} but supports dash streams.
     * This client cannot be selected in the settings and is used only for livestreams.
     */
    TV_DASH(
            TV_SABR.id,
            TV_SABR.clientName,
            "Samsung",
            "SmartTV",
            "Tizen",
            "2.4.0",
            "5.20150304",
            "TV",
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/2.4.0 TV Safari/538.1",
            TV_SABR.canLogin,
            TV_SABR.requireLogin,
            TV_SABR.supportsMultiAudioTracks,
            TV_SABR.supportsVRImmersiveMode,
            TV_SABR.requireJS,
            TV_SABR.requirePoToken,
            false,
            "TV Downgraded"
    ),
    /**
     * Video not playable: None.
     * AV1 codec available.
     */
    TV_SIMPLY(
            75,
            "TVHTML5_SIMPLY",
            TV_SABR.deviceMake,
            TV_SABR.deviceModel,
            TV_SABR.osName,
            TV_SABR.osVersion,
            "1.1",
            TV_SABR.clientPlatform,
            TV_SABR.userAgent,
            true,
            // This client requires a PoToken for logout.
            false,
            TV_SABR.supportsMultiAudioTracks,
            TV_SABR.supportsVRImmersiveMode,
            TV_SABR.requireJS,
            true,
            false,
            "TV Simply"
    ),
    /**
     * Video not playable: Kids, Paid, Movie, Private, Age-restricted.
     * AV1 codec available.
     * May stop working at any time.
     */
    VISIONOS_1_03(
            101,
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
            false,
            false,
            "visionOS 1.03"
    ),
    /**
     * Video not playable: Kids, Paid, Movie, Private, Age-restricted.
     * AV1 codec not available.
     * May stop working at any time.
     */
    VISIONOS_1_02(
            VISIONOS_1_03.id,
            VISIONOS_1_03.clientName,
            VISIONOS_1_03.deviceMake,
            "RealityDevice14,1",
            VISIONOS_1_03.osName,
            "2.6.22O785",
            "1.02",
            VISIONOS_1_03.clientPlatform,
            VISIONOS_1_03.userAgent,
            VISIONOS_1_03.canLogin,
            VISIONOS_1_03.requireLogin,
            VISIONOS_1_03.supportsMultiAudioTracks,
            VISIONOS_1_03.supportsVRImmersiveMode,
            VISIONOS_1_03.requireJS,
            VISIONOS_1_03.requirePoToken,
            VISIONOS_1_03.requireSABR,
            "visionOS 1.02"
    ),
    GET_CHANNEL_FROM_ID(
            3,
            "ANDROID",
            "com.google.android.youtube",
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT),
            Build.ID,
            "20.47.62",
            null,
            false,
            false,
            false,
            false,
            true,
            true,
            false,
            "Get Channel From ID"
    ),
    SAVE_TO_WATCH_LATER(
            3,
            "ANDROID",
            "com.google.android.youtube",
            Build.MANUFACTURER,
            Build.MODEL,
            "Android",
            Build.VERSION.RELEASE,
            String.valueOf(Build.VERSION.SDK_INT),
            Build.ID,
            "20.47.62",
            null,
            true,
            true,
            false,
            false,
            true,
            true,
            false,
            "Save To Watch Later"
    );

    public final int id;
    public final String clientName;
    @NonNull
    public final String packageName;
    public final String userAgent;
    public final String deviceMake;
    public final String deviceModel;
    public final String osName;
    public final String osVersion;
    @NonNull
    public final String androidSdkVersion;
    public final String buildID;
    public final String clientVersion;
    public final String clientPlatform;
    public final boolean canLogin;
    public final boolean requireLogin;
    public final boolean supportsOAuth2;
    public final boolean supportsMultiAudioTracks;
    public final boolean supportsVRImmersiveMode;
    public final boolean requireJS;
    public final boolean requirePoToken;
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
                "%s/%s (Linux; U; Android %s; %s; %s Build/%s)",
                packageName,
                clientVersion,
                osVersion,
                defaultLocale,
                deviceModel,
                buildId
        );

        requireJS = false;
        requirePoToken = false;
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
               boolean requirePoToken,
               boolean requireSABR,
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
        this.requirePoToken = requirePoToken;
        this.requireSABR = requireSABR;
        this.friendlyName = friendlyName;

        androidSdkVersion = "";
        buildID = null;
        packageName = "";
        supportsOAuth2 = false;
        usePlayerEndpoint = true;
    }
}
