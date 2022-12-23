package app.revanced.patches.music.layout.miniplayercolor.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

@Name("miniplayer-color-hook-fingerprint")
@YouTubeMusicCompatibility
@Version("0.0.1")
object MiniplayerColorParentFingerprint : MethodFingerprint(
    "V",
    AccessFlags.PRIVATE or AccessFlags.FINAL,
    null,
    listOf(
        Opcode.IGET,
        Opcode.IGET,
        Opcode.CONST_WIDE_16,
        Opcode.IF_EQ,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET,
        Opcode.IGET,
        Opcode.IF_EQ,
        Opcode.IPUT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    )
)