/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to Morphe contributions.
 */

@file:Suppress("SpellCheckingInspection")

package app.morphe.patches.youtube.player.miniplayer.general

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.checkCast
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object MiniplayerOffscreenHandlerFingerprint : Fingerprint(
    classFingerprint = MiniplayerRectDragFieldsNameFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("I", "I", "I", "I"),
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Landroid/graphics/Rect;"),
        methodCall(opcode = Opcode.INVOKE_VIRTUAL, smali = "Landroid/graphics/Rect;->set(IIII)V")
    )
)

internal object MiniplayerLegacyControlsFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "controls_layout"),
        checkCast("Landroid/view/ViewGroup;", location = MatchAfterWithin(5))
    )
)

internal object MiniplayerLegacyControlsVisibilityFingerprint : Fingerprint(
    classFingerprint = MiniplayerLegacyControlsFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = $$"Landroid/view/ViewGroup;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;"
        ),
        literal(8),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            smali = "Landroid/view/ViewGroup;->setVisibility(I)V",
            location = MatchAfterImmediately()
        )
    )
)
