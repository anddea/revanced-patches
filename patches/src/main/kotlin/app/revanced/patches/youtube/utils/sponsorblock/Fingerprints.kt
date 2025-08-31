package app.revanced.patches.youtube.utils.sponsorblock

import app.revanced.patches.youtube.utils.extension.Constants.EXTENSION_PATH
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
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

internal val adProgressTextViewVisibilityFingerprint = legacyFingerprint(
    name = "adProgressTextViewVisibilityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    customFingerprint = { method, _ ->
        indexOfAdProgressTextViewVisibilityInstruction(method) >= 0
    }
)

internal fun indexOfAdProgressTextViewVisibilityInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() ==
                "Lcom/google/android/libraries/youtube/ads/player/ui/AdProgressTextView;->setVisibility(I)V"
    }
