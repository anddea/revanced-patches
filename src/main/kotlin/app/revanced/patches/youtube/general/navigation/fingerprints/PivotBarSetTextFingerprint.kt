package app.revanced.patches.youtube.general.navigation.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object PivotBarSetTextFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = listOf("Lcom/google/android/libraries/youtube/rendering/ui/pivotbar/PivotBar;", "Landroid/widget/TextView;", "Ljava/lang/CharSequence;"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { methodDef, _ -> methodDef.name == "<init>" }
)