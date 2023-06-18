package app.revanced.patches.youtube.layout.seekbar.seekbarcolor.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.inlineTimeBarColorizedBarPlayedColorDarkId
import app.revanced.util.bytecode.isWideLiteralExists

object SeekbarColorFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(inlineTimeBarColorizedBarPlayedColorDarkId) }
)