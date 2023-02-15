package app.revanced.patches.youtube.layout.player.infocards.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import org.jf.dexlib2.AccessFlags

@Name("infocards-incognito-fingerprint")
@YouTubeCompatibility
@Version("0.0.1")
object InfocardsIncognitoFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/Boolean;",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("vibrator")
)