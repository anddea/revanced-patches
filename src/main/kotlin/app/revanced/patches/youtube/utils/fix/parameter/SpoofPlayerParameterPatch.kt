package app.revanced.patches.youtube.utils.fix.parameter

import app.revanced.extensions.exception
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fingerprints.PlayerParameterBuilderFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelImplGeneralFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelImplLiveStreamFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelImplRecommendedLevel
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererSpecFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererSpecRecommendedLevelFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.patches.youtube.utils.playerresponse.PlayerResponsePatch
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.util.integrations.Constants.MISC_PATH
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    name = "Spoof player parameters",
    description = "Spoofs player parameters to prevent playback issues.",
    dependencies = [
        PlayerTypeHookPatch::class,
        PlayerResponsePatch::class,
        VideoIdPatch::class,
        SettingsPatch::class
    ],
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.youtube",
            [
                "18.25.40",
                "18.27.36",
                "18.29.38",
                "18.30.37",
                "18.31.40",
                "18.32.39",
                "18.33.40",
                "18.34.38",
                "18.35.36",
                "18.36.39",
                "18.37.36",
                "18.38.44",
                "18.39.41",
                "18.40.34",
                "18.41.39",
                "18.42.41",
                "18.43.45",
                "18.44.41",
                "18.45.43",
                "18.46.43"
            ]
        )
    ]
)
object SpoofPlayerParameterPatch : BytecodePatch(
    setOf(
        PlayerParameterBuilderFingerprint,
        PlayerResponseModelImplGeneralFingerprint,
        PlayerResponseModelImplLiveStreamFingerprint,
        PlayerResponseModelImplRecommendedLevel,
        StoryboardRendererSpecFingerprint,
        StoryboardRendererSpecRecommendedLevelFingerprint,
        StoryboardThumbnailParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Hook player parameter
         */
        PlayerResponsePatch += PlayerResponsePatch.Hook.PlayerParameter(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->spoofParameter(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
        )

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
         */
        arrayOf(
            PlayerResponseModelImplGeneralFingerprint,
            PlayerResponseModelImplLiveStreamFingerprint,
            StoryboardRendererSpecFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val getStoryBoardIndex = it.scanResult.patternScanResult!!.endIndex
                    val getStoryBoardRegister =
                        getInstruction<OneRegisterInstruction>(getStoryBoardIndex).registerA

                    addInstructionsWithLabels(
                        getStoryBoardIndex, """
                            if-nez v$getStoryBoardRegister, :ignore
                            invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec()Ljava/lang/String;
                            move-result-object v$getStoryBoardRegister
                            """, ExternalLabel("ignore", getInstruction(getStoryBoardIndex))
                    )
                }
            } ?: throw fingerprint.exception
        }

        /**
         * Hook recommended value and StoryBoard Renderer for live stream
         */
        StoryboardRendererSpecRecommendedLevelFingerprint.result?.let {
            it.mutableMethod.apply {
                val moveOriginalRecommendedValueIndex = it.scanResult.patternScanResult!!.endIndex
                val originalValueRegister =
                    getInstruction<OneRegisterInstruction>(moveOriginalRecommendedValueIndex).registerA

                val liveStreamStoryBoardUrlIndex =
                    implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_INTERFACE
                    } + 1
                val liveStreamStoryBoardUrlRegister =
                    getInstruction<OneRegisterInstruction>(liveStreamStoryBoardUrlIndex).registerA

                addInstructions(
                    moveOriginalRecommendedValueIndex + 1, """
                        invoke-static { v$originalValueRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getRecommendedLevel(I)I
                        move-result v$originalValueRegister
                        """
                )

                addInstructions(
                    liveStreamStoryBoardUrlIndex + 1, """
                        invoke-static { v$liveStreamStoryBoardUrlRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$liveStreamStoryBoardUrlRegister
                        """
                )
            }
        } ?: throw StoryboardRendererSpecRecommendedLevelFingerprint.exception

        PlayerResponseModelImplRecommendedLevel.result?.let {
            it.mutableMethod.apply {
                val moveOriginalRecommendedValueIndex = it.scanResult.patternScanResult!!.endIndex
                val originalValueRegister =
                    getInstruction<OneRegisterInstruction>(moveOriginalRecommendedValueIndex).registerA

                addInstructions(
                    moveOriginalRecommendedValueIndex, """
                        invoke-static { v$originalValueRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getRecommendedLevel(I)I
                        move-result v$originalValueRegister
                        """
                )
            }

        } ?: throw PlayerResponseModelImplRecommendedLevel.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "SETTINGS: EXPERIMENTAL_FLAGS",
                "SETTINGS: SPOOF_PLAYER_PARAMETER"
            )
        )

        SettingsPatch.updatePatchStatus("Spoof player parameters")

    }

    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofPlayerParameterPatch;"
}
