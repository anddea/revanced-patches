package app.morphe.extension.youtube.patches.voiceovertranslation;

public class VoiceOverTranslationPatch {
    public static String channel, author, title;
    public static long length;
    public static boolean live;
    public static String pending = "";
    public static int starts, paused, suspended, attempts;
    public static boolean active, inflight;

    public static void newVideoStarted(String a, String b, String id, String c, long d, boolean e) {
        channel = a;
        author = b;
        title = c;
        length = d;
        live = e;
        pending = id;
        TranslationPlaybackController.metadataLoaded(id);
    }

    public static boolean startAutomaticTranslation(String id) {
        attempts++;
        if (!pending.equals(id)) return false;
        if (!active && !inflight) {
            starts++;
            inflight = true;
            TranslationPlaybackController.select(TranslationPlaybackState.YANDEX, id);
        }
        return true;
    }

    public static void suspendTranslation() {
        suspended++;
        active = false;
        inflight = false;
        TranslationPlaybackController.failed(TranslationPlaybackState.YANDEX, pending);
    }

    public static void pauseAudio() {
        paused++;
    }
}
