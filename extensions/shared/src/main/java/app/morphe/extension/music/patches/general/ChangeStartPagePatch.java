package app.morphe.extension.music.patches.general;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.music.utils.ExtendedUtils;
import app.morphe.extension.shared.utils.Logger;
import app.morphe.extension.shared.utils.ResourceUtils;

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
        SEARCH("");

        @NonNull
        final String browseId;

        StartPage(@NonNull String browseId) {
            this.browseId = browseId;
        }

        public final String getBrowseId() {
            return this.browseId;
        }
    }

    /**
     * Intent action when YouTube is cold started from the launcher.
     */
    private static final String ACTION_MAIN = "android.intent.action.MAIN";

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
        if (START_PAGE != StartPage.SEARCH) {
            return;
        }
        Activity mActivity = ResourceUtils.getActivity();
        if (mActivity != null) {
            Logger.printDebug(() -> "Changing intent action to " + START_PAGE.name());
            ExtendedUtils.setSearchIntent(mActivity, intent);
        }
    }
}
