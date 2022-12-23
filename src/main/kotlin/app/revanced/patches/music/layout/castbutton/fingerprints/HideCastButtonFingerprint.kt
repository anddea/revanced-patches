package app.revanced.patches.music.layout.castbutton.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.AccessFlags

@Name("hide-castbutton-signature")
@YouTubeMusicCompatibility
@Version("0.0.1")
object HideCastButtonFingerprint : MethodFingerprint (
    "V", AccessFlags.PUBLIC or AccessFlags.FINAL, listOf("I"), null ,null, null
)