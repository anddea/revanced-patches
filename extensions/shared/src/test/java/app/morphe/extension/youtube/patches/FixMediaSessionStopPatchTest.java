package app.morphe.extension.youtube.patches;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Verifies ownership isolation and cleanup for players sharing one media session. */
public class FixMediaSessionStopPatchTest {
    @Before
    public void resetOwner() {
        FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed();
        Object player = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(player);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(player);
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(true));
    }

    @Test
    public void nonPlayerStopIsAllowed() {
        FixMediaSessionStopPatch.setMediaSessionOwner(new Object());
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
    }

    @Test
    public void ownerCanStopAndSecondaryPlayerCannot() {
        Object owner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(owner);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(owner);
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(new Object());
        assertFalse(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
        // The caller marker is consumed even when a stop was refused.
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
    }

    @Test
    public void destroyedOwnerReleasesOwnership() {
        Object owner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(owner);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(owner);
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(true));
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(new Object());
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
    }

    @Test
    public void destroyedSecondaryPlayerDoesNotReleaseOwner() {
        Object owner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(owner);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(new Object());
        assertFalse(FixMediaSessionStopPatch.isMediaSessionStopAllowed(true));
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(new Object());
        assertFalse(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
    }

    @Test
    public void newOwnerSupersedesPreviousOwner() {
        Object oldOwner = new Object();
        Object newOwner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(oldOwner);
        FixMediaSessionStopPatch.setMediaSessionOwner(newOwner);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(oldOwner);
        assertFalse(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(newOwner);
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
    }
    @Test
    public void backgroundStopRejectsSecondaryPlayerAndConsumesCaller() {
        Object owner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(owner);
        FixMediaSessionStopPatch.setBackgroundPlaybackStoppingPlayer(new Object());
        assertFalse(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
        assertTrue(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
        FixMediaSessionStopPatch.setBackgroundPlaybackStoppingPlayer(owner);
        assertTrue(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
    }

    @Test
    public void backgroundStopWithoutKnownOwnerIsAllowed() {
        FixMediaSessionStopPatch.setBackgroundPlaybackStoppingPlayer(new Object());
        assertTrue(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
    }

    @Test
    public void sessionAndBackgroundStopCallersAreIndependent() {
        Object owner = new Object();
        FixMediaSessionStopPatch.setMediaSessionOwner(owner);
        FixMediaSessionStopPatch.setMediaSessionStoppingPlayer(owner);
        FixMediaSessionStopPatch.setBackgroundPlaybackStoppingPlayer(new Object());
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(false));
        assertFalse(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
        // A stop without a player must not discard the known session owner.
        assertTrue(FixMediaSessionStopPatch.isMediaSessionStopAllowed(true));
        FixMediaSessionStopPatch.setBackgroundPlaybackStoppingPlayer(new Object());
        assertFalse(FixMediaSessionStopPatch.isBackgroundPlaybackStopAllowed());
    }
}
