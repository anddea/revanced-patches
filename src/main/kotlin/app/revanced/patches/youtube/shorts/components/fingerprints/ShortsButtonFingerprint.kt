package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynRemix
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelDynShare
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightDislikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ReelRightLikeIcon
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.RightComment
import app.revanced.util.containsWideLiteralInstructionValue

internal object ShortsButtonFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ ->
        methodDef.containsWideLiteralInstructionValue(ReelDynRemix)
                && methodDef.containsWideLiteralInstructionValue(ReelDynShare)
                && methodDef.containsWideLiteralInstructionValue(ReelRightDislikeIcon)
                && methodDef.containsWideLiteralInstructionValue(ReelRightLikeIcon)
                && methodDef.containsWideLiteralInstructionValue(RightComment)
    },
)