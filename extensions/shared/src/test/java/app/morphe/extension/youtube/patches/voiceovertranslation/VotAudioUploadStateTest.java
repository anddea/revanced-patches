package app.morphe.extension.youtube.patches.voiceovertranslation;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class VotAudioUploadStateTest {
    private final AtomicInteger uploads = new AtomicInteger();
    private final AtomicInteger failures = new AtomicInteger();
    private final AtomicInteger empty = new AtomicInteger();

    private void request(VotAudioUploadState state, String id, boolean success) {
        state.handle("https://youtu.be/example", id,
                () -> { uploads.incrementAndGet(); return success; },
                failures::incrementAndGet, empty::incrementAndGet);
    }

    @Test public void repeatedAudioRequestedAfterSuccessNeverSendsFailure() {
        VotAudioUploadState state = new VotAudioUploadState();
        for (int i = 0; i < 30; i++) request(state, "translation-1", true);
        assertEquals(1, uploads.get());
        assertEquals(0, failures.get());
        assertEquals(0, empty.get());
    }

    @Test public void failedDownloadReportsFallbackOnlyOnce() {
        VotAudioUploadState state = new VotAudioUploadState();
        for (int i = 0; i < 30; i++) request(state, "translation-1", false);
        assertEquals(1, uploads.get());
        assertEquals(1, failures.get());
        assertEquals(1, empty.get());
    }

    @Test public void newTranslationIdAllowsAnotherUpload() {
        VotAudioUploadState state = new VotAudioUploadState();
        request(state, "translation-1", false);
        request(state, "translation-2", true);
        request(state, "translation-2", true);
        assertEquals(2, uploads.get());
        assertEquals(1, failures.get());
        assertEquals(1, empty.get());
    }

    @Test public void missingIdDoesNotPreventLaterUpload() {
        VotAudioUploadState state = new VotAudioUploadState();
        request(state, null, false);
        request(state, "", false);
        request(state, "translation-1", true);
        assertEquals(1, uploads.get());
        assertEquals(1, failures.get());
        assertEquals(0, empty.get());
    }

    @Test public void oldRequestCompletionCannotSuppressNewRequest() {
        VotAudioUploadState oldRequest = new VotAudioUploadState();
        VotAudioUploadState newRequest = new VotAudioUploadState();
        request(newRequest, "same-id", true);
        request(oldRequest, "same-id", false);
        request(newRequest, "same-id", true);
        assertEquals(2, uploads.get());
        assertEquals(1, failures.get());
        assertEquals(1, empty.get());
    }
}
