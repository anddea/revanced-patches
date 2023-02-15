package app.revanced.patches.youtube.layout.general.widesearchbar.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object WideSearchbarOneParentFingerprint : MethodFingerprint(
    returnType = "V", access = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf(
        "FEhistory",
        "FEmy_videos",
        "FEpurchases"
    )
)