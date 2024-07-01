package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.DarkBackground
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TapBloomView
import app.revanced.util.containsWideLiteralInstructionIndex

internal object QuickSeekOverlayFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionIndex(DarkBackground)
                && methodDef.containsWideLiteralInstructionIndex(TapBloomView)
    },
)