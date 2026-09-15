package app.morphe.extension.youtube.patches.voiceovertranslation;

import org.junit.Test;
import static org.junit.Assert.*;
import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;

public class TranslationPlaybackStateTest {
    private final TranslationPlaybackState state = new TranslationPlaybackState();

    @Test public void firstPlayIsBlockedBeforeMetadataArrives() {
        state.reset(YANDEX, true);
        assertFalse(state.filterPlay(true, false));
        state.newVideo("a", YANDEX, true);
        state.select("a", YANDEX, true, false);
        assertTrue(state.ready(YANDEX, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test public void everyNewVideoIsHeldIncludingReturningToPreviousVideo() {
        state.reset(YANDEX, true);
        for (String id : new String[]{"a", "b", "a"}) {
            assertTrue(state.newVideo(id, YANDEX, true));
            assertFalse(state.filterPlay(true, false));
            assertTrue(state.ready(YANDEX, id));
        }
    }

    @Test public void duplicateMetadataCannotPauseAnAlreadyReadyVideo() {
        state.reset(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.ready(YANDEX, "a");
        assertFalse(state.newVideo("a", YANDEX, true));
        assertTrue(state.filterPlay(true, false));
    }

    @Test public void youtubeAutoplayCannotReleaseTheTranslationHold() {
        state.reset(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        for (int i = 0; i < 5; i++) assertFalse(state.filterPlay(true, false));
        assertTrue(state.isWaiting(YANDEX, "a"));
        assertTrue(state.ready(YANDEX, "a"));
    }

    @Test public void staleReadyOrFailureCannotReleaseNextVideo() {
        state.reset(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.newVideo("b", YANDEX, true);
        state.cancel(YANDEX, "a");
        assertFalse(state.ready(YANDEX, "a"));
        assertFalse(state.filterPlay(true, false));
    }

    @Test public void switchingProviderIgnoresThePreviousProvidersCompletion() {
        state.reset(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.select("a", GOOGLE, true, false);
        assertFalse(state.ready(YANDEX, "a"));
        state.cancel(YANDEX, "a");
        assertTrue(state.isWaiting(GOOGLE, "a"));
        assertTrue(state.ready(GOOGLE, "a"));
    }

    @Test public void failedTranslationAllowsManualPlaybackWithoutAutomaticallyStartingIt() {
        state.reset(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.cancel(GOOGLE, "a");
        assertFalse(state.ready(GOOGLE, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test public void disabledPauseDoesNotInterceptPlayback() {
        state.reset(YANDEX, false);
        state.newVideo("a", YANDEX, false);
        assertTrue(state.filterPlay(true, false));
        assertFalse(state.ready(YANDEX, "a"));
    }

    @Test public void translationStartedOnManualPauseDoesNotResumeVideo() {
        state.reset(GOOGLE, false);
        state.newVideo("a", GOOGLE, false);
        state.select("a", GOOGLE, true, false);
        assertFalse(state.ready(GOOGLE, "a"));
    }

    @Test public void userPauseWhileWaitingCancelsAutomaticResume() {
        state.reset(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, false);
        assertFalse(state.ready(GOOGLE, "a"));
    }

    @Test public void internalPauseDoesNotCancelAutomaticResume() {
        state.reset(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, true);
        assertTrue(state.ready(GOOGLE, "a"));
    }

    @Test public void selectingTranslationPreservesAnEarlierManualPause() {
        state.reset(YANDEX, true);
        state.filterPlay(false, false);
        state.newVideo("a", YANDEX, true);
        state.select("a", YANDEX, true, false);
        assertFalse(state.ready(YANDEX, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test public void metadataArmsPauseEvenIfEarlierRequestWasCancelled() {
        state.reset(YANDEX, true);
        state.cancel(YANDEX, "");
        state.newVideo("a", YANDEX, true);
        assertFalse(state.filterPlay(true, false));
    }
    @Test public void cachedMetadataCannotClaimANewPlayerBeforeItsActualIdArrives() {
        state.reset(GOOGLE, true);
        state.newVideo("old", GOOGLE, true);
        state.ready(GOOGLE, "old");
        state.reset(GOOGLE, true);
        assertFalse(state.matchesVideo("old"));
        assertFalse(state.matchesVideo(""));
        assertFalse(state.ready(GOOGLE, "old"));
        assertFalse(state.filterPlay(true, false));
        state.newVideo("new", GOOGLE, true);
        assertFalse(state.matchesVideo("old"));
        assertTrue(state.matchesVideo("new"));
        assertTrue(state.ready(GOOGLE, "new"));
    }

}
