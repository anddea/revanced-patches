package app.revanced.patches.youtube.misc.microg.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object GooglePlayUtilityFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    strings = listOf("This should never happen.", "MetadataValueReader", "com.google.android.gms")
)