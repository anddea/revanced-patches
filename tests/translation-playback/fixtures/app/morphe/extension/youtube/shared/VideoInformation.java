package app.morphe.extension.youtube.shared;

import app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackController;

public class VideoInformation {
    public static long time;

    public static long getVideoTime() {
        return time;
    }

    public static String channel, author, title;
    public static long length;
    public static boolean live;
    public static Object player;
    public static String id = "";
    public static boolean playing, fail, available = true;
    public static int playCalls, pauseCalls;

    public static boolean isCurrentPlayer(Object p) {
        return p == player;
    }

    public static String getVideoId() {
        return id;
    }

    public static boolean isPlayerPlaying() {
        return playing;
    }

    public static boolean setPlayerPlaying(boolean value) {
        if (fail) throw new IllegalStateException("backend");
        if (!available) return false;
        if (value) playCalls++;
        else pauseCalls++;
        playing = TranslationPlaybackController.overridePlayWhenReady(player, value);
        return true;
    }

    public static void setVideoInformation(
            String a, String b, String c, String d, long e, boolean f) {
        channel = a;
        author = b;
        id = c;
        title = d;
        length = e;
        live = f;
    }
}
