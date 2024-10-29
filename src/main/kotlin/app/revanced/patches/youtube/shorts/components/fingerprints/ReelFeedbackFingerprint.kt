package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackLike
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPause
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPlay
import app.revanced.util.containsWideLiteralInstructionValue

internal object ReelFeedbackFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(ReelFeedbackLike)
                && methodDef.containsWideLiteralInstructionValue(ReelFeedbackPause)
                && methodDef.containsWideLiteralInstructionValue(ReelFeedbackPlay)
    },
)