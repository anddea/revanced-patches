package app.revanced.patches.youtube.layout.player.watermark.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.annotation.FuzzyPatternScanMethod
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

@FuzzyPatternScanMethod(3)
object HideWatermarkParentFingerprint : MethodFingerprint (
    "L", AccessFlags.PUBLIC or AccessFlags.FINAL, null, null, listOf("player_overlay_in_video_programming"), null
)
