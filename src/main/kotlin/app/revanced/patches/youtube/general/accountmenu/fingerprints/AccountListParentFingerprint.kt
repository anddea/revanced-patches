package app.revanced.patches.youtube.general.accountmenu.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.CompactListItem
import app.revanced.util.bytecode.isWideLiteralExists

object AccountListParentFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(CompactListItem) }
)