package app.revanced.patches.music.account.tos.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TosFooter
import app.revanced.util.bytecode.isWideLiteralExists

object TermsOfServiceFingerprint : MethodFingerprint(
    returnType = "Landroid/view/View;",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(TosFooter) }
)
