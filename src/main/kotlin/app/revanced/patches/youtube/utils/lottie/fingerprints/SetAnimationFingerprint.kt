package app.revanced.patches.youtube.utils.lottie.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.lottie.fingerprints.SetAnimationFingerprint.LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object SetAnimationFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("I"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.NEW_INSTANCE,
        Opcode.NEW_INSTANCE,
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
    }
) {
    const val LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR =
        "Lcom/airbnb/lottie/LottieAnimationView;"
}