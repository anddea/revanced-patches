package app.morphe.patches.music.general.redirection

import app.morphe.util.containsLiteralInstruction
import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstruction
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val notificationLikeButtonControllerFingerprint = legacyFingerprint(
    name = "notificationLikeButtonControllerFingerprint",
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("com/google/android/apps/youtube/music/player/notification/NotificationLikeButtonController"),
    customFingerprint = { method, _ ->
        method.name == "<clinit>"
    }
)

internal val notificationLikeButtonOnClickListenerFingerprint = legacyFingerprint(
    name = "notificationLikeButtonOnClickListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L"),
    customFingerprint = { method, _ ->
        indexOfMapInstruction(method) >= 0
    }
)

internal fun indexOfMapInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.parameterTypes?.size == 3 &&
                reference.parameterTypes[2].toString() == "Ljava/util/Map;"
    }

internal val dislikeButtonOnClickListenerFingerprint = legacyFingerprint(
    name = "dislikeButtonOnClickListenerFingerprint",
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
 * 6.20 - 7.25
 */
internal val dislikeButtonOnClickListenerLegacyFingerprint = legacyFingerprint(
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
