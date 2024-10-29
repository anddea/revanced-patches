package app.revanced.patches.music.flyoutmenu.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ScreenWidthParentFingerprint : MethodFingerprint(
    returnType = "Landroid/graphics/Bitmap;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Landroid/app/Activity;", "I"),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstructionReversed {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "destroyDrawingCache"
        } >= 0
    }
)

