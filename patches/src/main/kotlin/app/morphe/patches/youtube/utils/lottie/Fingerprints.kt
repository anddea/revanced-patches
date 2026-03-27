package app.morphe.patches.youtube.utils.lottie

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR =
    "Lcom/airbnb/lottie/LottieAnimationView;"

internal val setAnimationFingerprint = legacyFingerprint(
    name = "setAnimationFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.NEW_INSTANCE,
        Opcode.NEW_INSTANCE,
    ),
    customFingerprint = { method, _ ->
        method.definingClass == LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
    }
)