package app.revanced.extension.music.patches.general;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public final class ChangeStartPagePatch {

    public enum StartPage {
        /**
         * Unmodified type, and same as un-patched.
         */
        ORIGINAL(""),

        /**
         * Browse id.
         */
        CHARTS("FEmusic_charts"),
        EXPLORE("FEmusic_explore"),
        HISTORY("FEmusic_history"),
        LIBRARY("FEmusic_library_landing"),
        PODCASTS("FEmusic_non_music_audio"),
        SAMPLES("FEmusic_immersive"),
        SUBSCRIPTIONS("FEmusic_library_corpus_artists"),

        /**
         * Playlist id, this can be used as a browseId.
         */
        EPISODES_FOR_LATER("VLSE"),
        LIKED_MUSIC("VLLM"),

        /**
         * Intent extra.
         */
        SEARCH("", 1, "Eh4IBRDTnQEYmgMiEwiZn+H0r5WLAxVV5OcDHcHRBmPqpd25AQA=");

        @NonNull
        final String browseId;

        final int shortcutType;

        /**
         * Unique identifier for shortcut (Base64).
         */
        @NonNull
        final String shortcutId;

        StartPage(@NonNull String browseId) {
            this(browseId, 0, "");
        }

        StartPage(@NonNull String browseId, int shortcutType, @NonNull String shortcutId) {
            this.browseId = browseId;
            this.shortcutType = shortcutType;
            this.shortcutId = shortcutId;
        }
    }

    /**
     * Intent action when YouTube is cold started from the launcher.
     */
    private static final String ACTION_MAIN = "android.intent.action.MAIN";

    private static final String SHORTCUT_ACTION = "com.google.android.youtube.music.action.shortcut";

    private static final String SHORTCUT_CLASS_DESCRIPTOR = "com.google.android.apps.youtube.music.activities.InternalMusicActivity";

    private static final String SHORTCUT_TYPE = "com.google.android.youtube.music.action.shortcut_type";

    private static final StartPage START_PAGE = Settings.CHANGE_START_PAGE.get();

    public static String overrideBrowseId(@NonNull String browseId) {
        if (!browseId.equals("FEmusic_home")) {
            return browseId;
        }
        final String overrideBrowseId = START_PAGE.browseId;
        if (overrideBrowseId.isEmpty()) {
            return browseId;
        }

        Logger.printDebug(() -> "Changing browseId to " + START_PAGE.name());
        return overrideBrowseId;
    }

    public static void overrideIntent(@NonNull Intent intent) {
        if (!StringUtils.equals(intent.getAction(), ACTION_MAIN)) {
            Logger.printDebug(() -> "Ignore override intent action" +
                    " as the current activity is not the entry point of the application");
            return;
        }
        final String overrideShortcutId = START_PAGE.shortcutId;
        if (overrideShortcutId.isEmpty()) {
            return;
        }
        Activity mActivity = ResourceUtils.getActivity();
        if (mActivity == null) {
            return;
        }

        Logger.printDebug(() -> "Changing intent action to " + START_PAGE.name());
        intent.setAction(SHORTCUT_ACTION);
        intent.setClassName(mActivity, SHORTCUT_CLASS_DESCRIPTOR);
        intent.setPackage(mActivity.getPackageName());
        intent.putExtra(SHORTCUT_TYPE, START_PAGE.shortcutType);
        intent.putExtra(SHORTCUT_ACTION, overrideShortcutId);
    }
}
