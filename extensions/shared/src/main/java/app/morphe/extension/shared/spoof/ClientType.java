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

import app.morphe.extension.shared.requests.Route;
import app.morphe.extension.shared.spoof.requests.PlayerRoutes;

@SuppressWarnings("ConstantLocale")
public enum ClientType {

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
            "20.26.46",
            null,
            IS_YOUTUBE,
            IS_YOUTUBE,
            true,
            false,
            false,
            PlayerRoutes.GET_REEL_STREAMING_DATA,
            "Android Reel auth"
    ),
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
            ANDROID_REEL_AUTH.requireJS,
            ANDROID_REEL_AUTH.endpoint,
            "Android Reel no auth"
    ),
    ANDROID_MUSIC_NO_SDK(
            21,
            "ANDROID_MUSIC",
            ANDROID_REEL_AUTH.deviceMake,
            ANDROID_REEL_AUTH.deviceModel,
            ANDROID_REEL_AUTH.osName,
            ANDROID_REEL_AUTH.osVersion,
            "7.12.52",
            null,
            "com.google.android.apps.youtube.music/7.12.52 (Linux; U; Android " + Build.VERSION.RELEASE + ") gzip",
            IS_YOUTUBE_MUSIC,
            true,
            false,
            false,
            false,
            PlayerRoutes.GET_PLAYER_STREAMING_DATA,
            "Android Music No SDK"
    ),
    ANDROID_VR_1_65(
            28,
            "ANDROID_VR",
            "com.google.android.apps.youtube.vr.oculus",
            "Oculus",
            "Quest 3",
            "Android",
            "14",
            "34",
            "UP1A.231005.007.A1",
            "1.65.10",
            null,
            false,
            false,
            false,
            true,
            false,
            PlayerRoutes.GET_PLAYER_STREAMING_DATA,
            "Android VR 1.65"
    ),
    ANDROID_VR_1_64(
            ANDROID_VR_1_65.id,
            ANDROID_VR_1_65.clientName,
            Objects.requireNonNull(ANDROID_VR_1_65.packageName),
            ANDROID_VR_1_65.deviceMake,
            "Quest",
            ANDROID_VR_1_65.osName,
            "10",
            "29",
            "QQ3A.200805.001",
            "1.64.34",
            ANDROID_VR_1_65.clientPlatform,
            ANDROID_VR_1_65.canLogin,
            ANDROID_VR_1_65.requireLogin,
            ANDROID_VR_1_65.supportsMultiAudioTracks,
            ANDROID_VR_1_65.supportsOAuth2,
            ANDROID_VR_1_65.requireJS,
            ANDROID_VR_1_65.endpoint,
            "Android VR 1.64"
    ),
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
            PlayerRoutes.GET_PLAYER_STREAMING_DATA,
            "Android Studio"
    ),
    TV(7,
            "TVHTML5",
            "Samsung",
            "SmartTV",
            "Tizen",
            "2.4.0",
            "5.20150304",
            "TV",
            "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/2.4.0 TV Safari/538.1",
            true,
            false,
            true,
            false,
            true,
            PlayerRoutes.GET_PLAYER_STREAMING_DATA,
            "TV"
    ),
    VISIONOS(101,
            "VISIONOS",
            "Apple",
            "RealityDevice14,1",
            "visionOS",
            "1.3.21O771",
            "0.1",
            null,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15",
            false,
            false,
            false,
            false,
            false,
            PlayerRoutes.GET_PLAYER_STREAMING_DATA,
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
            ANDROID_REEL_AUTH.requireJS,
            PlayerRoutes.GET_CHANNEL_FROM_ID,
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
            ANDROID_REEL_AUTH.requireJS,
            PlayerRoutes.SEND_SAVE_VIDEO_TO_WATCH_LATER,
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
    public final boolean requireJS;
    public final Route.CompiledRoute endpoint;
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
               boolean requireJS,
               Route.CompiledRoute endpoint,
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
        this.supportsMultiAudioTracks = supportsMultiAudioTracks;
        this.supportsOAuth2 = supportsOAuth2;
        this.requireJS = requireJS;
        this.endpoint = endpoint;
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
               boolean supportsOAuth2,
               boolean requireJS,
               Route.CompiledRoute endpoint,
               String friendlyName) {
        this.id = id;
        this.clientName = clientName;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.buildID = null;
        this.clientVersion = clientVersion;
        this.clientPlatform = clientPlatform;
        this.userAgent = userAgent;
        this.canLogin = canLogin;
        this.requireLogin = requireLogin;
        this.supportsMultiAudioTracks = supportsMultiAudioTracks;
        this.supportsOAuth2 = supportsOAuth2;
        this.requireJS = requireJS;
        this.endpoint = endpoint;
        this.friendlyName = friendlyName;

        this.packageName = null;
        this.androidSdkVersion = null;
    }
}
