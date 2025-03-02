package app.revanced.patches.shared.captions

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val startVideoInformerFingerprint = legacyFingerprint(
    name = "startVideoInformerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.RETURN_VOID
    ),
    strings = listOf("pc"),
    customFingerprint = { method, _ ->
        method.implementation
            ?.instructions
            ?.withIndex()
            ?.filter { (_, instruction) ->
                instruction.opcode == Opcode.CONST_STRING
            }
            ?.map { (index, _) -> index }
            ?.size == 1
    }
)

internal val storyboardRendererDecoderRecommendedLevelFingerprint = legacyFingerprint(
    name = "storyboardRendererDecoderRecommendedLevelFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf("#-1#")
)

internal val subtitleTrackFingerprint = legacyFingerprint(
    name = "subtitleTrackFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("DISABLE_CAPTIONS_OPTION")
)
