package app.revanced.patches.youtube.seekbar.seekbarcolor.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.InlineTimeBarColorizedBarPlayedColorDark
import app.revanced.util.bytecode.isWideLiteralExists

object SeekbarColorFingerprint : MethodFingerprint(
    customFingerprint = { it, _ -> it.isWideLiteralExists(InlineTimeBarColorizedBarPlayedColorDark) }
)