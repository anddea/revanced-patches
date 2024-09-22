package app.revanced.patches.youtube.video.information.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.PlayerResponseModelUtils.PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.video.information.fingerprints.PlaybackInitializationFingerprint.indexOfPlayerResponseModelInstruction
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object PlaybackInitializationFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("play() called when the player wasn\'t loaded."),
    customFingerprint = { methodDef, _ ->
        indexOfPlayerResponseModelInstruction(methodDef) >= 0
    }
) {
    fun indexOfPlayerResponseModelInstruction(methodDef: Method) =
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_DIRECT &&
                    getReference<MethodReference>()?.returnType == PLAYER_RESPONSE_MODEL_CLASS_DESCRIPTOR
        }
}
