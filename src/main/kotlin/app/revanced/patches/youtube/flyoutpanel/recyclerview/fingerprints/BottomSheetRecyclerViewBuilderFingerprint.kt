package app.revanced.patches.youtube.flyoutpanel.recyclerview.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object BottomSheetRecyclerViewBuilderFingerprint : MethodFingerprint(
    customFingerprint = { methodDef, _ -> methodDef.isWide32LiteralExists(45382015) }
)