package app.morphe.patches.youtube.utils.sponsorblock

import app.morphe.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversed
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val rectangleFieldInvalidatorFingerprint = legacyFingerprint(
    name = "rectangleFieldInvalidatorFingerprint",
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        indexOfInvalidateInstruction(method) >= 0
    }
)

internal val segmentPlaybackControllerFingerprint = legacyFingerprint(
    name = "segmentPlaybackControllerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(Opcode.CONST_STRING),
    customFingerprint = { method, _ ->
        method.definingClass == "$EXTENSION_PATH/sponsorblock/SegmentPlaybackController;"
                && method.name == "setSponsorBarRect"
    }
)

internal fun indexOfInvalidateInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        getReference<MethodReference>()?.name == "invalidate"
    }
