package app.revanced.patches.youtube.utils.fix.litho

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.mainactivity.injectOnBackPressedMethodCall
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playservice.is_20_16_or_greater
import app.revanced.patches.youtube.utils.scrollTopParentFingerprint
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.getWalkerMethod
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DoubleBackToClosePatch;"

val lithoLayoutPatch = bytecodePatch(
    description = "lithoLayoutPatch"
) {
    execute {

        // region patch double back to close

        fun MutableMethod.injectScrollView(
            index: Int,
            descriptor: String
        ) = addInstruction(
            index,
            "invoke-static {}, $EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR->$descriptor()V"
        )

        // Hook onBackPressed method inside MainActivity (WatchWhileActivity)
        injectOnBackPressedMethodCall(
            EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR,
            "closeActivityOnBackPressed"
        )

        // Inject the methods which start of ScrollView
        scrollPositionFingerprint.matchOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(it.patternMatch!!.startIndex + 1)
            val insertIndex = walkerMethod.implementation!!.instructions.size - 1 - 1

            walkerMethod.injectScrollView(insertIndex, "onStartScrollView")
        }

        // Inject the methods which stop of ScrollView
        val fingerprint = if (is_20_16_or_greater) scrollTopFingerprint2016 else scrollTopFingerprint
        fingerprint.matchOrThrow(scrollTopParentFingerprint).let {
            val insertIndex = it.patternMatch!!.endIndex

            it.method.injectScrollView(insertIndex, "onStopScrollView")
        }

        // endregion

        // region fix swipe to refresh

        swipeRefreshLayoutFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val register = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$register, 0x0"
                )
            }
        }

        // endregion

    }
}
