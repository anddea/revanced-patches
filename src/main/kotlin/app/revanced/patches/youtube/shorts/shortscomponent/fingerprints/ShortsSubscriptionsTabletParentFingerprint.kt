package app.revanced.patches.youtube.shorts.shortscomponent.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.ReelPlayerFooter
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags

object ShortsSubscriptionsTabletParentFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(ReelPlayerFooter) }
)