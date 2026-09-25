package app.morphe.extension.youtube.settings;

public class Settings {
    public static final Flag GOOGLE_VOT_SESSION_ENABLED = new Flag("session");

    public static class Flag {
        public final String key;
        public boolean value;

        Flag(String k) {
            key = k;
        }

        public boolean get() {
            return value;
        }

        public void save(boolean v) {
            value = v;
        }
    }

    public static final Flag VOT_ENABLED = new Flag("VOT_ENABLED");
    public static final Flag VOT_AUTO_TRANSLATE = new Flag("VOT_AUTO_TRANSLATE");
    public static final Flag VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION =
            new Flag("VOT_PAUSE_VIDEO_WHILE_PREPARING_TRANSLATION");
    public static final Flag GOOGLE_VOT_ENABLED = new Flag("GOOGLE_VOT_ENABLED");
    public static final Flag GOOGLE_VOT_AUTO_TRANSLATE = new Flag("GOOGLE_VOT_AUTO_TRANSLATE");
    public static final Flag GOOGLE_VOT_PAUSE_WHILE_PREPARING =
            new Flag("GOOGLE_VOT_PAUSE_WHILE_PREPARING");
    public static final Flag GOOGLE_VOT_USE_NATIVE_TTS = new Flag("GOOGLE_VOT_USE_NATIVE_TTS");
    public static final Flag GOOGLE_VOT_TTS_VOICE_TYPE = new Flag("GOOGLE_VOT_TTS_VOICE_TYPE");
    public static final Flag GOOGLE_VOT_TRANSLATION_SERVICE =
            new Flag("GOOGLE_VOT_TRANSLATION_SERVICE");
    public static final Flag GOOGLE_VOT_CAPTION_LANGUAGE = new Flag("GOOGLE_VOT_CAPTION_LANGUAGE");
    public static final Flag GOOGLE_VOT_OPENROUTER_MODEL = new Flag("GOOGLE_VOT_OPENROUTER_MODEL");
}
