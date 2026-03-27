package app.morphe.patches.youtube.player.flyoutmenu.hide

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.indexOfAddHeaderViewInstruction
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_FLYOUT_MENU
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_18_39_or_greater
import app.morphe.patches.youtube.utils.playservice.is_19_30_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.morphe.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.REGISTER_TEMPLATE_REPLACEMENT
import app.morphe.util.fingerprint.injectLiteralInstructionBooleanCall
import app.morphe.util.fingerprint.injectLiteralInstructionViewCall
import app.morphe.util.fingerprint.methodOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val PANELS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val playerFlyoutMenuPatch = bytecodePatch(
    HIDE_PLAYER_FLYOUT_MENU.title,
    HIDE_PLAYER_FLYOUT_MENU.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        videoInformationPatch,
        versionCheckPatch
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "PREFERENCE_SCREENS: FLYOUT_MENU",
            "SETTINGS: HIDE_PLAYER_FLYOUT_MENU"
        )

        // region hide player flyout menu header, footer (non-litho)

        mapOf(
            advancedQualityBottomSheetFingerprint to "hidePlayerFlyoutMenuQualityFooter",
            captionsBottomSheetFingerprint to "hidePlayerFlyoutMenuCaptionsFooter",
            qualityMenuViewInflateFingerprint to "hidePlayerFlyoutMenuQualityFooter"
        ).forEach { (fingerprint, name) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V
                    """
            fingerprint.injectLiteralInstructionViewCall(bottomSheetFooterText, smaliInstruction)
        }

        arrayOf(
            advancedQualityBottomSheetFingerprint,
            qualityMenuViewInflateFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                val insertIndex = indexOfAddHeaderViewInstruction(this)
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuQualityHeader(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide '1080p Premium' label

        currentVideoFormatConstructorFingerprint.methodOrThrow(
            currentVideoFormatToStringFingerprint
        ).apply {
            val videoQualitiesIndex =
                indexOfVideoQualitiesInstruction(this)
            val videoQualitiesRegister =
                getInstruction<TwoRegisterInstruction>(videoQualitiesIndex).registerA

            addInstructions(
                1, """
                    invoke-static/range { v$videoQualitiesRegister .. v$videoQualitiesRegister }, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuEnhancedBitrate([$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE)[$YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
                    move-result-object v$videoQualitiesRegister
                    """
            )
        }

        // endregion

        // region patch for hide pip mode menu

        if (is_18_39_or_greater) {
            pipModeConfigFingerprint.injectLiteralInstructionBooleanCall(
                45427407L,
                "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
            )
            settingArray += "SETTINGS: HIDE_PIP_MODE_MENU"
        }

        // endregion

        // region patch for hide sleep timer menu

        if (is_19_30_or_greater) {
            // Sleep timer menu in Additional settings (deprecated)
            // TODO: A patch will be implemented to assign this deprecated menu to another action.
            // mapOf(
            //     sleepTimerConstructorFingerprint to SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG,
            //     sleepTimerFingerprint to SLEEP_TIMER_FEATURE_FLAG
            // ).forEach { (fingerprint, literal) ->
            //     fingerprint.injectLiteralInstructionBooleanCall(
            //         literal,
            //         "$PLAYER_CLASS_DESCRIPTOR->hideDeprecatedSleepTimerMenu(Z)Z"
            //     )
            // }
            settingArray += "SETTINGS: HIDE_SLEEP_TIMER_MENU"
        }

        // endregion

        addLithoFilter(PANELS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, HIDE_PLAYER_FLYOUT_MENU)

        // endregion

    }
}
