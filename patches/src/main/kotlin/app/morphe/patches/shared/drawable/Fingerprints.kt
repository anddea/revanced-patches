package app.morphe.patches.shared.drawable

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

/** Matches the shape drawable color assignment independently of its surrounding branch opcode. */
internal object DrawableColorFingerprint : Fingerprint(
    returnType = "V",
    accessFlags = listOf(AccessFlags.PROTECTED, AccessFlags.FINAL),
    parameters = listOf("L"),
    filters = listOf(
        methodCall(smali = "Landroid/graphics/Paint;->setColor(I)V")
    ),
    custom = { method, classDef ->
        method.name == "onBoundsChange" &&
                classDef.superclass == "Landroid/graphics/drawable/Drawable;"
    }
)
