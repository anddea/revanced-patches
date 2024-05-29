package app.revanced.patches.music.ads.general

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.ads.general.fingerprints.AccountMenuFooterFingerprint
import app.revanced.patches.music.ads.general.fingerprints.FloatingLayoutFingerprint
import app.revanced.patches.music.ads.general.fingerprints.GetPremiumTextViewFingerprint
import app.revanced.patches.music.ads.general.fingerprints.InterstitialsContainerFingerprint
import app.revanced.patches.music.ads.general.fingerprints.MembershipSettingsFingerprint
import app.revanced.patches.music.ads.general.fingerprints.MembershipSettingsParentFingerprint
import app.revanced.patches.music.ads.general.fingerprints.NotifierShelfFingerprint
import app.revanced.patches.music.ads.general.fingerprints.ShowDialogCommandFingerprint
import app.revanced.patches.music.navigation.components.NavigationBarComponentsPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.ADS_PATH
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ButtonContainer
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.FloatingLayout
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.InterstitialsContainer
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

@Suppress("unused")
object AdsPatch : BaseBytecodePatch(
    name = "Hide ads",
    description = "Adds options to hide ads.",
    dependencies = setOf(
        LithoFilterPatch::class,
        MusicAdsPatch::class,
        NavigationBarComponentsPatch::class, // for 'Hide upgrade button' setting
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountMenuFooterFingerprint,
        FloatingLayoutFingerprint,
        GetPremiumTextViewFingerprint,
        InterstitialsContainerFingerprint,
        MembershipSettingsParentFingerprint,
        NotifierShelfFingerprint,
        ShowDialogCommandFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/AdsFilter;"

    private const val FULLSCREEN_ADS_CLASS_DESCRIPTOR =
        "$ADS_PATH/FullscreenAdsPatch;"

    private const val PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumPromotionPatch;"

    private const val PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR =
        "$ADS_PATH/PremiumRenewalPatch;"

    override fun execute(context: BytecodeContext) {
        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        // region patch for hide fullscreen ads

        // non-litho view, used in some old clients
        InterstitialsContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(InterstitialsContainer) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->hideFullscreenAds(Landroid/view/View;)V"
                )
            }
        }

        // litho view, used in 'ShowDialogCommandOuterClass' in innertube
        ShowDialogCommandFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                // In this method, custom dialog is created and shown.
                // There were no issues despite adding “return-void” to the first index.
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range {p2 .. p2}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->hideFullscreenAds(Ljava/lang/Object;)Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )

                // If an issue occurs due to patching due to server-side changes in the future,
                // Find the instruction whose name is "show" in [MethodReference] and click the 'AlertDialog.BUTTON_POSITIVE' button.
                //
                // In this case, an instruction for 'getButton' must be added to smali, not in integrations
                // (This custom dialog cannot be cast to [AlertDialog] or [Dialog])
                //
                // See the comments below.

                // val dialogIndex = getTargetIndexWithMethodReferenceName("show")
                // val dialogReference = getInstruction<ReferenceInstruction>(dialogIndex).reference
                // val dialogDefiningClass = (dialogReference as MethodReference).definingClass
                // val getButtonMethod = context.findClass(dialogDefiningClass)!!
                //     .mutableClass.methods.first { method ->
                //         method.parameters == listOf("I")
                //                 && method.returnType == "Landroid/widget/Button;"
                //     }
                // val getButtonCall = dialogDefiningClass + "->" + getButtonMethod.name + "(I)Landroid/widget/Button;"
                // val dialogRegister = getInstruction<FiveRegisterInstruction>(dialogIndex).registerC
                // val freeIndex = getTargetIndex(dialogIndex, Opcode.IF_EQZ)
                // val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

                // addInstructions(
                //     dialogIndex + 1, """
                //         # Get the 'AlertDialog.BUTTON_POSITIVE' from custom dialog
                //         # Since this custom dialog cannot be cast to AlertDialog or Dialog,
                //         # It should come from smali, not integrations.
                //         const/4 v$freeRegister, -0x1
                //         invoke-virtual {v$dialogRegister, $freeRegister}, $getButtonCall
                //         move-result-object $freeRegister
                //         invoke-static {$freeRegister}, $FULLSCREEN_ADS_CLASS_DESCRIPTOR->confirmDialog(Landroid/widget/Button;)V
                //         """
                // )
            }
        }

        // endregion

        // region patch for hide premium promotion popup

        FloatingLayoutFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = getWideLiteralInstructionIndex(FloatingLayout) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PREMIUM_PROMOTION_POP_UP_CLASS_DESCRIPTOR->hidePremiumPromotion(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide premium renewal banner

        NotifierShelfFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val linearLayoutIndex = getWideLiteralInstructionIndex(ButtonContainer) + 3
                val linearLayoutRegister =
                    getInstruction<OneRegisterInstruction>(linearLayoutIndex).registerA

                addInstruction(
                    linearLayoutIndex + 1,
                    "invoke-static {v$linearLayoutRegister}, $PREMIUM_PROMOTION_BANNER_CLASS_DESCRIPTOR->hidePremiumRenewal(Landroid/widget/LinearLayout;)V"
                )
            }
        }

        // endregion

        // region patch for hide get premium

        // get premium button at the top of the account switching menu
        GetPremiumTextViewFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex + 1,
                    "const/4 v$register, 0x0"
                )
            }
        }

        // get premium button at the bottom of the account switching menu
        AccountMenuFooterFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(SharedResourceIdPatch.PrivacyTosFooter)
                val walkerIndex = getTargetIndex(constIndex + 2, Opcode.INVOKE_VIRTUAL)
                val viewIndex = getTargetIndex(constIndex, Opcode.IGET_OBJECT)
                val viewReference = getInstruction<ReferenceInstruction>(viewIndex).reference.toString()

                val walkerMethod = getWalkerMethod(context, walkerIndex)
                walkerMethod.apply {
                    val insertIndex = getTargetIndexWithReference(viewReference)
                    val nullCheckIndex = getTargetIndex(insertIndex - 1, Opcode.IF_NEZ)
                    val nullCheckRegister = getInstruction<OneRegisterInstruction>(nullCheckIndex).registerA

                    addInstruction(
                        nullCheckIndex,
                        "const/4 v$nullCheckRegister, 0x0"
                    )
                }
            }
        }

        // premium membership menu in settings
        MembershipSettingsFingerprint.resolve(
            context,
            MembershipSettingsParentFingerprint.resultOrThrow().classDef
        )
        MembershipSettingsFingerprint.resultOrThrow().mutableMethod.addInstructions(
            0, """
                const/4 v0, 0x0
                return-object v0
                """
        )

        // endregion

        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_fullscreen_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_general_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_music_ads",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_paid_promotion_label",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_promotion",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.ADS,
            "revanced_hide_premium_renewal",
            "true"
        )
    }
}
