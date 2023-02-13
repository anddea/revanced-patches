package app.revanced.patches.youtube.misc.sponsorblock.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.util.MethodUtil

object NextGenWatchLayoutFingerprint : MethodFingerprint(
    returnType = "V", // constructors return void, in favour of speed of matching, this fingerprint has been added
    customFingerprint =  { MethodUtil.isConstructor(it) && it.parameterTypes.size == 3 && it.definingClass.endsWith("NextGenWatchLayout;") }
)