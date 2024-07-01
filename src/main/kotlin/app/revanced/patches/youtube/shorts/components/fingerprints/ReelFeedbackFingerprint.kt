package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackLike
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPause
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelFeedbackPlay
import app.revanced.util.containsWideLiteralInstructionIndex

internal object ReelFeedbackFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionIndex(ReelFeedbackLike)
                && methodDef.containsWideLiteralInstructionIndex(ReelFeedbackPause)
                && methodDef.containsWideLiteralInstructionIndex(ReelFeedbackPlay)
    },
)