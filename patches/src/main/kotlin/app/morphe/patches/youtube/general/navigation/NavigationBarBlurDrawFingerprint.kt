/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.navigation

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

/**
 * Draws the blurred background of the translucent navigation bar. If the canvas is hardware
 * accelerated, a copy of the watch layout, with the player, is drawn into a blurred render node,
 * then the bar color is drawn over it.
 */
internal object NavigationBarBlurDrawFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        filters = listOf(
            string("frostedGlassRenderNode")
        )
    ),
    name = "draw",
    returnType = "V",
    parameters = listOf("Landroid/graphics/Canvas;"),
    filters = listOf(
        methodCall(smali = "Landroid/graphics/Canvas;->isHardwareAccelerated()Z"),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(smali = "Landroid/view/View;->draw(Landroid/graphics/Canvas;)V")
    )
)

