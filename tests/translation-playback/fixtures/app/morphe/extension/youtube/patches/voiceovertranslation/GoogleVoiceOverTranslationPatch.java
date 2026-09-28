package app.morphe.extension.youtube.patches.voiceovertranslation;

public class GoogleVoiceOverTranslationPatch {
    public static String id = "";
    public static int starts, paused, suspended, voiceChanges, reloads;

    public static void newVideoLoaded(String value) {
        id = value;
        TranslationPlaybackController.metadataLoaded(value);
    }

    public static void startAutomaticTranslation() {
        starts++;
        TranslationPlaybackController.select(TranslationPlaybackState.GOOGLE, id);
    }

    public static void suspendTranslation() {
        suspended++;
        TranslationPlaybackController.failed(TranslationPlaybackState.GOOGLE, id);
    }

    public static void onPlaybackPaused() {
        paused++;
    }

    public static void onVoiceChanged() {
        voiceChanges++;
    }

    public static void reloadTranscript() {
        reloads++;
    }
}
