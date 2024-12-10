package app.revanced.patches.reddit.misc.tracking.url

import app.revanced.util.fingerprint.legacyFingerprint

internal val shareLinkFormatterFingerprint = legacyFingerprint(
    name = "shareLinkFormatterFingerprint",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/Map;"),
    customFingerprint = { method, classDef ->
        method.definingClass.startsWith("Lcom/reddit/sharing/") &&
                classDef.sourceFile == "UrlUtil.kt"
    }
)
