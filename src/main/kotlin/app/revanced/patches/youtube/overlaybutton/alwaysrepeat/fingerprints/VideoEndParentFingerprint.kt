package app.revanced.patches.youtube.overlaybutton.alwaysrepeat.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

object VideoEndParentFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    strings = listOf(
        "play() called when the player wasn't loaded.",
        "play() blocked because Background Playability failed"
    )
)