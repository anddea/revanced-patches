package app.revanced.patches.youtube.ads.general

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.ads.baseAdsPatch
import app.revanced.patches.shared.ads.hookLithoFullscreenAds
import app.revanced.patches.shared.ads.hookNonLithoFullscreenAds
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.ADS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.patch.PatchList.HIDE_ADS
import app.revanced.patches.youtube.utils.resourceid.adAttribution
import app.revanced.patches.youtube.utils.resourceid.interstitialsContainer
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.findMutableMethodOf
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.injectHideViewCall
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

@Suppress("unused")
val adsPatch = bytecodePatch(
    HIDE_ADS.title,
    HIDE_ADS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        baseAdsPatch(ADS_CLASS_DESCRIPTOR, "hideVideoAds"),
        lithoFilterPatch,
        lithoLayoutPatch,
        sharedResourceIdPatch,
    )

    execute {
        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        // region patch for hide fullscreen ads

        // non-litho view, used in some old clients
        interstitialsContainerFingerprint
            .methodOrThrow()
            .hookNonLithoFullscreenAds(interstitialsContainer)

        // litho view, used in 'ShowDialogCommandOuterClass' in innertube
        showDialogCommandFingerprint
            .matchOrThrow()
            .hookLithoFullscreenAds()

        // endregion

        // region patch for hide general ads

        classes.forEach { classDef ->
            classDef.methods.forEach { method ->
                method.implementation.apply {
                    this?.instructions?.forEachIndexed { index, instruction ->
                        if (instruction.opcode != Opcode.CONST)
                            return@forEachIndexed
                        // Instruction to store the id adAttribution into a register
                        if ((instruction as Instruction31i).wideLiteral != adAttribution)
                            return@forEachIndexed

                        val insertIndex = index + 1

                        // Call to get the view with the id adAttribution
                        (instructions.elementAt(insertIndex)).apply {
                            if (opcode != Opcode.INVOKE_VIRTUAL)
                                return@forEachIndexed

                            // Hide the view
                            val viewRegister = (this as Instruction35c).registerC
                            proxy(classDef)
                                .mutableClass
                                .findMutableMethodOf(method)
                                .injectHideViewCall(
                                    insertIndex,
                                    viewRegister,
                                    ADS_CLASS_DESCRIPTOR,
                                    "hideAdAttributionView"
                                )
                        }
                    }
                }
            }
        }

        // endregion

        // region patch for hide get premium

        compactYpcOfferModuleViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val measuredWidthRegister =
                    getInstruction<TwoRegisterInstruction>(startIndex).registerA
                val measuredHeightInstruction =
                    getInstruction<TwoRegisterInstruction>(startIndex + 1)
                val measuredHeightRegister = measuredHeightInstruction.registerA
                val tempRegister = measuredHeightInstruction.registerB

                addInstructionsWithLabels(
                    startIndex + 2, """
                        invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideGetPremium()Z
                        move-result v$tempRegister
                        if-eqz v$tempRegister, :show
                        const/4 v$measuredWidthRegister, 0x0
                        const/4 v$measuredHeightRegister, 0x0
                        """, ExternalLabel("show", getInstruction(startIndex + 2))
                )
            }
        }

        // endregion

        // region patch for hide end screen store banner

        fullScreenEngagementAdContainerFingerprint.methodOrThrow().apply {
            val addListIndex = indexOfAddListInstruction(this)
            val addListInstruction =
                getInstruction<FiveRegisterInstruction>(addListIndex)
            val listRegister = addListInstruction.registerC
            val objectRegister = addListInstruction.registerD

            replaceInstruction(
                addListIndex,
                "invoke-static { v$listRegister, v$objectRegister }, " +
                        "$ADS_CLASS_DESCRIPTOR->hideEndScreenStoreBanner(Ljava/util/List;Ljava/lang/Object;)V"
            )
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ADS"
            ),
            HIDE_ADS
        )

        // endregion

    }
}
