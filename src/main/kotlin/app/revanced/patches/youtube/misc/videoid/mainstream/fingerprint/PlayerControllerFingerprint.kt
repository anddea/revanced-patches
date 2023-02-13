package app.revanced.patches.youtube.misc.videoid.mainstream.fingerprint

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerControllerFingerprint : MethodFingerprint(
    customFingerprint = {
        it.definingClass == "Lapp/revanced/integrations/sponsorblock/PlayerController;"
                && it.name == "setSponsorBarRect"
    }
)
