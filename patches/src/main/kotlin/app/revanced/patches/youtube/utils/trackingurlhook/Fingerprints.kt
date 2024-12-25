package app.revanced.patches.youtube.utils.trackingurlhook

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val trackingUrlModelFingerprint = legacyFingerprint(
    name = "trackingUrlModelFingerprint",
    returnType = "Landroid/net/Uri;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    customFingerprint = { method, _ ->
        method.definingClass == "Lcom/google/android/libraries/youtube/innertube/model/player/TrackingUrlModel;"
    }
)
