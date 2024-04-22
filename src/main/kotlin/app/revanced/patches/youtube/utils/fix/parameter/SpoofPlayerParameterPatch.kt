package app.revanced.patches.youtube.utils.fix.parameter

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.ParamsMapPutFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelGeneralStoryboardRendererFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelLiveStreamStoryboardRendererFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.PlayerResponseModelStoryboardRecommendedLevelFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StatsQueryParameterFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererDecoderRecommendedLevelFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererDecoderSpecFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardRendererSpecFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.fix.parameter.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.playerresponse.PlayerResponsePatch
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.videoid.general.VideoIdPatch
import app.revanced.util.exception
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Patch(
    name = "Spoof player parameters",
    description = "Adds options to spoof player parameters to prevent playback issues.",
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
                "18.46.45",
                "18.48.39",
                "18.49.37",
                "19.01.34",
                "19.02.39",
                "19.03.36",
                "19.04.38",
                "19.05.36",
                "19.06.39",
                "19.07.40",
                "19.08.36",
                "19.09.38",
                "19.10.39",
                "19.11.43",
                "19.12.41",
                "19.13.37",
                "19.14.43"
            ]
        )
    ],
    use = false
)
object SpoofPlayerParameterPatch : BytecodePatch(
    setOf(
        ParamsMapPutFingerprint,
        PlayerResponseModelGeneralStoryboardRendererFingerprint,
        PlayerResponseModelLiveStreamStoryboardRendererFingerprint,
        PlayerResponseModelStoryboardRecommendedLevelFingerprint,
        StatsQueryParameterFingerprint,
        StoryboardRendererDecoderRecommendedLevelFingerprint,
        StoryboardRendererDecoderSpecFingerprint,
        StoryboardRendererSpecFingerprint,
        StoryboardThumbnailParentFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$MISC_PATH/SpoofPlayerParameterPatch;"

    override fun execute(context: BytecodeContext) {

        // Hook the player parameters.
        PlayerResponsePatch += PlayerResponsePatch.Hook.PlayerParameter(
            "$INTEGRATIONS_CLASS_DESCRIPTOR->spoofParameter(Ljava/lang/String;Ljava/lang/String;Z)Ljava/lang/String;"
        )

        // Force the seekbar time and chapters to always show up.
        // This is used if the storyboard spec fetch fails, for viewing paid videos,
        // or if storyboard spoofing is turned off.
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

        // Hook storyboard renderer url.
        arrayOf(
            PlayerResponseModelGeneralStoryboardRendererFingerprint,
            PlayerResponseModelLiveStreamStoryboardRendererFingerprint
        ).forEach { fingerprint ->
            fingerprint.result?.let {
                it.mutableMethod.apply {
                    val getStoryboardIndex = it.scanResult.patternScanResult!!.endIndex
                    val getStoryboardRegister =
                        getInstruction<OneRegisterInstruction>(getStoryboardIndex).registerA

                    addInstructions(
                        getStoryboardIndex,
                        """
                            invoke-static { v$getStoryboardRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$getStoryboardRegister
                            """
                    )
                }
            } ?: throw fingerprint.exception
        }

        // Hook recommended seekbar thumbnails quality level.
        StoryboardRendererDecoderRecommendedLevelFingerprint.result?.let {
            it.mutableMethod.apply {
                val moveOriginalRecommendedValueIndex = it.scanResult.patternScanResult!!.endIndex
                val originalValueRegister =
                    getInstruction<OneRegisterInstruction>(moveOriginalRecommendedValueIndex).registerA

                addInstructions(
                    moveOriginalRecommendedValueIndex + 1, """
                        invoke-static { v$originalValueRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getRecommendedLevel(I)I
                        move-result v$originalValueRegister
                        """
                )
            }
        } ?: throw StoryboardRendererDecoderRecommendedLevelFingerprint.exception

        // Hook the recommended precise seeking thumbnails quality level.
        PlayerResponseModelStoryboardRecommendedLevelFingerprint.result?.let {
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
        } ?: throw PlayerResponseModelStoryboardRecommendedLevelFingerprint.exception

        StoryboardRendererSpecFingerprint.result?.let {
            it.mutableMethod.apply {
                val storyBoardUrlParams = 0

                addInstructionsWithLabels(
                    0, """
                        if-nez p$storyBoardUrlParams, :ignore
                        invoke-static { p$storyBoardUrlParams }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$storyBoardUrlParams
                        """, ExternalLabel("ignore", getInstruction(0))
                )
            }
        } ?: throw StoryboardRendererSpecFingerprint.exception

        // Hook the seekbar thumbnail decoder and use a NULL spec for live streams.
        StoryboardRendererDecoderSpecFingerprint.result?.let {
            val storyBoardUrlIndex = it.scanResult.patternScanResult!!.startIndex + 1
            val storyboardUrlRegister =
                it.mutableMethod.getInstruction<OneRegisterInstruction>(storyBoardUrlIndex).registerA

            it.mutableMethod.addInstructions(
                storyBoardUrlIndex + 1, """
                        invoke-static { v$storyboardUrlRegister }, $INTEGRATIONS_CLASS_DESCRIPTOR->getStoryboardDecoderRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$storyboardUrlRegister
                        """
            )
        } ?: throw StoryboardRendererDecoderSpecFingerprint.exception

        // Fix stats not being tracked.
        // Due to signature spoofing "adformat" is present in query parameters made for /stats requests,
        // even though, for regular videos, it should not be.
        // This breaks stats tracking.
        // Replace the ad parameter with the video parameter in the query parameters.
        StatsQueryParameterFingerprint.result?.let {
            val putMethod = ParamsMapPutFingerprint.result?.method?.toString()
                ?: throw ParamsMapPutFingerprint.exception

            it.mutableMethod.apply {
                val adParamIndex = it.scanResult.stringsScanResult!!.matches.first().index
                val videoParamIndex = adParamIndex + 3

                // Replace the ad parameter with the video parameter.
                replaceInstruction(adParamIndex, getInstruction(videoParamIndex))

                // Call paramsMap.put instead of paramsMap.putIfNotExist
                // because the key is already present in the map.
                val putAdParamIndex = adParamIndex + 1
                val putIfKeyNotExistsInstruction = getInstruction<FiveRegisterInstruction>(putAdParamIndex)
                replaceInstruction(
                    putAdParamIndex,
                    "invoke-virtual { " +
                        "v${putIfKeyNotExistsInstruction.registerC}, " +
                        "v${putIfKeyNotExistsInstruction.registerD}, " +
                        "v${putIfKeyNotExistsInstruction.registerE} }, " +
                        putMethod,
                )
            }
        } ?: throw StatsQueryParameterFingerprint.exception

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
}
