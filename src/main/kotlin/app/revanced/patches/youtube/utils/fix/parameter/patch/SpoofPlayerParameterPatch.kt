package app.revanced.patches.youtube.utils.fix.parameter.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerParameterBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelImplFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererSpecFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.patches.youtube.utils.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch
@Name("Spoof player parameters")
@Description("Spoofs player parameters to prevent playback issues.")
@DependsOn([PlayerTypeHookPatch::class])
@YouTubeCompatibility
class SpoofPlayerParameterPatch : BytecodePatch(
    listOf(
        PlayerParameterBuilderFingerprint,
        PlayerResponseModelImplFingerprint,
        StoryboardRendererSpecFingerprint,
        StoryboardThumbnailParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hook player parameter
         */
        PlayerParameterBuilderFingerprint.result?.let {
            it.mutableMethod.apply {
                val videoIdRegister = 1
                val playerParameterRegister = 3

                addInstructions(
                    0, """
                        invoke-static {p$videoIdRegister, p$playerParameterRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->spoofParameter(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$playerParameterRegister
                        """
                )
            }
        } ?: throw PlayerParameterBuilderFingerprint.exception

        /**
         * Forces the SeekBar thumbnail preview container to be shown
         * I don't think this code is needed anymore
         */
        StoryboardThumbnailParentFingerprint.result?.classDef?.let { classDef ->
            StoryboardThumbnailFingerprint.also {
                it.resolve(
                    context,
                    classDef
                )
            }.result?.let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.endIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    // Since this is end of the method must replace one line then add the rest.
                    addInstructions(
                        targetIndex + 1,
                        """
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getSeekbarThumbnailOverrideValue()Z
                            move-result v$targetRegister
                            return v$targetRegister
                            """
                    )
                    removeInstruction(targetIndex)
                }
            } ?: throw StoryboardThumbnailFingerprint.exception
        } ?: throw StoryboardThumbnailParentFingerprint.exception

        /**
         * Hook StoryBoard Renderer URL
         * TODO: Find a way to increase the quality of SeekBar thumbnail previews
         */
        PlayerResponseModelImplFingerprint.result?.let {
            it.mutableMethod.apply {
                val getStoryBoardIndex = it.scanResult.patternScanResult!!.endIndex
                val getStoryBoardRegister =
                    getInstruction<OneRegisterInstruction>(getStoryBoardIndex).registerA

                addInstructions(
                    getStoryBoardIndex, """
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec()Ljava/lang/String;
                        move-result-object v$getStoryBoardRegister
                        """
                )
            }
        } ?: throw PlayerResponseModelImplFingerprint.exception

        StoryboardRendererSpecFingerprint.result?.let {
            it.mutableMethod.apply {
                val storyBoardUrlParams = 0

                addInstructionsWithLabels(
                    0, """
                        if-nez p$storyBoardUrlParams, :ignore
                        invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec()Ljava/lang/String;
                        move-result-object p$storyBoardUrlParams
                        """, ExternalLabel("ignore", getInstruction(0))
                )
            }
        } ?: throw StoryboardRendererSpecFingerprint.exception


        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

    }

    private companion object {
        const val INTEGRATIONS_CLASS_DESCRIPTOR =
            "$MISC_PATH/SpoofPlayerParameterPatch;"
    }
}
