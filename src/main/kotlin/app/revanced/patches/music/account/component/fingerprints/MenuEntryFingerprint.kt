package app.revanced.patches.music.account.component.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MenuEntry
import app.revanced.util.bytecode.isWideLiteralExists

object MenuEntryFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(MenuEntry) }
)
