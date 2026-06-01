package app.morphe.patches.youtube.player.flyoutmenu.hide

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.litho.addLithoFilter
import app.morphe.patches.shared.litho.lithoFilterPatch
import app.morphe.patches.youtube.utils.YOUTUBE_VIDEO_QUALITY_CLASS_TYPE
import app.morphe.patches.youtube.utils.compatibility.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.utils.componentlist.hookComponentList
import app.morphe.patches.youtube.utils.componentlist.lazilyConvertedElementHookPatch
import app.morphe.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.morphe.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.morphe.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.morphe.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_FLYOUT_MENU
import app.morphe.patches.youtube.utils.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.utils.playservice.is_19_30_or_greater
import app.morphe.patches.youtube.utils.playservice.versionCheckPatch
import app.morphe.patches.youtube.utils.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.utils.proto.hookElement
import app.morphe.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.morphe.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.morphe.patches.youtube.utils.settings.settingsPatch
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.getFiveRegisters
import app.morphe.util.insertLiteralOverride
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val PANELS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val playerFlyoutMenuPatch = bytecodePatch(
    HIDE_PLAYER_FLYOUT_MENU.title,
    HIDE_PLAYER_FLYOUT_MENU.summary,
) {
    compatibleWith(COMPATIBILITY_YOUTUBE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        lazilyConvertedElementHookPatch,
        playerTypeHookPatch,
        elementProtoParserHookPatch,
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

        hookElement("$PLAYER_CLASS_DESCRIPTOR->hideNativeBottomSheetHeader([B)[B")
        hookComponentList("$PLAYER_CLASS_DESCRIPTOR->hideNativeBottomSheetFooter")

        CaptionsOldBottomSheetLayoutInflaterFingerprint.matchAll(1..2).forEach { match ->
            match.method.apply {
                val footerViewIndex = match.instructionMatches.last().index
                val footerViewArgs = getFiveRegisters(footerViewIndex)

                replaceInstruction(
                    footerViewIndex,
                    "invoke-static { $footerViewArgs }, $PLAYER_CLASS_DESCRIPTOR->" +
                        "hidePlayerFlyoutMenuCaptionsFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V"
                )
            }
        }

        QualityOldBottomSheetLayoutInflaterFingerprint.matchAll(2..3).forEach { match ->
            match.method.apply {
                val footerViewIndex = match.instructionMatches.last().index
                val footerViewArgs = getFiveRegisters(footerViewIndex)

                replaceInstruction(
                    footerViewIndex,
                    "invoke-static { $footerViewArgs }, $PLAYER_CLASS_DESCRIPTOR->" +
                        "hidePlayerFlyoutMenuQualityFooter(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V"
                )

                val headerViewIndex = match.instructionMatches[1].index
                val headerViewArgs = getFiveRegisters(headerViewIndex)

                replaceInstruction(
                    headerViewIndex,
                    "invoke-static { $headerViewArgs }, $PLAYER_CLASS_DESCRIPTOR->" +
                        "hidePlayerFlyoutMenuQualityHeader(Landroid/widget/ListView;Landroid/view/View;Ljava/lang/Object;Z)V"
                )
            }
        }

        // endregion

        // region patch for hide '1080p Premium' label

        CurrentVideoFormatConstructorFingerprint.method.apply {
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

        PipModeConfigFingerprint.method.insertLiteralOverride(
            45427407L,
            "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
        )

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
