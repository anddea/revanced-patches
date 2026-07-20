/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - anddea (https://github.com/anddea)
 * - ILoveOpenSourceApplications (https://github.com/ILoveOpenSourceApplications)
 * - inotia00 (https://github.com/inotia00)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

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
        DEFAULT(""),

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
