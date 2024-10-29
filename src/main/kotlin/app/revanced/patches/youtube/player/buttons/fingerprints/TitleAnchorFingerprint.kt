package app.revanced.patches.youtube.player.buttons.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.PlayerCollapseButton
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.TitleAnchor
import app.revanced.util.containsWideLiteralInstructionValue

internal object TitleAnchorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(PlayerCollapseButton)
                && methodDef.containsWideLiteralInstructionValue(TitleAnchor)
    }
)