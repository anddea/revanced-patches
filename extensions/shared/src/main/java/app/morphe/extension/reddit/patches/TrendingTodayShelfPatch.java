package app.morphe.extension.reddit.patches;

import app.morphe.extension.reddit.settings.Settings;

@SuppressWarnings("unused")
public final class TrendingTodayShelfPatch {

    public static boolean hideTrendingTodayShelf() {
        return Settings.HIDE_TRENDING_TODAY_SHELF.get();
    }

}
