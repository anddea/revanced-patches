/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches/pull/2640
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.general.toolbar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.shared.mapping.ResourceType
import app.morphe.patches.shared.mapping.resourceLiteral
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object ToolBarButtonFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    filters = listOf(
        resourceLiteral(ResourceType.ID, "menu_item_view"),
        methodCall(smali = "Landroid/view/MenuItem;->setShowAsAction(I)V"),
        methodCall(
            opcode = Opcode.INVOKE_STATIC,
            returnType = "L",
            parameters = listOf("I")
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            returnType = "I",
            parameters = listOf("L")
        ),
        opcode(Opcode.MOVE_RESULT, MatchAfterImmediately()),
        fieldAccess(
            opcode = Opcode.IGET_OBJECT,
            type = "Landroid/widget/ImageView;",
            location = MatchAfterWithin(6)
        ),
        methodCall(
            definingClass = "Landroid/content/res/Resources;",
            name = "getDrawable",
            location = MatchAfterWithin(8)
        ),
        methodCall(
            definingClass = "Landroid/widget/ImageView;",
            name = "setImageDrawable",
            location = MatchAfterWithin(4)
        )
    )
)

