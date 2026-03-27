package app.morphe.patches.shared.drawable

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val drawableColorFingerprint = legacyFingerprint(
    name = "drawableColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.IF_NEZ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL, // Paint.setColor: inject point
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, classDef ->
        method.name == "onBoundsChange" &&
                classDef.superclass == "Landroid/graphics/drawable/Drawable;"
    }
)

