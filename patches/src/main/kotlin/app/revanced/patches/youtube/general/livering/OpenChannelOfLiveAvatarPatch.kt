package app.revanced.patches.youtube.general.livering

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.utils.engagement.hookEngagementPanelState
import app.revanced.patches.youtube.utils.extension.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.CHANGE_LIVE_RING_CLICK_ACTION
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.playbackstart.playbackStartDescriptorPatch
import app.revanced.patches.youtube.video.playbackstart.playbackStartVideoIdReference
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$GENERAL_PATH/OpenChannelOfLiveAvatarPatch;"

@Suppress("unused")
val openChannelOfLiveAvatarPatch = bytecodePatch(
    CHANGE_LIVE_RING_CLICK_ACTION.title,
    CHANGE_LIVE_RING_CLICK_ACTION.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        sharedResourceIdPatch,
        playbackStartDescriptorPatch,
        engagementPanelHookPatch,
    )

    execute {

        elementsImageFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->liveChannelAvatarClicked()V"
        )

        hookEngagementPanelState(EXTENSION_CLASS_DESCRIPTOR)

        clientSettingEndpointFingerprint.methodOrThrow().apply {
            val eqzIndex = indexOfFirstInstructionReversedOrThrow(Opcode.IF_EQZ)
            var freeIndex = indexOfFirstInstructionReversedOrThrow(eqzIndex, Opcode.NEW_INSTANCE)
            var freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

            addInstructionsWithLabels(
                eqzIndex, """
                    invoke-static { }, $EXTENSION_CLASS_DESCRIPTOR->openChannelOfLiveAvatar()Z
                    move-result v$freeRegister
                    if-eqz v$freeRegister, :ignore
                    return-void
                    :ignore
                    nop
                    """
            )

            val playbackStartIndex = indexOfPlaybackStartDescriptorInstruction(this) + 1
            val playbackStartRegister = getInstruction<OneRegisterInstruction>(playbackStartIndex).registerA

            freeIndex = indexOfFirstInstructionOrThrow(playbackStartIndex, Opcode.CONST_STRING)
            freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA

            addInstructions(
                playbackStartIndex + 1, """
                    invoke-virtual { v$playbackStartRegister }, $playbackStartVideoIdReference
                    move-result-object v$freeRegister
                    invoke-static { v$freeRegister }, $EXTENSION_CLASS_DESCRIPTOR->openChannelOfLiveAvatar(Ljava/lang/String;)V
                    """
            )
        }

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "PREFERENCE_CATEGORY: GENERAL_EXPERIMENTAL_FLAGS",
                "SETTINGS: CHANGE_LIVE_RING_CLICK_ACTION"
            ),
            CHANGE_LIVE_RING_CLICK_ACTION
        )

        // endregion

    }
}
