package app.revanced.extension.youtube.patches.general;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import app.revanced.extension.shared.settings.Setting;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;
import app.revanced.extension.youtube.utils.VideoUtils;

@SuppressWarnings("unused")
public final class YouTubeMusicActionsPatch extends VideoUtils {

    private static final String PACKAGE_NAME_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    private static final boolean isOverrideYouTubeMusicEnabled =
            Settings.OVERRIDE_YOUTUBE_MUSIC_BUTTON.get();

    private static final boolean overrideYouTubeMusicEnabled =
            isOverrideYouTubeMusicEnabled && isYouTubeMusicEnabled();

    public static String overridePackageName(@NonNull String packageName) {
        if (!overrideYouTubeMusicEnabled) {
            return packageName;
        }
        if (!StringUtils.equals(PACKAGE_NAME_YOUTUBE_MUSIC, packageName)) {
            return packageName;
        }
        final String thirdPartyPackageName = Settings.THIRD_PARTY_YOUTUBE_MUSIC_PACKAGE_NAME.get();
        if (!ExtendedUtils.isPackageEnabled(thirdPartyPackageName)) {
            return packageName;
        }
        return thirdPartyPackageName;
    }

    private static boolean isYouTubeMusicEnabled() {
        return ExtendedUtils.isPackageEnabled(PACKAGE_NAME_YOUTUBE_MUSIC);
    }

    public static final class HookYouTubeMusicAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return isYouTubeMusicEnabled();
        }
    }

    public static final class HookYouTubeMusicPackageNameAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return isOverrideYouTubeMusicEnabled && isYouTubeMusicEnabled();
        }
    }

}
