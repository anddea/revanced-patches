/*
 * Copyright (C) 2026 anddea
 *
 * This file is part of the revanced-patches project:
 * https://github.com/anddea/revanced-patches
 *
 * Original author(s):
 * - COOLak (https://github.com/COOLak)
 *
 * Licensed under the GNU General Public License v3.0.
 *
 * ------------------------------------------------------------------------
 * GPLv3 Section 7 – Additional Terms & Attribution Requirements
 * ------------------------------------------------------------------------
 *
 * This file contains substantial original work by the author(s) listed above.
 *
 * In accordance with Section 7 of the GNU General Public License v3.0,
 * the following additional terms apply to this file:
 *
 * 1. Source Credit Preservation (Section 7(b)): This specific copyright notice
 *    and the list of original authors above must be preserved in any copy
 *    or derivative work. You may add your own copyright notice below it,
 *    but you may not remove the original one.
 *
 * 2. Origin & Modification Marking (Section 7(c)): Modified versions must be
 *    clearly marked as such (e.g., by adding a "Modified by" line or a new
 *    copyright notice) and must not be misrepresented as the original work.
 *
 * 3. Version Control Attribution (Section 7(b)): Any ports or substantial
 *    modifications must retain historical authorship credit in version control
 *    systems (e.g., Git), listing original author(s) appropriately and
 *    modifiers as committers or co-authors.
 *
 * 4. User Interface Attribution (Section 7(b)): Any works containing or
 *    derived from this material must maintain a visible credit or
 *    acknowledgment to the original author(s) within the application's
 *    user interface (e.g., in an "About" or "Credits" section).
 */

package app.morphe.extension.youtube.patches.voiceovertranslation;

import static app.morphe.extension.youtube.patches.voiceovertranslation.TranslationPlaybackState.*;

import static org.junit.Assert.*;

import org.junit.Test;

public class TranslationPlaybackStateTest {
    private final TranslationPlaybackState state = new TranslationPlaybackState();

    @Test
    public void firstPlayIsBlockedBeforeMetadataArrives() {
        state.initialize(YANDEX, true);
        assertFalse(state.filterPlay(true, false));
        state.newVideo("a", YANDEX, true);
        state.select("a", YANDEX, true, false);
        assertTrue(state.ready(YANDEX, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void everyNewVideoIsHeldIncludingReturningToPreviousVideo() {
        state.initialize(YANDEX, true);
        for (String id : new String[] {"a", "b", "a"}) {
            assertTrue(state.newVideo(id, YANDEX, true));
            assertFalse(state.filterPlay(true, false));
            assertTrue(state.ready(YANDEX, id));
        }
    }

    @Test
    public void duplicateMetadataCannotPauseAnAlreadyReadyVideo() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.ready(YANDEX, "a");
        assertFalse(state.newVideo("a", YANDEX, true));
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void youtubeAutoplayCannotReleaseTheTranslationHold() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        for (int i = 0; i < 5; i++) assertFalse(state.filterPlay(true, false));
        assertTrue(state.isWaiting(YANDEX, "a"));
        assertTrue(state.ready(YANDEX, "a"));
    }

    @Test
    public void staleReadyOrFailureCannotReleaseNextVideo() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.newVideo("b", YANDEX, true);
        state.cancel(YANDEX, "a");
        assertFalse(state.ready(YANDEX, "a"));
        assertFalse(state.filterPlay(true, false));
    }

    @Test
    public void switchingProviderIgnoresThePreviousProvidersCompletion() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.select("a", GOOGLE, true, false);
        assertFalse(state.ready(YANDEX, "a"));
        state.cancel(YANDEX, "a");
        assertTrue(state.isWaiting(GOOGLE, "a"));
        assertTrue(state.ready(GOOGLE, "a"));
    }

    @Test
    public void failedTranslationAllowsManualPlaybackWithoutAutomaticallyStartingIt() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.cancel(GOOGLE, "a");
        assertFalse(state.ready(GOOGLE, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void disabledPauseDoesNotInterceptPlayback() {
        state.initialize(YANDEX, false);
        state.newVideo("a", YANDEX, false);
        assertTrue(state.filterPlay(true, false));
        assertFalse(state.ready(YANDEX, "a"));
    }

    @Test
    public void translationStartedOnManualPauseDoesNotResumeVideo() {
        state.initialize(GOOGLE, false);
        state.newVideo("a", GOOGLE, false);
        state.select("a", GOOGLE, true, false);
        assertFalse(state.ready(GOOGLE, "a"));
    }

    @Test
    public void userPauseWhileWaitingCancelsAutomaticResume() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, false);
        assertFalse(state.ready(GOOGLE, "a"));
    }

    @Test
    public void internalPauseDoesNotCancelAutomaticResume() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, true);
        assertTrue(state.ready(GOOGLE, "a"));
    }

    @Test
    public void selectingTranslationPreservesAnEarlierManualPause() {
        state.initialize(YANDEX, true);
        state.filterPlay(false, false);
        state.newVideo("a", YANDEX, true);
        state.select("a", YANDEX, true, false);
        assertFalse(state.ready(YANDEX, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void metadataArmsPauseEvenIfEarlierRequestWasCancelled() {
        state.initialize(YANDEX, true);
        state.cancel(YANDEX, "");
        state.newVideo("a", YANDEX, true);
        assertFalse(state.filterPlay(true, false));
    }

    @Test
    public void cachedMetadataCannotClaimANewPlayerBeforeItsActualIdArrives() {
        state.initialize(GOOGLE, true);
        state.newVideo("old", GOOGLE, true);
        state.ready(GOOGLE, "old");
        state.initialize(GOOGLE, true);
        assertFalse(state.matchesVideo("old"));
        assertFalse(state.matchesVideo(""));
        assertFalse(state.ready(GOOGLE, "old"));
        assertFalse(state.filterPlay(true, false));
        state.newVideo("new", GOOGLE, true);
        assertFalse(state.matchesVideo("old"));
        assertTrue(state.matchesVideo("new"));
        assertTrue(state.ready(GOOGLE, "new"));
    }

    @Test
    public void returningToPreparedTranslationDoesNotRearmTheHold() {
        for (int provider : new int[] {YANDEX, GOOGLE}) {
            TranslationPlaybackState state = new TranslationPlaybackState();
            state.initialize(provider, true);
            state.newVideo("a", provider, true);
            state.ready(provider, "a");
            state.filterPlay(false, false); // Pause, then background the app.
            state.initialize(provider, true);
            assertFalse(state.matchesVideo("a"));
            assertFalse(state.newVideo("a", provider, true));
            assertFalse(state.isWaiting(provider, "a"));
            assertFalse(state.takeDeferredResume());
            assertTrue(state.filterPlay(true, false)); // The play button works again.
        }
    }

    @Test
    public void pendingRequestSurvivesPlayerRecreationWithoutBeingRestarted() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.select("a", YANDEX, true, false);
        state.initialize(YANDEX, true);
        assertFalse(state.newVideo("a", YANDEX, true));
        assertTrue(state.isWaiting(YANDEX, "a"));
        assertTrue(state.ready(YANDEX, "a"));
    }

    @Test
    public void manualPauseDuringTranslationSurvivesPlayerRecreation() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, false);
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.ready(GOOGLE, "a"));
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void completionDuringRecreationWaitsForTheActualVideoId() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, true);
        assertFalse(state.ready(YANDEX, "a"));
        assertFalse(state.takeDeferredResume());
        assertFalse(state.filterPlay(true, false));
        assertFalse(state.newVideo("a", YANDEX, true));
        assertTrue(state.takeDeferredResume());
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void completionForOldVideoCannotResumeADifferentReplacementVideo() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, true);
        state.ready(YANDEX, "a");
        state.newVideo("b", YANDEX, true);
        assertFalse(state.takeDeferredResume());
        assertFalse(state.filterPlay(true, false));
        assertTrue(state.ready(YANDEX, "b"));
    }

    @Test
    public void playDuringRecreationIsReplayedOnlyForTheConfirmedReadyVideo() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.ready(GOOGLE, "a");
        state.initialize(GOOGLE, true);
        assertFalse(state.filterPlay(true, false));
        state.newVideo("a", GOOGLE, true);
        assertTrue(state.takeDeferredResume());
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void pauseDuringRecreationCancelsADeferredResume() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.initialize(GOOGLE, true);
        state.ready(GOOGLE, "a");
        state.filterPlay(false, false);
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void internalPauseDuringRecreationDoesNotDiscardAPlayRequest() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.ready(GOOGLE, "a");
        state.initialize(GOOGLE, true);
        state.filterPlay(true, false);
        state.filterPlay(false, true);
        state.newVideo("a", GOOGLE, true);
        assertTrue(state.takeDeferredResume());
    }

    @Test
    public void canceledRequestStaysReleasedAfterRecreation() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.cancel(YANDEX, "a");
        state.initialize(YANDEX, true);
        assertFalse(state.newVideo("a", YANDEX, true));
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void failureDuringRecreationDoesNotLeaveAnUnreleaseableHold() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, true);
        state.cancel(YANDEX, "a");
        state.newVideo("a", YANDEX, true);
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void unidentifiedPlayerDoesNotReleaseTheFirstFrameOfANewVideo() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.ready(YANDEX, "a");
        state.initialize(YANDEX, true);
        assertFalse(state.filterPlay(true, false));
        assertFalse(state.matchesVideo("a"));
        assertTrue(state.newVideo("b", YANDEX, true));
        assertFalse(state.takeDeferredResume());
        assertFalse(state.filterPlay(true, false));
        assertTrue(state.ready(YANDEX, "b"));
    }

    @Test
    public void noProviderNeverHoldsEvenIfPauseIsRequested() {
        state.initialize(NONE, true);
        assertEquals(NONE, state.provider());
        assertFalse(state.isWaiting());
        assertTrue(state.filterPlay(true, false));
        assertFalse(state.filterPlay(false, false));
        state.newVideo("a", NONE, true);
        assertFalse(state.isWaiting());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void disabledPausePassesBothRequestsBeforeMetadata() {
        state.initialize(GOOGLE, false);
        assertTrue(state.filterPlay(true, false));
        assertFalse(state.filterPlay(false, false));
        assertFalse(state.isWaiting());
    }

    @Test
    public void emptyMetadataIsIgnoredWithoutLosingOwnership() {
        assertFalse(state.matchesVideo(""));
        state.initialize(YANDEX, true);
        assertFalse(state.newVideo("", YANDEX, true));
        assertEquals("", state.videoId());
        state.newVideo("a", YANDEX, true);
        assertFalse(state.newVideo("", GOOGLE, true));
        assertEquals("a", state.videoId());
        assertEquals(YANDEX, state.provider());
    }

    @Test
    public void playWhileRebindingAnUnfinishedRequestOverridesEarlierPause() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        assertFalse(state.filterPlay(false, false));
        state.initialize(YANDEX, true);
        assertFalse(state.filterPlay(true, false));
        state.newVideo("a", YANDEX, true);
        assertTrue(state.ready(YANDEX, "a"));
    }

    @Test
    public void pauseWhileRebindingAnUnfinishedRequestCancelsResume() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.initialize(GOOGLE, true);
        assertFalse(state.filterPlay(false, false));
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.ready(GOOGLE, "a"));
    }

    @Test
    public void duplicateMetadataDoesNotReplayAnOldPlayRequest() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.ready(GOOGLE, "a");
        state.initialize(GOOGLE, true);
        state.filterPlay(true, false);
        state.newVideo("a", GOOGLE, true);
        assertTrue(state.takeDeferredResume());
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.takeDeferredResume());
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void aNewVideoDiscardsDeferredResumeEvenWithPauseDisabled() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, true);
        state.ready(YANDEX, "a");
        state.newVideo("b", YANDEX, false);
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void canceledDeferredCompletionCannotResumeAfterBinding() {
        state.initialize(YANDEX, true);
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, true);
        state.ready(YANDEX, "a");
        state.cancel(YANDEX, "a");
        state.newVideo("a", YANDEX, true);
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void selectingOnPlayingVideoResumesWhenReady() {
        state.initialize(GOOGLE, false);
        state.newVideo("a", GOOGLE, false);
        state.select("a", GOOGLE, true, true);
        assertFalse(state.filterPlay(false, true));
        assertTrue(state.ready(GOOGLE, "a"));
    }

    @Test
    public void selectingDifferentVideoDoesNotInheritItsPredecessorsPause() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, false);
        state.select("b", YANDEX, true, true);
        assertEquals("b", state.videoId());
        assertTrue(state.ready(YANDEX, "b"));
    }

    @Test
    public void selectingWithoutPauseReleasesAnExistingHold() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.select("a", YANDEX, false, true);
        assertFalse(state.isWaiting());
        assertFalse(state.ready(YANDEX, "a"));
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void selectingWithoutPauseClearsADeferredCompletion() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.initialize(GOOGLE, true);
        state.ready(GOOGLE, "a");
        state.select("a", YANDEX, false, false);
        state.newVideo("a", YANDEX, false);
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void unidentifiedPlayerIsHeldWhileTheRetainedVideoIsAlreadyReady() {
        state.initialize(GOOGLE, true);
        state.newVideo("a", GOOGLE, true);
        state.ready(GOOGLE, "a");
        assertFalse(state.isWaiting());
        state.initialize(GOOGLE, true);
        assertTrue(state.isWaiting());
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.isWaiting());
    }

    @Test
    public void nextVideoDoesNotInheritThePreviousConstructorsPauseRequest() {
        state.initialize(GOOGLE, true);
        state.filterPlay(false, false);
        state.newVideo("a", GOOGLE, true);
        assertFalse(state.ready(GOOGLE, "a"));
        state.newVideo("b", GOOGLE, true);
        assertTrue(state.ready(GOOGLE, "b"));
    }

    @Test
    public void newManualVideoDoesNotReplayOldReadyCallback() {
        state.newVideo("a", YANDEX, true);
        state.initialize(YANDEX, false);
        assertFalse(state.ready(YANDEX, "a"));
        state.newVideo("b", YANDEX, false);
        assertFalse(state.takeDeferredResume());
        assertTrue(state.filterPlay(true, false));
    }

    @Test
    public void differentManualVideoResumesOnlyItsSuppressedPlayRequest() {
        state.newVideo("a", YANDEX, false);
        state.select("a", YANDEX, true, true);
        state.initialize(YANDEX, false);
        assertFalse(state.filterPlay(true, false));
        state.newVideo("b", YANDEX, false);
        assertTrue(state.takeDeferredResume());
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void selectingAnotherVideoDoesNotInheritPreviousResumeIntent() {
        state.newVideo("a", YANDEX, true);
        state.select("b", GOOGLE, true, false);
        assertFalse(state.ready(GOOGLE, "b"));
    }


    @Test
    public void newVideoWithoutRecreationDoesNotReuseOldSuppressedPlay() {
        state.initialize(YANDEX, true);
        state.filterPlay(true, false);
        state.newVideo("a", YANDEX, true);
        state.ready(YANDEX, "a");
        state.newVideo("b", YANDEX, false);
        assertFalse(state.takeDeferredResume());
    }

    @Test
    public void newAutomaticHoldConsumesPendingPlayWithoutLeavingASecondResume() {
        state.initialize(GOOGLE, true);
        state.filterPlay(true, false);
        state.newVideo("a", GOOGLE, true);
        state.filterPlay(false, false);
        assertFalse(state.ready(GOOGLE, "a"));
        assertFalse(state.takeDeferredResume());
    }

}
