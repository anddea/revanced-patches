package app.revanced.patches.music.utils.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarAdBreakMarkerColor
import app.revanced.util.bytecode.isWideLiteralExists

object SeekBarConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.isWideLiteralExists(
            InlineTimeBarAdBreakMarkerColor
        )
    }
)