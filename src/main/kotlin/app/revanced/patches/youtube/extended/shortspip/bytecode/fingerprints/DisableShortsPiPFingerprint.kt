package app.revanced.patches.youtube.extended.shortspip.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object DisableShortsPiPFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L"),
    customFingerprint = { it.definingClass == "Lcom/google/android/apps/youtube/app/common/ui/pip/DefaultPipController;" }
)