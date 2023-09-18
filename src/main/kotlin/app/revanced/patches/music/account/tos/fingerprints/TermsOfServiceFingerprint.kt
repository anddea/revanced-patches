package app.revanced.patches.music.account.tos.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.TosFooter
import app.revanced.util.bytecode.isWideLiteralExists

object TermsOfServiceFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(TosFooter) }
)
