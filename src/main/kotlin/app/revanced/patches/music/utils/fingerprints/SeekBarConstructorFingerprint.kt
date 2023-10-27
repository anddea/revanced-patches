package app.revanced.patches.music.utils.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InlineTimeBarAdBreakMarkerColor
import app.revanced.util.bytecode.isWideLiteralExists

object SeekBarConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            InlineTimeBarAdBreakMarkerColor
        )
    }
)