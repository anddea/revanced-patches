package app.revanced.patches.youtube.overlaybutton.autorepeat.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object VideoEndFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf(),
    customFingerprint = { methodDef, _ -> methodDef.implementation!!.instructions.count() == 3 && methodDef.annotations.isEmpty() }
)