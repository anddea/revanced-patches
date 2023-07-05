package app.revanced.patches.shared.fingerprints.captions

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object SubtitleTrackFingerprint : MethodFingerprint(
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("DISABLE_CAPTIONS_OPTION"),
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/SubtitleTrack;")
    }
)