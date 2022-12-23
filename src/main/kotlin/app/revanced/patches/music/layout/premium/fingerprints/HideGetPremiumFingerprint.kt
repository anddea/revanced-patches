package app.revanced.patches.music.layout.premium.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.Opcode

@Name("hide-get-premium-fingerprint")
@YouTubeMusicCompatibility
@Version("0.0.1")
object HideGetPremiumFingerprint : MethodFingerprint(
    "V", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf(), listOf(
        Opcode.IF_NEZ,
        Opcode.CONST_16,
        Opcode.GOTO,
        Opcode.NOP,
        Opcode.INVOKE_VIRTUAL
    )
)
