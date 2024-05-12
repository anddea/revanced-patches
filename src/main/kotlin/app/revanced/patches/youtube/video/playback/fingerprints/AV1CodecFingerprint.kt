package app.revanced.patches.youtube.video.playback.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex
import com.android.tools.smali.dexlib2.AccessFlags

internal object AV1CodecFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    returnType = "L",
    strings = listOf("AtomParsers", "video/av01"),
    customFingerprint = handler@{ methodDef, _ ->
        if (methodDef.returnType == "Ljava/util/List;")
            return@handler false

        methodDef.containsWideLiteralInstructionIndex(1987076931)
    }
)
