package app.revanced.patches.youtube.swipe.swipecontrols.bytecode.patch

import app.revanced.extensions.exception
import app.revanced.extensions.transformMethods
import app.revanced.extensions.traverseClassHierarchy
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
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
    override fun execute(context: BytecodeContext) {
        val wrapperClass = SwipeControlsHostActivityFingerprint.result?.mutableClass
            ?: throw SwipeControlsHostActivityFingerprint.exception
        val targetClass = WatchWhileActivityFingerprint.result?.mutableClass
            ?: throw WatchWhileActivityFingerprint.exception

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

    }
}