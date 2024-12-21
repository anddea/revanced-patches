package app.revanced.patches.reddit.misc.openlink

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getWalkerMethod

lateinit var screenNavigatorMethod: MutableMethod

val screenNavigatorMethodResolverPatch = bytecodePatch(
    description = "screenNavigatorMethodResolverPatch"
) {
    execute {
        screenNavigatorMethod =
            // ~ Reddit 2024.25.3
            screenNavigatorFingerprint.second.methodOrNull
                // Reddit 2024.26.1 ~
                ?: with (customReportsFingerprint.methodOrThrow()) {
                    getWalkerMethod(indexOfScreenNavigatorInstruction(this))
                }
    }
}
