package app.revanced.patches.youtube.utils.trackingurlhook.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object TrackingUrlModelFingerprint : MethodFingerprint(
    returnType = "Landroid/net/Uri;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/player/TrackingUrlModel;"
    }
)