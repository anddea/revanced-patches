package app.morphe.extension.music.sponsorblock;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.UUID;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.sponsorblock.objects.SegmentCategory;
import app.morphe.extension.shared.settings.Setting;

public class SponsorBlockSettings {

    public static final Setting.ImportExportCallback SB_IMPORT_EXPORT_CALLBACK = new Setting.ImportExportCallback() {
        @Override
        public void settingsImported(@Nullable Context context) {
            SegmentCategory.loadAllCategoriesFromSettings();
        }

        @Override
        public void settingsExported(@Nullable Context context) {
        }
    };

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
}
