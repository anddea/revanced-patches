package app.revanced.patches.youtube.utils.playercontrols.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object FullscreenEngagementSpeedEduVisibleParentFingerprint : MethodFingerprint(
    returnType = "Ljava/lang/String;",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf(
        ", isSpeedmasterEDUVisible=",
        ", isFullscreenEngagementViewVisible="
    )
)