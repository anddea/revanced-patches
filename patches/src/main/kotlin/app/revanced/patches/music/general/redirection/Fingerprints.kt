package app.revanced.patches.music.general.redirection

import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal val dislikeButtonOnClickListenerFingerprint = legacyFingerprint(
    name = "dislikeButtonOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    customFingerprint = { method, _ ->
        method.name == "onClick" &&
                (method.containsLiteralInstruction(53465L) || method.containsLiteralInstruction(
                    98173L
                ))
    }
)

/**
 * YouTube Music 7.27.52 ~
 * TODO: Make this fingerprint more concise
 */
internal val dislikeButtonOnClickListenerAlternativeFingerprint = legacyFingerprint(
    name = "dislikeButtonOnClickListenerAlternativeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "Ljava/util/Map;"),
    customFingerprint = custom@{ method, classDef ->
        if (classDef.fields.count() != 7) {
            return@custom false
        }
        if (classDef.methods.count() != 5) {
            return@custom false
        }
        val implementation = method.implementation
            ?: return@custom false
        val instructions = implementation.instructions
        val instructionCount = instructions.count()
        if (instructionCount < 50) {
            return@custom false
        }

        ((instructions.elementAt(0) as? ReferenceInstruction)?.reference as? FieldReference)?.name == "likeEndpoint"
    }
)

/**
 * Finds the dislike handler method in the main player UI (8.19+).
 */
internal val dislikeUiHandlerFingerprint = legacyFingerprint(
    name = "dislikeUiHandlerFingerprint",
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    customFingerprint = { method, _ ->
        val instructions = method.implementation?.instructions ?: return@legacyFingerprint false

        val hasLikeEndpointAccess = instructions.any { instruction ->
            instruction.opcode == Opcode.SGET_OBJECT &&
                    (instruction as? ReferenceInstruction)?.reference?.toString()?.contains(
                        "Lcom/google/protos/youtube/api/innertube/LikeEndpointOuterClass\$LikeEndpoint;->likeEndpoint"
                    ) == true
        }

        val hasStringEqualsCall = instructions.any { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                    (instruction as? ReferenceInstruction)?.reference?.toString() == "Ljava/lang/String;->equals(Ljava/lang/Object;)Z"
        }

        hasLikeEndpointAccess && hasStringEqualsCall
    }
)
