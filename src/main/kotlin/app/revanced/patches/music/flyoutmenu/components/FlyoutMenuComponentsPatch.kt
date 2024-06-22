package app.revanced.patches.music.flyoutmenu.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.flyoutmenu.components.fingerprints.DialogSolidFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.EndButtonsContainerFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.MenuItemFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.SleepTimerFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.TouchOutsideFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.TrimSilenceConfigFingerprint
import app.revanced.patches.music.flyoutmenu.components.fingerprints.TrimSilenceSwitchFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.flyoutmenu.FlyoutMenuHookPatch
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.integrations.Constants.FLYOUT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.EndButtonsContainer
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.patches.music.video.information.VideoInformationPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.getWalkerMethod
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object FlyoutMenuComponentsPatch : BaseBytecodePatch(
    name = "Flyout menu components",
    description = "Adds options to hide or change flyout menu components.",
    dependencies = setOf(
        FlyoutMenuComponentsResourcePatch::class,
        FlyoutMenuHookPatch::class,
        LithoFilterPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoInformationPatch::class,
        VideoTypeHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        DialogSolidFingerprint,
        EndButtonsContainerFingerprint,
        MenuItemFingerprint,
        SleepTimerFingerprint,
        TouchOutsideFingerprint,
        TrimSilenceConfigFingerprint,
        TrimSilenceSwitchFingerprint
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

    override fun execute(context: BytecodeContext) {
        var trimSilenceIncluded = false

        // region patch for enable compact dialog

        DialogSolidFingerprint.resultOrThrow().let {
            val walkerMethod =
                it.getWalkerMethod(context, it.scanResult.patternScanResult!!.endIndex)
            walkerMethod.addInstructions(
                2, """
                    invoke-static {p0}, $FLYOUT_CLASS_DESCRIPTOR->enableCompactDialog(I)I
                    move-result p0
                    """
            )
        }

        // endregion

        // region patch for enable trim silence

        TrimSilenceConfigFingerprint.result?.let {
            TrimSilenceConfigFingerprint.literalInstructionBooleanHook(
                45619123,
                "$FLYOUT_CLASS_DESCRIPTOR->enableTrimSilence(Z)Z"
            )

            TrimSilenceSwitchFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val constIndex =
                        getWideLiteralInstructionIndex(SharedResourceIdPatch.TrimSilenceSwitch)
                    val onCheckedChangedListenerIndex =
                        getTargetIndexOrThrow(constIndex, Opcode.INVOKE_DIRECT)
                    val onCheckedChangedListenerReference =
                        getInstruction<ReferenceInstruction>(onCheckedChangedListenerIndex).reference
                    val onCheckedChangedListenerDefiningClass =
                        (onCheckedChangedListenerReference as MethodReference).definingClass
                    val onCheckedChangedListenerClass =
                        context.findClass(onCheckedChangedListenerDefiningClass)!!.mutableClass

                    onCheckedChangedListenerClass.methods.find { method -> method.name == "onCheckedChanged" }
                        ?.apply {
                            val walkerIndex = indexOfFirstInstructionOrThrow {
                                val reference =
                                    ((this as? ReferenceInstruction)?.reference as? MethodReference)

                                opcode == Opcode.INVOKE_VIRTUAL
                                        && reference?.returnType == "V"
                                        && reference.parameterTypes.size == 1
                                        && reference.parameterTypes[0] == "Z"
                            }
                            getWalkerMethod(context, walkerIndex).apply {
                                val insertIndex = getTargetIndexOrThrow(Opcode.MOVE_RESULT)
                                val insertRegister =
                                    getInstruction<OneRegisterInstruction>(insertIndex).registerA

                                addInstructions(
                                    insertIndex + 1, """
                                    invoke-static {v$insertRegister}, $FLYOUT_CLASS_DESCRIPTOR->enableTrimSilenceSwitch(Z)Z
                                    move-result v$insertRegister
                                    """
                                )
                            }
                        } ?: throw PatchException("onClickClass not found!")
                }
            }

            trimSilenceIncluded = true
        }

        // endregion

        // region patch for hide flyout menu components and replace menu

        MenuItemFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val freeIndex = getTargetIndexOrThrow(Opcode.OR_INT_LIT16)
                val textViewIndex = it.scanResult.patternScanResult!!.startIndex
                val imageViewIndex = it.scanResult.patternScanResult!!.endIndex

                val freeRegister =
                    getInstruction<TwoRegisterInstruction>(freeIndex).registerA
                val textViewRegister =
                    getInstruction<OneRegisterInstruction>(textViewIndex).registerA
                val imageViewRegister =
                    getInstruction<OneRegisterInstruction>(imageViewIndex).registerA

                val enumIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC
                            && (this as? ReferenceInstruction)?.reference.toString()
                        .contains("(I)L")
                } + 1
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                addInstructionsWithLabels(
                    enumIndex + 1,
                    """
                        invoke-static {v$enumRegister, v$textViewRegister, v$imageViewRegister}, $FLYOUT_CLASS_DESCRIPTOR->replaceComponents(Ljava/lang/Enum;Landroid/widget/TextView;Landroid/widget/ImageView;)V
                        invoke-static {v$enumRegister}, $FLYOUT_CLASS_DESCRIPTOR->hideComponents(Ljava/lang/Enum;)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hide
                        """,
                    ExternalLabel("hide", getInstruction(implementation!!.instructions.size - 1))
                )
            }
        }

        TouchOutsideFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val setOnClickListenerIndex =
                    getTargetIndexWithMethodReferenceNameOrThrow("setOnClickListener")
                val setOnClickListenerRegister =
                    getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "invoke-static {v$setOnClickListenerRegister}, $FLYOUT_CLASS_DESCRIPTOR->setTouchOutSideView(Landroid/view/View;)V"
                )
            }
        }

        EndButtonsContainerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val startIndex = getWideLiteralInstructionIndex(EndButtonsContainer)
                val targetIndex = getTargetIndexOrThrow(startIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $FLYOUT_CLASS_DESCRIPTOR->hideLikeDislikeContainer(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for enable sleep timer

        /**
         * Forces sleep timer menu to be enabled.
         * This method may be desperate in the future.
         */
        SleepTimerFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // endregion

        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_enable_compact_dialog",
            "false"
        )
        if (trimSilenceIncluded) {
            SettingsPatch.addSwitchPreference(
                CategoryType.FLYOUT,
                "revanced_enable_trim_silence",
                "false"
            )
        }
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_like_dislike",
            "false",
            false
        )
        if (SettingsPatch.upward0636) {
            LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

            SettingsPatch.addSwitchPreference(
                CategoryType.FLYOUT,
                "revanced_hide_flyout_menu_3_column_component",
                "false",
                false
            )
        }
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_add_to_queue",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_captions",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_delete_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_dismiss_queue",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_download",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_edit_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_album",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_artist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_episode",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_podcast",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_help",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_play_next",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_quality",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_remove_from_library",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_remove_from_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_report",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_save_episode_for_later",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_save_to_library",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_save_to_playlist",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_share",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_shuffle_play",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_sleep_timer",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_start_radio",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_stats_for_nerds",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_subscribe",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_view_song_credit",
            "false",
            false
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_dismiss_queue",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_dismiss_queue_continue_watch",
            "true",
            "revanced_replace_flyout_menu_dismiss_queue"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_report",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_report_only_player",
            "true",
            "revanced_replace_flyout_menu_report"
        )
    }
}
