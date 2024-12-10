package app.revanced.patches.youtube.utils.lottie

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
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