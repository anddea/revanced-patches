package app.morphe.extension.youtube.patches.voiceovertranslation;

import java.util.*;

class TranscriptSegment {
    long durationMs = -1;
    long startMs, playbackEndMs = 10000;
    String lang = "ru", text = "hello";
}

class TtsPrefetcher {
    static long time;
    static int updates, clears, rescans;
    static String video;

    static void triggerRescan() {
        rescans++;
    }

    static void updateTime(long t) {
        time = t;
    }

    static void updateVideo(String id, List<TranscriptSegment> s) {
        updates++;
        video = id;
    }

    static void clear() {
        clears++;
    }
}

class TranscriptFetcher {
    static boolean isSpokenLanguageDifferent(String a, String b) {
        return !a.equals(b);
    }
}

class TranscriptTranslator {
    static boolean awaiting;
    static int aborted;

    static void requestAbort() {
        aborted++;
    }

    static boolean isAwaitingTranslationAt(int i, long time, String text) {
        return awaiting;
    }
}

class TtsCache {
    static byte[] audio;
    static String id = "a", voice = "edge", lang = "ru", text = "hello";
    static int index;

    static byte[] get(String v, int i, String vc, String l, String t) {
        return id.equals(v) && index == i && voice.equals(vc) && lang.equals(l) && text.equals(t)
                ? audio
                : null;
    }
}

/** Deterministic voice-discovery boundaries; these fixtures never contact Microsoft. */
class VoiceCatalog {
    static boolean available = true;

    static String resolve(String lang, String preferred) {
        return available ? "edge" : null;
    }
}

class EdgeTtsVoiceRepository {
    static Runnable callback;

    static void load(Object context, Runnable onUpdate) {
        callback = onUpdate;
    }
}
