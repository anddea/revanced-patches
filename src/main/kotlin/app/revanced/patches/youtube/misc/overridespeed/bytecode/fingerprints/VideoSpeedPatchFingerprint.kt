package app.revanced.patches.youtube.misc.overridespeed.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object VideoSpeedPatchFingerprint : MethodFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC,
    parameters = listOf("F"),
    customFingerprint = { it, _ -> it.definingClass.endsWith("/VideoSpeedPatch;")  && it.name == "overrideSpeed"}
)