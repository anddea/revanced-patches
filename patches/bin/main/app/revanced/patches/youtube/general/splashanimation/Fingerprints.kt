package app.revanced.patches.youtube.general.splashanimation

import app.revanced.patches.youtube.utils.resourceid.darkSplashAnimation
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags

internal val splashAnimationFingerprint = legacyFingerprint(
    name = "splashAnimationFingerprint",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    literals = listOf(darkSplashAnimation),
    customFingerprint = { method, _ ->
        method.name == "onCreate"
    }
)

internal val startUpResourceIdFingerprint = legacyFingerprint(
    name = "startUpResourceIdFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("I"),
    literals = listOf(3L, 4L)
)

internal val startUpResourceIdParentFingerprint = legacyFingerprint(
    name = "startUpResourceIdParentFingerprint",
    returnType = "Z",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL or AccessFlags.DECLARED_SYNCHRONIZED,
    parameters = listOf("I", "I"),
    strings = listOf("early type", "final type")
)
