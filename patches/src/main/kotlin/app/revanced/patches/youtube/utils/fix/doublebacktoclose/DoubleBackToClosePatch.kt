package app.revanced.patches.youtube.utils.fix.doublebacktoclose

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.mainactivity.injectOnBackPressedMethodCall
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.scrollTopParentFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.getWalkerMethod

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DoubleBackToClosePatch;"

val doubleBackToClosePatch = bytecodePatch(
    description = "doubleBackToClosePatch"
) {
    execute {
        fun MutableMethod.injectScrollView(
            index: Int,
            descriptor: String
        ) = addInstruction(
            index,
            "invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->$descriptor()V"
        )

        /**
         * Hook onBackPressed method inside MainActivity (WatchWhileActivity)
         */
        injectOnBackPressedMethodCall(
            EXTENSION_CLASS_DESCRIPTOR,
            "closeActivityOnBackPressed"
        )

        /**
         * Inject the methods which start of ScrollView
         */
        scrollPositionFingerprint.matchOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex + 1)
            val insertIndex = walkerMethod.implementation!!.instructions.size - 1 - 1

            walkerMethod.injectScrollView(insertIndex, "onStartScrollView")
        }

        /**
         * Inject the methods which stop of ScrollView
         */
        scrollTopFingerprint.matchOrThrow(scrollTopParentFingerprint).let {
            val insertIndex = it.patternMatch!!.endIndex

            it.method.injectScrollView(insertIndex, "onStopScrollView")
        }
    }
}
