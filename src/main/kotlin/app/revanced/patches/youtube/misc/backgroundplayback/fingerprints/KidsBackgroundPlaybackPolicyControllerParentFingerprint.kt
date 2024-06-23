package app.revanced.patches.youtube.misc.backgroundplayback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

internal object KidsBackgroundPlaybackPolicyControllerParentFingerprint : MethodFingerprint(
    returnType = "L",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("Lcom/google/android/libraries/youtube/innertube/model/player/PlayerResponseModel;"),
    customFingerprint = { methodDef, _ ->
        methodDef.indexOfFirstInstruction {
            opcode == Opcode.SGET_OBJECT
                    && getReference<FieldReference>()?.name == "miniplayerRenderer"
        } >= 0
    }
)
