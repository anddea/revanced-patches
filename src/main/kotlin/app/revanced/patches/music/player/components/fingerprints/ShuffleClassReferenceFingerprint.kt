package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.player.components.fingerprints.ShuffleClassReferenceFingerprint.indexOfImageViewInstruction
import app.revanced.patches.music.player.components.fingerprints.ShuffleClassReferenceFingerprint.indexOfOrdinalInstruction
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.YtFillArrowShuffle
import app.revanced.util.containsWideLiteralInstructionValue
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ShuffleClassReferenceFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("Unknown shuffle mode"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(YtFillArrowShuffle) &&
                indexOfOrdinalInstruction(methodDef) >= 0 &&
                indexOfImageViewInstruction(methodDef) >= 0
    }
) {
    fun indexOfOrdinalInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.name == "ordinal"
        }

    fun indexOfImageViewInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.IGET_OBJECT &&
                    getReference<FieldReference>()?.type == "Landroid/widget/ImageView;"
        }
}

