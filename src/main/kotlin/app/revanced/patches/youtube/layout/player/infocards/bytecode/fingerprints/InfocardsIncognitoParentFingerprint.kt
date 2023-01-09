package app.revanced.patches.youtube.layout.player.infocards.bytecode.fingerprints

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import org.jf.dexlib2.AccessFlags

@Name("infocards-incognito-parent-fingerprint")
@YouTubeCompatibility
@Version("0.0.1")
object InfocardsIncognitoParentFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf("player_overlay_info_card_teaser"),
)