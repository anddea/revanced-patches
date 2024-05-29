package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.containsWideLiteralInstructionIndex

internal object MusicActivityWidgetFingerprint : MethodFingerprint(
    customFingerprint = handler@{ methodDef, _ ->
        if (!methodDef.definingClass.endsWith("/MusicActivity;"))
            return@handler false

        methodDef.containsWideLiteralInstructionIndex(79500)
    }
)
