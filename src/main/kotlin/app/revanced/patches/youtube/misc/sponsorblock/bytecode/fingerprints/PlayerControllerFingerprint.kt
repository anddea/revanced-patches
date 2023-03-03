package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object PlayerControllerFingerprint : MethodFingerprint(
    customFingerprint = {
        it.definingClass == "Lapp/revanced/integrations/sponsorblock/PlayerController;"
                && it.name == "setSponsorBarRect"
    }
)
