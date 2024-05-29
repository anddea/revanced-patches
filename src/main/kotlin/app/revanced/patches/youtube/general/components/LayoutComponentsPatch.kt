package app.revanced.patches.youtube.general.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.youtube.general.components.fingerprints.AccountListFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.AccountListParentFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.AccountMenuFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.AccountSwitcherAccessibilityLabelFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.AppBlockingCheckResultToStringFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.BottomUiContainerFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.FloatingMicrophoneFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.PiPNotificationFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.SettingsMenuFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.TooltipContentFullscreenFingerprint
import app.revanced.patches.youtube.general.components.fingerprints.TooltipContentViewFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.fingerprints.AccountMenuParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.AccountSwitcherAccessibility
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.viewgroup.ViewGroupMarginLayoutParamsHookPatch
import app.revanced.util.getTargetIndex
import app.revanced.util.getTargetIndexWithFieldReferenceType
import app.revanced.util.getTargetIndexWithMethodReferenceName
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
object LayoutComponentsPatch : BaseBytecodePatch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        ViewGroupMarginLayoutParamsHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AccountListParentFingerprint,
        AccountMenuParentFingerprint,
        AccountSwitcherAccessibilityLabelFingerprint,
        AppBlockingCheckResultToStringFingerprint,
        BottomUiContainerFingerprint,
        FloatingMicrophoneFingerprint,
        PiPNotificationFingerprint,
        SettingsMenuFingerprint,
        TooltipContentFullscreenFingerprint,
        TooltipContentViewFingerprint
    )
) {
    private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CustomFilter;"
    private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LayoutComponentsFilter;"

    override fun execute(context: BytecodeContext) {

        // region patch for disable pip notification

        PiPNotificationFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val checkCastCalls = implementation!!.instructions.withIndex()
                    .filter { instruction ->
                        (instruction.value as? ReferenceInstruction)?.reference.toString() == "Lcom/google/apps/tiktok/account/AccountId;"
                    }

                val checkCastCallSize = checkCastCalls.size
                if (checkCastCallSize != 3)
                    throw PatchException("Couldn't find target index, size: $checkCastCallSize")

                arrayOf(
                    checkCastCalls.elementAt(1).index,
                    checkCastCalls.elementAt(0).index
                ).forEach { index ->
                    addInstruction(
                        index + 1,
                        "return-void"
                    )
                }
            }
        }

        // endregion

        // region patch for disable update screen

        AppBlockingCheckResultToStringFingerprint.resultOrThrow().mutableClass.methods.first { method ->
            MethodUtil.isConstructor(method)
                    && method.parameters == listOf("Landroid/content/Intent;", "Z")
        }.addInstructions(
            1,
            "const/4 p1, 0x0"
        )

        // endregion

        // region patch for hide account menu

        // for you tab
        AccountListParentFingerprint.resultOrThrow().let { parentResult ->
            AccountListFingerprint.resolve(context, parentResult.classDef)

            AccountListFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 3
                    val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                    addInstruction(
                        targetIndex,
                        "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                                "$GENERAL_CLASS_DESCRIPTOR->hideAccountList(Landroid/view/View;Ljava/lang/CharSequence;)V"
                    )
                }
            }
        }

        // for tablet and old clients
        AccountMenuFingerprint.resolve(
            context,
            AccountMenuParentFingerprint.resultOrThrow().classDef
        )
        AccountMenuFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.startIndex + 2
                val targetInstruction = getInstruction<FiveRegisterInstruction>(targetIndex)

                addInstruction(
                    targetIndex,
                    "invoke-static {v${targetInstruction.registerC}, v${targetInstruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideAccountMenu(Landroid/view/View;Ljava/lang/CharSequence;)V"
                )
            }
        }

        // endregion

        // region patch for hide floating microphone

        FloatingMicrophoneFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->hideFloatingMicrophone(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide handle

        AccountSwitcherAccessibilityLabelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val constIndex = getWideLiteralInstructionIndex(AccountSwitcherAccessibility)
                val insertIndex = getTargetIndex(constIndex, Opcode.IF_EQZ)
                val setVisibilityIndex = getTargetIndexWithMethodReferenceName(insertIndex, "setVisibility")
                val visibilityRegister = getInstruction<FiveRegisterInstruction>(setVisibilityIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$visibilityRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHandle(I)I
                        move-result v$visibilityRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide settings menu

        SettingsMenuFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = getTargetIndexWithFieldReferenceType("Landroid/support/v7/widget/RecyclerView;")
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideSettingsMenu(Landroid/support/v7/widget/RecyclerView;)V"
                )
            }
        }

        // endregion

        // region patch for hide snack bar

        BottomUiContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideSnackBar()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }

        // endregion

        // region patch for hide tooltip content

        TooltipContentFullscreenFingerprint.resultOrThrow().mutableMethod.apply {
            val literalIndex = getWideLiteralInstructionIndex(45384061)
            val targetIndex = getTargetIndex(literalIndex, Opcode.MOVE_RESULT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "const/4 v$targetRegister, 0x0"
            )
        }

        TooltipContentViewFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "return-void"
        )

        // endregion

        LithoFilterPatch.addFilter(CUSTOM_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HIDE_LAYOUT_COMPONENTS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
