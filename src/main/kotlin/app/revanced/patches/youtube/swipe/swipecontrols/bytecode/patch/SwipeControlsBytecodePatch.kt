package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.TypeUtil.traverseClassHierarchy
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.misc.hdrbrightness.bytecode.patch.HDRBrightnessBytecodePatch
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.SwipeControlsHostActivityFingerprint
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.WatchWhileActivityFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.transformMethods
import org.jf.dexlib2.AccessFlags
import org.jf.dexlib2.immutable.ImmutableMethod

@Name("swipe-controls-bytecode-patch")
@YouTubeCompatibility
@Version("0.0.3")
@DependsOn(
    [
        HDRBrightnessBytecodePatch::class,
        PlayerTypeHookPatch::class
    ]
)
class SwipeControlsBytecodePatch : BytecodePatch(
    listOf(
        SwipeControlsHostActivityFingerprint,
        WatchWhileActivityFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val wrapperClass = SwipeControlsHostActivityFingerprint.result!!.mutableClass
        val targetClass = WatchWhileActivityFingerprint.result!!.mutableClass

        // inject the wrapper class from integrations into the class hierarchy of WatchWhileActivity
        wrapperClass.setSuperClass(targetClass.superclass)
        targetClass.setSuperClass(wrapperClass.type)

        // ensure all classes and methods in the hierarchy are non-final, so we can override them in integrations
        context.traverseClassHierarchy(targetClass) {
            accessFlags = accessFlags and AccessFlags.FINAL.value.inv()
            transformMethods {
                ImmutableMethod(
                    definingClass,
                    name,
                    parameters,
                    returnType,
                    accessFlags and AccessFlags.FINAL.value.inv(),
                    annotations,
                    hiddenApiRestrictions,
                    implementation
                ).toMutable()
            }
        }

        return PatchResultSuccess()
    }
}