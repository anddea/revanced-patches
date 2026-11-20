/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.video.playback

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

/**
 * Plays a Short faster while the screen is held, at 2.0x, unless it already plays at 2.0x,
 * and shows "2x" above the player. Logs the gesture with its own id. 21.11+
 */
internal object SpeedmasterStartFingerprint : Fingerprint(
    filters = listOf(
        literal(307700L),
        literal(2.0f),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("F"),
            returnType = "V"
        )
    )
)

