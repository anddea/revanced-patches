package app.revanced.patches.music.account.component.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.patch.SharedResourceIdPatch.Companion.MenuEntry
import app.revanced.util.bytecode.isWideLiteralExists

object MenuEntryFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(MenuEntry) }
)
