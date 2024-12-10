package app.revanced.patches.reddit.layout.communities

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val communityRecommendationSectionFingerprint = legacyFingerprint(
    name = "communityRecommendationSectionFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/CommunityRecommendationSection;")
    }
)
