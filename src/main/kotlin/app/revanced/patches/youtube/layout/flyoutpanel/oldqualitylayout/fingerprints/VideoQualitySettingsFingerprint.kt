package app.revanced.patches.youtube.layout.flyoutpanel.oldqualitylayout.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

object VideoQualitySettingsFingerprint : MethodFingerprint(
    returnType = "V",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("[L", "I", "Z"),
    opcodes = listOf(
        Opcode.IF_EQZ,
        Opcode.IF_EQ,
        Opcode.IF_EQ
    ),
    strings = listOf("menu_item_video_quality")
)