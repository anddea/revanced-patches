package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.DarkBackground
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TapBloomView
import app.revanced.util.containsWideLiteralInstructionValue

internal object QuickSeekOverlayFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(DarkBackground)
                && methodDef.containsWideLiteralInstructionValue(TapBloomView)
    },
)