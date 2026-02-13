package app.morphe.extension.youtube.patches.general;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.youtube.settings.Settings;
import app.morphe.extension.youtube.utils.ExtendedUtils;
import app.morphe.extension.youtube.utils.VideoUtils;

@SuppressWarnings({"deprecation", "unused"})
public final class YouTubeMusicActionsPatch extends VideoUtils {

    private static final String PACKAGE_NAME_YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    private static final boolean isOverrideYouTubeMusicEnabled =
            Settings.OVERRIDE_YOUTUBE_MUSIC_BUTTON.get();

    private static final boolean overrideYouTubeMusicEnabled =
            isOverrideYouTubeMusicEnabled && ExtendedUtils.isPackageEnabled(PACKAGE_NAME_YOUTUBE_MUSIC);

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

    public static final class HookYouTubeMusicPackageNameAvailability implements Setting.Availability {
        @Override
        public boolean isAvailable() {
            return Settings.OVERRIDE_YOUTUBE_MUSIC_BUTTON.get();
        }

        @Override
        public List<Setting<?>> getParentSettings() {
            return List.of(Settings.OVERRIDE_YOUTUBE_MUSIC_BUTTON);
        }
    }

}
