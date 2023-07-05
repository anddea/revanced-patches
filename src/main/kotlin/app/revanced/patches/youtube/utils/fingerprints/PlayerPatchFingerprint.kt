package app.revanced.patches.youtube.utils.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerPatchFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.definingClass == "Lapp/revanced/integrations/patches/layout/PlayerPatch;" && methodDef.name == "hidePlayerButton" }
)