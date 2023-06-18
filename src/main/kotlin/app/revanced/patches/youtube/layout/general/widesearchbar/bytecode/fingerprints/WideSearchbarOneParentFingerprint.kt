package app.revanced.patches.youtube.layout.general.widesearchbar.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object WideSearchbarOneParentFingerprint : MethodFingerprint(
    returnType = "V", accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L"),
    strings = listOf(
        "FEhistory",
        "FEmy_videos",
        "FEpurchases"
    )
)