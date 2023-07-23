package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch

import app.revanced.extensions.toErrorResult
import app.revanced.extensions.transformMethods
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.TypeUtil.traverseClassHierarchy
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.SwipeControlsHostActivityFingerprint
import app.revanced.patches.youtube.swipe.swipecontrols.bytecode.fingerprints.WatchWhileActivityFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

@DependsOn([PlayerTypeHookPatch::class])
class SwipeControlsBytecodePatch : BytecodePatch(
    listOf(
        SwipeControlsHostActivityFingerprint,
        WatchWhileActivityFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val wrapperClass = SwipeControlsHostActivityFingerprint.result?.mutableClass
            ?: return SwipeControlsHostActivityFingerprint.toErrorResult()
        val targetClass = WatchWhileActivityFingerprint.result?.mutableClass
            ?: return WatchWhileActivityFingerprint.toErrorResult()

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