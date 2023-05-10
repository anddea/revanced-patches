package app.revanced.patches.youtube.layout.seekbar.seekbartapping.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.accessibilityProgressTimeId
import app.revanced.util.bytecode.isWideLiteralExists
import org.jf.dexlib2.AccessFlags

object SeekbarTappingParentFingerprint : MethodFingerprint(
    returnType = "L",
    access = AccessFlags.PUBLIC or AccessFlags.FINAL,
    customFingerprint = { it.isWideLiteralExists(accessibilityProgressTimeId)}
)