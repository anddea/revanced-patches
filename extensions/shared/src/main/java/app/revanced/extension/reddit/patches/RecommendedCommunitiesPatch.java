package app.revanced.extension.reddit.patches;

import app.revanced.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class RecommendedCommunitiesPatch {

    public static boolean hideRecommendedCommunitiesShelf() {
        return Settings.HIDE_RECOMMENDED_COMMUNITIES_SHELF.get();
    }

}
