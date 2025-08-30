package app.revanced.extension.music.patches.spoof;

import android.os.Build;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("deprecation")
public enum ClientType {
    ANDROID_VR_1_43_32(28,
            "Oculus",
            "Oculus",
            "Quest",
            "Android",
            "7.1.1",
            "25",
            "NGI77B",
            "com.google.android.apps.youtube.vr.oculus",
            "ANDROID_VR",
            "1.43.32",
            false
    ),
    ANDROID_VR_1_65_09(ANDROID_VR_1_43_32.id,
            ANDROID_VR_1_43_32.deviceBrand,
            ANDROID_VR_1_43_32.deviceMake,
            "Quest 3",
            ANDROID_VR_1_43_32.osName,
            "14",
            "34",
            "UP1A.231005.007.A1",
            ANDROID_VR_1_43_32.packageName,
            ANDROID_VR_1_43_32.clientName,
            "1.65.09",
            false
    ),
    IOS_MUSIC_6_21(26,
            "Apple",
            "Apple",
            "iPhone16,2",
            "iOS",
            "17.0.2.21A350",
            null,
            null,
            "com.google.ios.youtubemusic",
            "IOS_MUSIC",
            "6.21",
            false
    ),
    IOS_MUSIC_7_04(IOS_MUSIC_6_21.id,
            IOS_MUSIC_6_21.deviceBrand,
            IOS_MUSIC_6_21.deviceMake,
            IOS_MUSIC_6_21.deviceModel,
            IOS_MUSIC_6_21.osName,
            IOS_MUSIC_6_21.osVersion,
            IOS_MUSIC_6_21.androidSdkVersion,
            IOS_MUSIC_6_21.buildId,
            IOS_MUSIC_6_21.packageName,
            IOS_MUSIC_6_21.clientName,
            "7.04",
            false
    ),
    IOS_MUSIC_8_12(IOS_MUSIC_6_21.id,
            IOS_MUSIC_6_21.deviceBrand,
            IOS_MUSIC_6_21.deviceMake,
            IOS_MUSIC_6_21.deviceModel,
            IOS_MUSIC_6_21.osName,
            "18.6.1.22G90",
            IOS_MUSIC_6_21.androidSdkVersion,
            IOS_MUSIC_6_21.buildId,
            IOS_MUSIC_6_21.packageName,
            IOS_MUSIC_6_21.clientName,
            "8.12",
            false
    ),
    IOS_MUSIC_8_34(IOS_MUSIC_8_12.id,
            IOS_MUSIC_8_12.deviceBrand,
            IOS_MUSIC_8_12.deviceMake,
            IOS_MUSIC_8_12.deviceModel,
            IOS_MUSIC_8_12.osName,
            IOS_MUSIC_8_12.osVersion,
            IOS_MUSIC_8_12.androidSdkVersion,
            IOS_MUSIC_8_12.buildId,
            IOS_MUSIC_8_12.packageName,
            IOS_MUSIC_8_12.clientName,
            "8.34",
            false
    ),
    IOS_MUSIC_6_21_BLOCK_REQUEST(IOS_MUSIC_6_21.id,
            IOS_MUSIC_6_21.deviceBrand,
            IOS_MUSIC_6_21.deviceMake,
            IOS_MUSIC_6_21.deviceModel,
            IOS_MUSIC_6_21.osName,
            IOS_MUSIC_6_21.osVersion,
            IOS_MUSIC_6_21.androidSdkVersion,
            IOS_MUSIC_6_21.buildId,
            IOS_MUSIC_6_21.packageName,
            IOS_MUSIC_6_21.clientName,
            IOS_MUSIC_6_21.clientVersion,
            true
    ),
    IOS_MUSIC_7_04_BLOCK_REQUEST(IOS_MUSIC_7_04.id,
            IOS_MUSIC_7_04.deviceBrand,
            IOS_MUSIC_7_04.deviceMake,
            IOS_MUSIC_7_04.deviceModel,
            IOS_MUSIC_7_04.osName,
            IOS_MUSIC_7_04.osVersion,
            IOS_MUSIC_7_04.androidSdkVersion,
            IOS_MUSIC_7_04.buildId,
            IOS_MUSIC_7_04.packageName,
            IOS_MUSIC_7_04.clientName,
            IOS_MUSIC_7_04.clientVersion,
            true
    ),
    IOS_MUSIC_8_12_BLOCK_REQUEST(IOS_MUSIC_8_12.id,
            IOS_MUSIC_8_12.deviceBrand,
            IOS_MUSIC_8_12.deviceMake,
            IOS_MUSIC_8_12.deviceModel,
            IOS_MUSIC_8_12.osName,
            IOS_MUSIC_8_12.osVersion,
            IOS_MUSIC_8_12.androidSdkVersion,
            IOS_MUSIC_8_12.buildId,
            IOS_MUSIC_8_12.packageName,
            IOS_MUSIC_8_12.clientName,
            IOS_MUSIC_8_12.clientVersion,
            true
    ),
    IOS_MUSIC_8_34_BLOCK_REQUEST(IOS_MUSIC_8_34.id,
            IOS_MUSIC_8_34.deviceBrand,
            IOS_MUSIC_8_34.deviceMake,
            IOS_MUSIC_8_34.deviceModel,
            IOS_MUSIC_8_34.osName,
            IOS_MUSIC_8_34.osVersion,
            IOS_MUSIC_8_34.androidSdkVersion,
            IOS_MUSIC_8_34.buildId,
            IOS_MUSIC_8_34.packageName,
            IOS_MUSIC_8_34.clientName,
            IOS_MUSIC_8_34.clientVersion,
            true
    );

    /**
     * YouTube
     * <a href="https://github.com/zerodytrash/YouTube-Internal-Clients?tab=readme-ov-file#clients">client type</a>
     */
    public final int id;

    /**
     * Device brand.
     */
    public final String deviceBrand;

    /**
     * Device make.
     */
    public final String deviceMake;

    /**
     * Device model.
     */
    public final String deviceModel;

    /**
     * Device OS name.
     */
    public final String osName;

    /**
     * Device OS version.
     */
    public final String osVersion;

    /**
     * Android SDK version, equivalent to {@link Build.VERSION#SDK} (System property: ro.build.version.sdk)
     * Field is null if not applicable.
     */
    @Nullable
    public final String androidSdkVersion;

    /**
     * Android build id, equivalent to {@link Build#ID}.
     * Field is null if not applicable.
     */
    @Nullable
    private final String buildId;

    /**
     * App package name.
     */
    private final String packageName;

    /**
     * Client name.
     */
    public final String clientName;

    /**
     * App version.
     */
    public final String clientVersion;

    /**
     * Client user-agent.
     */
    public final String userAgent;

    public final boolean blockRequest;

    @SuppressWarnings("ConstantLocale")
    ClientType(int id,
               String deviceBrand,
               String deviceMake,
               String deviceModel,
               String osName,
               String osVersion,
               @Nullable String androidSdkVersion,
               @Nullable String buildId,
               String packageName,
               String clientName,
               String clientVersion,
               boolean blockRequest) {
        this.id = id;
        this.deviceBrand = deviceBrand;
        this.deviceMake = deviceMake;
        this.deviceModel = deviceModel;
        this.osName = osName;
        this.osVersion = osVersion;
        this.androidSdkVersion = androidSdkVersion;
        this.buildId = buildId;
        this.packageName = packageName;
        this.clientName = clientName;
        this.clientVersion = clientVersion;
        this.blockRequest = blockRequest;

        Locale defaultLocale = Locale.getDefault();
        if (androidSdkVersion == null) {
            // Convert version from '18.6.1.22G90' into '18_6_1'
            String userAgentOsVersion = osVersion
                    .substring(0, StringUtils.lastIndexOf(osVersion, "."))
                    .replace(".", "_");
            this.userAgent = String.format(
                    Locale.ENGLISH,
                    "%s/%s (%s; U; CPU iOS %s like Mac OS X; %s)",
                    packageName,
                    clientVersion,
                    deviceModel,
                    userAgentOsVersion,
                    defaultLocale
            );
        } else {
            this.userAgent = String.format(
                    Locale.ENGLISH,
                    "%s/%s(Linux; U; Android %s; %s; %s; Build/%s) gzip",
                    packageName,
                    clientVersion,
                    osVersion,
                    defaultLocale,
                    deviceModel,
                    Objects.requireNonNull(buildId)
            );
        }
    }

}
