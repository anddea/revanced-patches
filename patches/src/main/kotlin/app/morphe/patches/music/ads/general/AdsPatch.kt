package app.morphe.patches.music.ads.general

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patches.music.navigation.components.navigationBarComponentsPatch
import app.morphe.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.music.utils.extension.Constants.ADS_PATH
import app.morphe.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.morphe.patches.music.utils.navigation.navigationBarHookPatch
import app.morphe.patches.music.utils.patch.PatchList.HIDE_ADS
import app.morphe.patches.music.utils.playservice.is_7_28_or_greater
import app.morphe.patches.music.utils.playservice.versionCheckPatch
import app.morphe.patches.music.utils.resourceid.buttonContainer
import app.morphe.patches.music.utils.resourceid.floatingLayout
import app.morphe.patches.music.utils.resourceid.privacyTosFooter
import app.morphe.patches.music.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.music.utils.settings.CategoryType
import app.morphe.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.morphe.patches.music.utils.settings.addSwitchPreference
import app.morphe.patches.music.utils.settings.settingsPatch
import app.morphe.patches.shared.ads.adsPatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.shared.mainactivity.onStartMethod
import app.morphe.patches.shared.mainactivity.onStopMethod
import app.morphe.util.fingerprint.matchOrThrow
import app.morphe.util.fingerprint.methodOrThrow
import app.morphe.util.getReference
import app.morphe.util.getWalkerMethod
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val ADS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/AdsFilter;"

private const val PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR =
    "$ADS_PATH/PremiumPromotionPatch;"

private const val PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR =
    "$ADS_PATH/PremiumRenewalPatch;"

@Suppress("unused")
val adsPatch = adsPatch(
    block = {
        compatibleWith(COMPATIBLE_PACKAGE)

        dependsOn(
            settingsPatch,
            lithoFilterPatch,
            navigationBarComponentsPatch, // for 'Hide upgrade button' setting
            navigationBarHookPatch,
            sharedResourceIdPatch,
            versionCheckPatch,
            mainActivityResolvePatch,
        )
    },
    classDescriptor = "$ADS_PATH/MusicAdsPatch;",
    methodDescriptor = "hideMusicAds",
    executeBlock = {
        // region patch for hide premium promotion popup

        // get premium bottom sheet
        floatingLayoutFingerprint.methodOrThrow().apply {
            val targetIndex = indexOfFirstLiteralInstructionOrThrow(floatingLayout) + 2
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->hidePremiumPromotionBottomSheet(Landroid/view/View;)V"
            )
        }


        // get premium dialog in player
        if (is_7_28_or_greater) {
            mapOf(
                onStartMethod to "onAppForegrounded",
                onStopMethod to "onAppBackgrounded"
            ).forEach { (method, name) ->
                method.addInstruction(
                    0,
                    "invoke-static {}, ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->$name()V"
                )
            }

            getPremiumDialogFingerprint
                .methodOrThrow(getPremiumDialogParentFingerprint)
                .apply {
                    val setContentViewIndex = indexOfSetContentViewInstruction(this)
                    val dialogInstruction =
                        getInstruction<FiveRegisterInstruction>(setContentViewIndex)
                    val dialogRegister = dialogInstruction.registerC
                    val viewRegister = dialogInstruction.registerD

                    replaceInstruction(
                        setContentViewIndex,
                        "invoke-static {v$dialogRegister, v$viewRegister}, " +
                                " ${PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR}->hidePremiumPromotionDialog(Landroid/app/Dialog;Landroid/view/View;)V"
                    )
                }
        }

        // endregion

        // region patch for hide premium renewal banner

        notifierShelfFingerprint.methodOrThrow().apply {
            val linearLayoutIndex =
                indexOfFirstLiteralInstructionOrThrow(buttonContainer) + 3
            val linearLayoutRegister =
                getInstruction<OneRegisterInstruction>(linearLayoutIndex).registerA

            addInstruction(
                linearLayoutIndex + 1,
                "invoke-static {v$linearLayoutRegister}, ${PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR}->hidePremiumRenewal(Landroid/widget/LinearLayout;)V"
            )
        }

        // endregion

        // region patch for hide get premium

        // get premium button at the top of the account switching menu
        getPremiumTextViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.instructionMatches.first().index
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        }

        // get premium button at the bottom of the account switching menu
        accountMenuFooterFingerprint.methodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(privacyTosFooter)
            val walkerIndex =
                indexOfFirstInstructionOrThrow(constIndex + 2, Opcode.INVOKE_VIRTUAL)
            val viewIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.IGET_OBJECT)
            val viewReference =
                getInstruction<ReferenceInstruction>(viewIndex).reference.toString()

            val walkerMethod = getWalkerMethod(walkerIndex)
            walkerMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    getReference<FieldReference>()?.toString() == viewReference
                }
                val nullCheckIndex =
                    indexOfFirstInstructionOrThrow(insertIndex - 1, Opcode.IF_NEZ)
                val nullCheckRegister =
                    getInstruction<OneRegisterInstruction>(nullCheckIndex).registerA

                addInstruction(
                    nullCheckIndex,
                    "const/4 v$nullCheckRegister, 0x0"
                )
            }
        }

        addLithoFilter(ADS_FILTER_CLASS_DESCRIPTOR)

        // endregion

        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_fullscreen_ads",
            "false"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_general_ads",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_music_ads",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_paid_promotion_label",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_promotion",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_renewal",
            "true"
        )
        addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_promotion_alert_banner",
            "true"
        )

        updatePatchStatus(HIDE_ADS)
    }
)