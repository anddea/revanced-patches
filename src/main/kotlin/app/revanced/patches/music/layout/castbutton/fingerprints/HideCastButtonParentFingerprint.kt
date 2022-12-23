package app.revanced.patches.music.layout.castbutton.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.shared.annotation.YouTubeMusicCompatibility
import org.jf.dexlib2.AccessFlags

@Name("hide-castbutton-parent-signature")
@YouTubeMusicCompatibility
@Version("0.0.1")
object HideCastButtonParentFingerprint : MethodFingerprint (
    "Z", AccessFlags.PRIVATE or AccessFlags.FINAL,
    strings = listOf("MediaRouteButton")
)
