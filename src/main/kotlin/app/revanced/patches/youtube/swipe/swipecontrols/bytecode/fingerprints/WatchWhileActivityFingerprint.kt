package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint
import org.jf.dexlib2.AccessFlags

object WatchWhileActivityFingerprint : MethodFingerprint(
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    parameters = emptyList(),
    customFingerprint = { methodDef, _ -> methodDef.definingClass.endsWith("/WatchWhileActivity;") }
)
