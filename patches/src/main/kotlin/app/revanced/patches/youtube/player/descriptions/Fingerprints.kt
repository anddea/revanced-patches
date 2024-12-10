package app.revanced.patches.youtube.player.descriptions

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val engagementPanelTitleFingerprint = legacyFingerprint(
    name = "engagementPanelTitleFingerprint",
    strings = listOf(". "),
    customFingerprint = { method, _ ->
        indexOfContentDescriptionInstruction(method) >= 0
    }
)

internal fun indexOfContentDescriptionInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "setContentDescription"
    }

internal val engagementPanelTitleParentFingerprint = legacyFingerprint(
    name = "engagementPanelTitleParentFingerprint",
    strings = listOf("[EngagementPanelTitleHeader] Cannot remove action buttons from header as the child count is out of sync. Buttons to remove exceed current header child count.")
)

/**
 * This fingerprint is compatible with YouTube v18.35.xx~
 * Nonetheless, the patch works in YouTube v19.02.xx~
 */
internal val textViewComponentFingerprint = legacyFingerprint(
    name = "textViewComponentFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.CMPL_FLOAT),
    customFingerprint = { method, _ ->
        method.implementation != null &&
                indexOfTextIsSelectableInstruction(method) >= 0
    },
)

internal fun indexOfTextIsSelectableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.name == "setTextIsSelectable" &&
                reference.definingClass != "Landroid/widget/TextView;"
    }