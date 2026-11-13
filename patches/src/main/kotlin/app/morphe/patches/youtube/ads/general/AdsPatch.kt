/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.ads.general

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.ANDROID_AUTOMOTIVE_STRING
import app.morphe.patches.shared.ads.adsPatch
import app.morphe.patches.shared.autoMotiveFingerprint
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.general.spoofappversion.Endpoint
import app.morphe.patches.youtube.general.spoofappversion.addOSNameHook
import app.morphe.patches.youtube.general.spoofappversion.clientContextHookPatch
import app.morphe.patches.shared.spoof.guide.addClientOSVersionHook
import app.morphe.patches.shared.spoof.guide.spoofClientGuideEndpointPatch
import app.morphe.patches.youtube.utils.engagement.addEngagementPanelIdHook
import app.morphe.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.extension.Constants.ADS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PATCH_STATUS_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_ADS
import app.morphe.patches.youtube.utils.playservice.is_20_06_or_greater
import app.morphe.patches.youtube.utils.playservice.is_20_21_or_greater
import app.morphe.patches.youtube.utils.playservice.is_21_25_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.resourceid.adAttribution
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.util.findMutableMethodOf
import app.morphe.util.indexOfFirstStringInstructionOrThrow
import app.morphe.util.injectHideViewCall
import app.morphe.util.updatePatchStatus
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction31i
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

/**
 * Category of the activated ad slot triggers that expire the slot.
 */
private const val SLOT_EXPIRATION_TRIGGER_CATEGORY = 6

@Suppress("unused")
val adsPatch = adsPatch(
    block = {
        compatibleWith(COMPATIBILITY_YOUTUBE)

        dependsOn(
            settingsPatch,
            lithoFilterPatch,
            lithoLayoutPatch,
            sharedResourceIdPatch,
            spoofClientGuideEndpointPatch,
            clientContextHookPatch,
            versionCheckPatch,
            engagementPanelHookPatch,
        )
    },
    classDescriptor = ADS_CLASS_DESCRIPTOR,
    methodDescriptor = "hideVideoAds",
    executeBlock = {
        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)
        addEngagementPanelIdHook("$ADS_CLASS_DESCRIPTOR->hidePlayerPopupAds(Ljava/lang/String;)Z")

        // Only the triggers that expire the ad slots are handled, so no ad slot is entered.
        // The slots are kept by the app until they expire, so without these triggers
        // every video adds its ad slots to the slots that are kept until the app closes.
        LoadVideoAdsFingerprint.let {
            val categoryField = it.instructionMatches.first().fieldReference

            val filterTriggersMethod = ImmutableMethod(
                it.classDef.type,
                "patch_filterVideoAdTriggers",
                listOf(ImmutableMethodParameter("Ljava/util/List;", null, null)),
                "Ljava/util/List;",
                AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(6),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static { }, $ADS_CLASS_DESCRIPTOR->hideVideoAds()Z
                        move-result v0
                        if-eqz v0, :keep_triggers
                        new-instance v0, Ljava/util/ArrayList;
                        invoke-direct { v0 }, Ljava/util/ArrayList;-><init>()V
                        invoke-interface { p0 }, Ljava/util/List;->iterator()Ljava/util/Iterator;
                        move-result-object v1
                        const/16 v4, $SLOT_EXPIRATION_TRIGGER_CATEGORY
                        :next_trigger
                        invoke-interface { v1 }, Ljava/util/Iterator;->hasNext()Z
                        move-result v2
                        if-eqz v2, :filtered_triggers
                        invoke-interface { v1 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                        move-result-object v2
                        check-cast v2, ${categoryField.definingClass}
                        iget v3, v2, $categoryField
                        if-ne v3, v4, :next_trigger
                        invoke-interface { v0, v2 }, Ljava/util/List;->add(Ljava/lang/Object;)Z
                        goto :next_trigger
                        :filtered_triggers
                        return-object v0
                        :keep_triggers
                        return-object p0
                    """
                )
            }
            it.classDef.methods.add(filterTriggersMethod)

            it.method.addInstructions(
                0,
                """
                    invoke-static/range { p1 .. p1 }, $filterTriggersMethod
                    move-result-object p1
                """
            )
        }

        // The player defers the start of playback until the player bytes ad slot is
        // scheduled. The hooks above stop the slot from ever being scheduled, so the
        // deferral is never released and the video never starts. Skip the deferral.
        if (is_21_25_or_greater) {
            PlayerBytesAdSchedulingFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static { }, $ADS_CLASS_DESCRIPTOR->hideVideoAds()Z
                    move-result v0
                    if-eqz v0, :show_video_ads
                    invoke-static { }, Lj$/util/Optional;->empty()Lj$/util/Optional;
                    move-result-object v0
                    return-object v0
                    :show_video_ads
                    nop
                """
            )
        }

        // region patch for hide general ads

        classDefForEach { classDef ->
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
                            mutableClassDefBy(classDef)
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

        compactYpcOfferModuleViewFingerprint.let {
            it.method.apply {
                val startIndex = it.instructionMatches.first().index
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

        // region patch for hide player overlay ad

        // This can be hidden with a regular Litho filter, but an empty space remains.
        if (is_20_21_or_greater) {
            // YouTube 21.39 moved the timely shelf check into a switch-lambda; hook inside the case instead.
            val timelyShelfMethod = runCatching { PlayerOverlayTimelyShelfFingerprint.method }.getOrNull()
            if (timelyShelfMethod != null) {
                timelyShelfMethod.addInstructionsWithLabels(
                    0, """
                    invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideAds()Z
                    move-result v0
                    if-eqz v0, :show
                    return-void
                    :show
                    nop
                    """
                )
            } else {
                playerOverlayTimelyShelfSwitchFingerprint.method.apply {
                    val stringIndex = indexOfFirstStringInstructionOrThrow("player_overlay_timely_shelf")
                    val optionalGetIndex = indexOfFirstInstructionReversedOrThrow(stringIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                                getReference<MethodReference>()?.toString()
                                    ?.endsWith("Optional;->get()Ljava/lang/Object;") == true
                    }
                    // invoke Optional.get, move-result-object, check-cast
                    addInstructionsWithLabels(
                        optionalGetIndex + 3, """
                        invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideAds()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        :show
                        nop
                        """
                    )
                }
            }
        }

        // endregion

        // region patch for hide end screen store banner

        fullScreenEngagementAdContainerFingerprint.method.apply {
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

        // region patch for hide shorts ad

        addOSNameHook(
            Endpoint.REEL,
            "$ADS_FILTER_CLASS_DESCRIPTOR->hideShortsAds(Ljava/lang/String;)Ljava/lang/String;"
        )

        // Hide Shorts ads by changing 'OSName' to 'Android Automotive'
        autoMotiveFingerprint.method.apply {
            val insertIndex = indexOfFirstStringInstructionOrThrow(ANDROID_AUTOMOTIVE_STRING) - 1
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex, """
                    invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsAds(Z)Z
                    move-result v$insertRegister
                    """
            )
        }

        // If 'OSName' is changed to 'Android Automotive' in all requests, a Notification button will appear in the navigation bar
        // To fix this side effect, requests to the '/guide' endpoint, which are related to navigation buttons, use the original 'OSName'
        addClientOSVersionHook(
            "patch_setClientOSNameByAdsPatch",
            "$ADS_CLASS_DESCRIPTOR->overrideOSName()Ljava/lang/String;",
            is_20_06_or_greater
        )

        // endregion

        // region patch for hide paid promotion label in Shorts (non-litho)

        shortsPaidPromotionFingerprint.method.apply {
            when (returnType) {
                "Landroid/widget/TextView;" -> {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex + 1, """
                            invoke-static {v$insertRegister}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel(Landroid/widget/TextView;)V
                            return-object v$insertRegister
                            """
                    )
                    removeInstruction(insertIndex)
                }

                "V" -> {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $ADS_CLASS_DESCRIPTOR->hideShortsPaidPromotionLabel()Z
                            move-result v0
                            if-eqz v0, :show
                            return-void
                            """, ExternalLabel("show", getInstruction(0))
                    )
                }

                else -> {
                    throw PatchException("Unknown returnType: $returnType")
                }
            }
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ADS"
            ),
            HIDE_ADS
        )

        updatePatchStatus(PATCH_STATUS_CLASS_DESCRIPTOR, "HideAds")

        // endregion
    }
)
