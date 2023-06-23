package app.revanced.patches.reddit.ad.comments.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object HideCommentAdsFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Object;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("RedditCommentsPageAdRepository;") },
)