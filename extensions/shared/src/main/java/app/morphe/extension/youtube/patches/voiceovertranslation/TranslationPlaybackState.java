package app.morphe.extension.youtube.patches.voiceovertranslation;

/** Per-video ownership of the initial translation pause. No Android dependencies. */
final class TranslationPlaybackState {
    static final int NONE = 0, YANDEX = 1, GOOGLE = 2;
    private String videoId = "";
    private int provider;
    private boolean waiting;
    private boolean resume;

    synchronized void reset(int provider, boolean pause) {
        videoId = "";
        this.provider = provider;
        waiting = pause && provider != NONE;
        resume = waiting;
    }

    synchronized boolean newVideo(String id, int provider, boolean pause) {
        if (id.isEmpty() || id.equals(videoId)) return false;
        // Preserve the early constructor pause when the metadata arrives.
        if (!videoId.isEmpty()) reset(provider, pause);
        else if (this.provider != provider || (!waiting && pause)) reset(provider, pause);
        videoId = id;
        return true;
    }

    synchronized void select(String id, int provider, boolean pause, boolean playing) {
        boolean alreadyHeld = waiting && id.equals(videoId);
        videoId = id;
        this.provider = provider;
        waiting = pause;
        resume = pause && (alreadyHeld ? resume : playing);
    }

    synchronized boolean filterPlay(boolean playing, boolean internalChange) {
        if (!waiting) return playing;
        if (playing) {
            resume = true;
            return false;
        }
        if (!internalChange) resume = false;
        return false;
    }

    synchronized boolean isWaiting(int provider, String id) {
        return waiting && this.provider == provider && videoId.equals(id);
    }

    synchronized boolean matchesVideo(String id) { return !id.isEmpty() && videoId.equals(id); }
    synchronized boolean isWaiting() { return waiting; }
    synchronized int provider() { return provider; }
    synchronized String videoId() { return videoId; }

    synchronized boolean ready(int provider, String id) {
        if (!isWaiting(provider, id)) return false;
        boolean shouldResume = resume;
        waiting = false;
        resume = false;
        return shouldResume;
    }

    synchronized void cancel(int provider, String id) {
        if (this.provider == provider && videoId.equals(id)) {
            waiting = false;
            resume = false;
        }
    }
}
