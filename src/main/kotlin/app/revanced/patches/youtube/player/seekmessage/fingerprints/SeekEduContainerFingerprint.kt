package app.revanced.patches.youtube.player.seekmessage.fingerprints

import app.revanced.patcher.fingerprint.MethodFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.EasySeekEduContainer
import app.revanced.util.bytecode.isWideLiteralExists

object SeekEduContainerFingerprint : MethodFingerprint(
    returnType = "V",
    customFingerprint = { methodDef, _ -> methodDef.isWideLiteralExists(EasySeekEduContainer) }
)