package app.revanced.patches.youtube.player.seekmessage.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.patch.SharedResourceIdPatch.Companion.EasySeekEduContainer
import app.revanced.util.bytecode.isWideLiteralExists

object SeekEduContainerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(EasySeekEduContainer) }
)