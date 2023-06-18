package app.revanced.patches.youtube.layout.fullscreen.seekmessage.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.misc.resourceid.patch.SharedResourceIdPatch.Companion.easySeekEduContainerId
import app.revanced.util.bytecode.isWideLiteralExists

object SeekEduContainerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { it, _ -> it.isWideLiteralExists(easySeekEduContainerId) }
)