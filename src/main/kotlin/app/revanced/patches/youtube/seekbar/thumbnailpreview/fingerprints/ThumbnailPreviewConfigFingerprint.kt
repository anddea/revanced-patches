package app.revanced.patches.youtube.seekbar.thumbnailpreview.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.util.bytecode.isWide32LiteralExists

object ThumbnailPreviewConfigFingerprint : MethodFingerprint(
    returnType = "Z",
    parameters = listOf(),
    customFingerprint = { it, _ -> it.isWide32LiteralExists(45398577) }
)