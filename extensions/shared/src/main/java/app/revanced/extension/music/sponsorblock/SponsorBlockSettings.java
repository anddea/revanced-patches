package app.revanced.extension.music.sponsorblock;

import androidx.annotation.NonNull;

import java.util.UUID;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.music.sponsorblock.objects.SegmentCategory;
import app.revanced.extension.shared.settings.Setting;

public class SponsorBlockSettings {
    private static boolean initialized;

    /**
     * @return if the user has ever voted, created a segment, or imported existing SB settings.
     */
    public static boolean userHasSBPrivateId() {
        return !Settings.SB_PRIVATE_USER_ID.get().isEmpty();
    }

    /**
     * Use this only if a user id is required (creating segments, voting).
     */
    @NonNull
    public static String getSBPrivateUserID() {
        String uuid = Settings.SB_PRIVATE_USER_ID.get();
        if (uuid.isEmpty()) {
            uuid = (UUID.randomUUID().toString() +
                    UUID.randomUUID().toString() +
                    UUID.randomUUID().toString())
                    .replace("-", "");
            Settings.SB_PRIVATE_USER_ID.save(uuid);
        }
        return uuid;
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        SegmentCategory.updateEnabledCategories();
    }

    /**
     * Updates internal data based on {@link Setting} values.
     */
    public static void updateFromImportedSettings() {
        SegmentCategory.loadAllCategoriesFromSettings();
    }
}
