package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object OnPlaybackSpeedItemClickFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("Landroid/widget/AdapterView;", "Landroid/view/View;", "I", "J"),
    customFingerprint = { methodDef, _ ->
        methodDef.name == "onItemClick" &&
                methodDef.indexOfFirstInstruction {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.type == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
                } >= 0
    }
)