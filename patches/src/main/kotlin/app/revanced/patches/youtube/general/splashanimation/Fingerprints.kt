package app.revanced.patches.youtube.general.splashanimation

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val splashAnimationFingerprint = legacyFingerprint(
    name = "splashAnimationFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    strings = listOf("PostCreateCalledKey"),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("Activity;") &&
                method.name == "onCreate" &&
                indexOfStartAnimatedVectorDrawableInstruction(method) >= 0
    }
)

fun indexOfStartAnimatedVectorDrawableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.toString() == "Landroid/graphics/drawable/AnimatedVectorDrawable;->start()V"
    }

internal val startUpResourceIdFingerprint = legacyFingerprint(
    name = "startUpResourceIdFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I"),
    literals = listOf(3L, 4L)
)

internal val startUpResourceIdParentFingerprint = legacyFingerprint(
    name = "startUpResourceIdParentFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.DECLARED_SYNCHRONIZED,
    parameters = listOf("I", "I"),
    strings = listOf("early type", "final type")
)
