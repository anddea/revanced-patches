package app.revanced.patches.music.player.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR

internal object PlayerPatchConstructorFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass == PLAYER_CLASS_DESCRIPTOR
                && methodDef.name == "<init>"
    }
)