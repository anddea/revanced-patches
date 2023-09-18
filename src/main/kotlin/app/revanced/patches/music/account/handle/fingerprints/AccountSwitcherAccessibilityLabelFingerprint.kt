package app.revanced.patches.music.account.handle.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.AccountSwitcherAccessibility
import app.revanced.util.bytecode.isWideLiteralExists

object AccountSwitcherAccessibilityLabelFingerprint : MethodFingerprint(
    returnType = "V",
    parameters = listOf("L", "Ljava/lang/Object;"),
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(AccountSwitcherAccessibility) }
)