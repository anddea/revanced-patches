package app.revanced.patches.youtube.video.videoid

import app.revanced.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val videoIdFingerprint = legacyFingerprint(
    name = "videoIdFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = custom@{ method, classDef ->
        if (!classDef.fields.any { it.type == "Lcom/google/android/libraries/youtube/player/subtitles/model/SubtitleTrack;" }) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false
        val instructions = implementation.instructions
        val instructionCount = instructions.count()
        if (instructionCount < 30) {
            return@custom false
        }

        val reference =
            (instructions.elementAt(instructionCount - 2) as? ReferenceInstruction)?.reference.toString()
        if (reference != "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;") {
            return@custom false
        }

        method.indexOfFirstInstruction {
            val methodReference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_INTERFACE &&
                    methodReference?.returnType == "Ljava/lang/String;" &&
                    methodReference.parameterTypes.isEmpty() &&
                    methodReference.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
        } >= 0
    },
)
