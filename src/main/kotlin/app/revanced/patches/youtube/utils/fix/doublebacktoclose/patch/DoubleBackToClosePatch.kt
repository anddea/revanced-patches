package app.revanced.patches.youtube.utils.fix.doublebacktoclose.patch

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.OnBackPressedFingerprint
import app.revanced.patches.youtube.utils.fix.doublebacktoclose.fingerprint.ScrollPositionFingerprint
import app.revanced.patches.youtube.utils.fix.doublebacktoclose.fingerprint.ScrollTopFingerprint
import app.revanced.patches.youtube.utils.fix.doublebacktoclose.fingerprint.ScrollTopParentFingerprint
import app.revanced.util.integrations.Constants.UTILS_PATH

class DoubleBackToClosePatch : BytecodePatch(
    listOf(
        OnBackPressedFingerprint,
        ScrollPositionFingerprint,
        ScrollTopParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hook onBackPressed method inside WatchWhileActivity
         */
        OnBackPressedFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex

                addInstruction(
                    insertIndex,
                    "invoke-static {p0}, $INTEGRATIONS_CLASS_DESCRIPTOR" +
                            "->" +
                            "closeActivityOnBackPressed(Landroid/app/Activity;)V"
                )
            }
        } ?: throw OnBackPressedFingerprint.exception


        /**
         * Inject the methods which start of ScrollView
         */
        ScrollPositionFingerprint.result?.let {
            val insertMethod = context.toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.startIndex + 1, true)
                .getMethod() as MutableMethod

            val insertIndex = insertMethod.implementation!!.instructions.size - 1 - 1

            insertMethod.injectScrollView(insertIndex, "onStartScrollView")
        } ?: throw ScrollPositionFingerprint.exception


        /**
         * Inject the methods which stop of ScrollView
         */
        ScrollTopParentFingerprint.result?.let { parentResult ->
            ScrollTopFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex

                it.mutableMethod.injectScrollView(insertIndex, "onStopScrollView")
            } ?: throw ScrollTopFingerprint.exception
        } ?: throw ScrollTopParentFingerprint.exception

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$UTILS_PATH/DoubleBackToClosePatch;"

        fun MutableMethod.injectScrollView(
            index: Int,
            descriptor: String
        ) {
            addInstruction(
                index,
                "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->$descriptor()V"
            )
        }
    }
}
