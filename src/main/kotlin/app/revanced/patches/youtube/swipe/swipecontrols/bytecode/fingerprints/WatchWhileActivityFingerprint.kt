package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints

import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint

object WatchWhileActivityFingerprint : MethodFingerprint(
    customFingerprint = {
        it.definingClass.endsWith("WatchWhileActivity;")
                && it.name == "<init>"
    }
)
