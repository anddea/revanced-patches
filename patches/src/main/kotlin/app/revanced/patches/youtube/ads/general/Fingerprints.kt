package app.revanced.patches.youtube.ads.general

import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementAdContainer
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val compactYpcOfferModuleViewFingerprint = legacyFingerprint(
    name = "compactYpcOfferModuleViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("I", "I"),
    opcodes = listOf(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/CompactYpcOfferModuleView;") &&
                method.name == "onMeasure"
    }
)

internal val fullScreenEngagementAdContainerFingerprint = legacyFingerprint(
    name = "fullScreenEngagementAdContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(fullScreenEngagementAdContainer),
    customFingerprint = { method, _ ->
        indexOfAddListInstruction(method) >= 0
    }
)

internal fun indexOfAddListInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "add"
    }