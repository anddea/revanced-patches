package app.morphe.extension.music.patches.ads;

import app.morphe.extension.music.settings.Settings;

@SuppressWarnings("unused")
public class MusicAdsPatch {
    private static final boolean HIDE_MUSIC_ADS = Settings.HIDE_MUSIC_ADS.get();

    public static boolean hideMusicAds() {
        return HIDE_MUSIC_ADS;
    }

    public static boolean hideMusicAds(boolean original) {
        return !HIDE_MUSIC_ADS && original;
    }
}
