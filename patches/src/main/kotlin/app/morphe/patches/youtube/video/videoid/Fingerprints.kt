package app.morphe.patches.youtube.video.videoid

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patches.youtube.utils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object LegacyVideoIdFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        opcode(Opcode.INVOKE_INTERFACE),
        opcode(Opcode.MOVE_RESULT_OBJECT),
        opcode(Opcode.INVOKE_INTERFACE),
        opcode(Opcode.MOVE_RESULT_OBJECT),
    ),
    custom = custom@{ method, classDef ->
        if (!classDef.fields.any {
                it.type == "Lcom/google/android/libraries/youtube/player/subtitles/model/SubtitleTrack;"
            }
        ) {
            return@custom false
        }

        val instructions = method.implementation?.instructions ?: return@custom false
        if (instructions.count() < 25) return@custom false

        val reference = (instructions.elementAt(instructions.count() - 2) as? ReferenceInstruction)
            ?.reference
            .toString()
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

private object VideoIdParentFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "[L",
    parameters = listOf("L"),
    filters = listOf(
        literal(524288L)
    )
)

internal object VideoIdFingerprint : Fingerprint(
    classFingerprint = VideoIdParentFingerprint,
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_INTERFACE, returnType = "Ljava/lang/String;"),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()), // videoId
        methodCall(
            smali = "Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            location = MatchAfterWithin(6)
        ),
        opcode(Opcode.RETURN_VOID, location = MatchAfterImmediately())
    )
)

internal object VideoIdBackgroundPlayFingerprint : Fingerprint(
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
