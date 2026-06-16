package app.morphe.patches.youtube.video.videoid

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
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
        if (instructionCount < 25) {
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

internal val videoIdBackgroundPlayFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.DECLARED_SYNCHRONIZED, AccessFlags.FINAL, AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT),
        opcode(Opcode.IPUT_OBJECT),
        opcode(Opcode.MONITOR_EXIT),
        opcode(Opcode.RETURN_VOID),
        opcode(Opcode.MONITOR_EXIT),
        opcode(Opcode.RETURN_VOID)
    ),
    custom = { method, classDef ->
        method.implementation != null &&
                (classDef.methods.count() == 17 || classDef.methods.count() == 16)
    }
)
