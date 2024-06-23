package app.revanced.patches.reddit.layout.communities.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object CommunityRecommendationSectionFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ ->
        methodDef.definingClass.endsWith("/CommunityRecommendationSection;")
    }
)