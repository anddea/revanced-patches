package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ColorGrey
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.util.containsWideLiteralInstructionValue

internal object MiniPlayerConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    strings = listOf("sharedToggleMenuItemMutations"),
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(ColorGrey)
                && methodDef.containsWideLiteralInstructionValue(MiniPlayerPlayPauseReplayButton)
    }
)