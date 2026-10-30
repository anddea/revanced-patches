/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/3899
 * https://github.com/MorpheApp/morphe-patches/pull/4268
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.patches.music.utils.gms

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.returnEarly

/**
 * Accounts without Premium cannot cast songs to audio only devices such as speakers and
 * Chromecast Audio. The app shows a Premium dialog instead of connecting, removes songs from the
 * queue and disconnects the session. All of these use the same account check, which is forced on.
 */
internal fun BytecodePatchContext.allowCastingToAudioOnlyDevices() {
    AudioCastAccountStateChangedFingerprint.instructionMatches.first()
        .getMethodCalled().returnEarly(true)
}

