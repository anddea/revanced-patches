package app.revanced.patches.youtube.misc.doublebacktoclose.patch

import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.OnBackPressedFingerprint
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.ScrollPositionFingerprint
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.ScrollTopFingerprint
import app.revanced.patches.youtube.misc.doublebacktoclose.fingerprint.ScrollTopParentFingerprint
import app.revanced.shared.annotation.YouTubeCompatibility
import app.revanced.shared.extensions.toErrorResult
import app.revanced.shared.patches.gestures.PredictiveBackGesturePatch
import app.revanced.shared.util.integrations.Constants.UTILS_PATH

@Name("double-back-to-close")
@DependsOn([PredictiveBackGesturePatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class DoubleBackToClosePatch : BytecodePatch(
    listOf(
        OnBackPressedFingerprint,
        ScrollPositionFingerprint,
        ScrollTopParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        /*
        Hook onBackPressed method inside WatchWhileActivity
         */
        OnBackPressedFingerprint.result?.let {
            val insertIndex = it.scanResult.patternScanResult!!.endIndex

            with(it.mutableMethod) {
                addInstruction(
                    insertIndex,
                    "invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR" +
                    "->" +
                    "closeActivityOnBackPressed(Lcom/google/android/apps/youtube/app/watchwhile/WatchWhileActivity;)V"
                )
            }
        } ?: return OnBackPressedFingerprint.toErrorResult()


        /*
        Inject the methods which start of ScrollView
         */
        ScrollPositionFingerprint.result?.let {
            val insertMethod = context.toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex + 1, true)
                .getMethod() as MutableMethod

            val insertIndex = insertMethod.implementation!!.instructions.size - 1 - 1

            injectScrollView(insertMethod, insertIndex, "onStartScrollView")
        } ?: return ScrollPositionFingerprint.toErrorResult()


        /*
        Inject the methods which stop of ScrollView
         */
        ScrollTopParentFingerprint.result?.let { parentResult ->
            ScrollTopFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                val insertMethod = it.mutableMethod
                val insertIndex = it.scanResult.patternScanResult!!.endIndex

                injectScrollView(insertMethod, insertIndex, "onStopScrollView")
            } ?: return ScrollTopFingerprint.toErrorResult()
        } ?: return ScrollTopParentFingerprint.toErrorResult()

        return PatchResultSuccess()
    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$UTILS_PATH/DoubleBackToClosePatch;"

        fun injectScrollView(
            method: MutableMethod,
            index: Int,
            descriptor: String
        ) {
            method.addInstruction(
                index,
                "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor()V"
            )
        }
    }
}
