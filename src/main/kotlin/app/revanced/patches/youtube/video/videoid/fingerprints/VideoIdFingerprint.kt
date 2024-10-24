package app.revanced.patches.youtube.video.videoid.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object VideoIdFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT
    ),
    customFingerprint = custom@{ methodDef, classDef ->
        if (!classDef.fields.any { it.type == "Lcom/google/android/libraries/youtube/player/subtitles/model/SubtitleTrack;" }) {
            return@custom false
        }
        val implementation = methodDef.implementation
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

        methodDef.indexOfFirstInstruction {
            val methodReference = getReference<MethodReference>()
            opcode == Opcode.INVOKE_INTERFACE &&
                    methodReference?.returnType == "Ljava/lang/String;" &&
                    methodReference.parameterTypes.isEmpty() &&
                    methodReference.definingClass == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
        } >= 0
    },
)