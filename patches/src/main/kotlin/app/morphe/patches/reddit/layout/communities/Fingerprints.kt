package app.morphe.patches.reddit.layout.communities

import app.morphe.util.fingerprint.legacyFingerprint
import app.morphe.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val communityRecommendationSectionFingerprint = legacyFingerprint(
    name = "communityRecommendationSectionFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("feedContext"),
)

internal val communityRecommendationSectionParentFingerprint = legacyFingerprint(
    name = "communityRecommendationSectionParentFingerprint",
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    strings = listOf("community_recomendation_section_"),
    customFingerprint = { method, _ ->
        method.definingClass.startsWith("Lcom/reddit/onboardingfeedscomponents/communityrecommendation/impl/") &&
                method.name == "key"
    }
)