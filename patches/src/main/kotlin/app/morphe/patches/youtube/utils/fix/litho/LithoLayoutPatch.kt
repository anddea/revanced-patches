/*
 * Portions of this file are ported from Morphe:
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.utils.fix.litho

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.mainactivity.injectOnBackPressedMethodCall
import app.morphe.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.morphe.patches.youtube.utils.playservice.is_21_45_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.playservice.is_20_16_or_greater
import app.morphe.patches.youtube.utils.scrollTopParentFingerprint
import app.morphe.util.getWalkerMethod
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR =
    "$UTILS_PATH/DoubleBackToClosePatch;"

val lithoLayoutPatch = bytecodePatch(
    description = "lithoLayoutPatch"
) {
    dependsOn(versionCheckPatch)

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
        scrollPositionFingerprint.match().let {
            val walkerMethod =
                it.getWalkerMethod(it.instructionMatches.first().index + 1)
            val insertIndex = walkerMethod.implementation!!.instructions.size - 1 - 1

            walkerMethod.injectScrollView(insertIndex, "onStartScrollView")
        }

        // Inject the methods which stop of ScrollView
        if (!is_20_16_or_greater) {
            scrollTopFingerprint.match(scrollTopParentFingerprint.second.classDef).let {
                val insertIndex = it.instructionMatches.last().index

                it.method.injectScrollView(insertIndex, "onStopScrollView")
            }
        } else {
            // Hooked after the call that scrolls the feed to the top, not in the called method,
            // because the call goes through an interface with several identical implementations.
            // Must be done before the code added at the start of the same method below.
            RecyclerViewTopScrollingCallingMethodFingerprint.let {
                it.method.addInstruction(
                    it.instructionMatches.first().index + 1,
                    "invoke-static { }, $EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR->onStopScrollView()V"
                )
            }

            if (is_21_45_or_greater) {
                // The feature flag is gone and the feed always scrolls to the top. Back is handled by
                // one of two navigation controllers, and both ask the feed through this method, which
                // does the scrolling. Returning false skips it, and back then exits as usual.
                RecyclerViewTopScrollingCallingMethodFingerprint.method.apply {
                    val free = findFreeRegister(0)

                    addInstructionsWithLabels(
                        0,
                        """
                            const/4 v$free, 0x1
                            invoke-static { v$free }, $EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR->allowBackButtonToScrollToTopOfFeed(Z)Z
                            move-result v$free
                            if-nez v$free, :scroll_to_top
                            return v$free
                        """,
                        ExternalLabel("scroll_to_top", getInstruction(0))
                    )
                }
            } else {
                backToRefreshFeatureFlagFingerprint.matchAll().forEach {
                    it.method.insertLiteralOverride(
                        it.instructionMatches.first().index,
                        "$EXTENSION_DOUBLE_BACK_TO_CLOSE_CLASS_DESCRIPTOR->allowBackButtonToScrollToTopOfFeed(Z)Z"
                    )
                }
            }
        }

        // endregion

        // region fix swipe to refresh

        swipeRefreshLayoutFingerprint.match().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.last().index
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
