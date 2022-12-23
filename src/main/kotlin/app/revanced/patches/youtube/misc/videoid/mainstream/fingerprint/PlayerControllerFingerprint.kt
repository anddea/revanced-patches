package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerControllerFingerprint : MethodFingerprint(
    customFingerprint = { methodDef ->
        methodDef.definingClass == "Lapp/revanced/integrations/sponsorblock/PlayerController;" && methodDef.name == "setSponsorBarRect"
    }
)
