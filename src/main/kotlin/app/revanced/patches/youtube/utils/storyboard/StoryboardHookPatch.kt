package app.revanced.patches.youtube.utils.storyboard

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.storyboard.fingerprints.PlayerResponseModelGeneralStoryboardRendererFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.PlayerResponseModelLiveStreamStoryboardRendererFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.PlayerResponseModelStoryboardRecommendedLevelFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.StoryboardRendererDecoderRecommendedLevelFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.StoryboardRendererDecoderSpecFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.StoryboardRendererSpecFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.StoryboardThumbnailFingerprint
import app.revanced.patches.youtube.utils.storyboard.fingerprints.StoryboardThumbnailParentFingerprint
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    description = "Force inject the Storyboard by fetching YouTube API.",
    dependencies = [SharedResourceIdPatch::class],
)
object StoryboardHookPatch : BytecodePatch(
    setOf(
        PlayerResponseModelGeneralStoryboardRendererFingerprint,
        PlayerResponseModelLiveStreamStoryboardRendererFingerprint,
        PlayerResponseModelStoryboardRecommendedLevelFingerprint,
        StoryboardRendererDecoderRecommendedLevelFingerprint,
        StoryboardRendererDecoderSpecFingerprint,
        StoryboardRendererSpecFingerprint,
        StoryboardThumbnailParentFingerprint,
    )
) {
    private lateinit var context: BytecodeContext

    override fun execute(context: BytecodeContext) {
        this.context = context
    }

    internal fun hook(classDescriptor: String) {

        // Force the seekbar time and chapters to always show up.
        // This is used if the storyboard spec fetch fails, for viewing paid videos,
        // or if storyboard spoofing is turned off.
        StoryboardThumbnailFingerprint.resolve(
            context,
            StoryboardThumbnailParentFingerprint.resultOrThrow().classDef
        )
        StoryboardThumbnailFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(targetIndex).registerA

                // Since this is end of the method must replace one line then add the rest.
                addInstructions(
                    targetIndex + 1,
                    """
                        invoke-static {}, $classDescriptor->getSeekbarThumbnailOverrideValue()Z
                        move-result v$targetRegister
                        return v$targetRegister
                        """
                )
                removeInstruction(targetIndex)
            }
        }

        // Hook storyboard renderer url.
        arrayOf(
            PlayerResponseModelGeneralStoryboardRendererFingerprint,
            PlayerResponseModelLiveStreamStoryboardRendererFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val getStoryboardIndex = it.scanResult.patternScanResult!!.endIndex
                    val getStoryboardRegister =
                        getInstruction<OneRegisterInstruction>(getStoryboardIndex).registerA

                    addInstructions(
                        getStoryboardIndex,
                        """
                            invoke-static { v$getStoryboardRegister }, $classDescriptor->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$getStoryboardRegister
                            """
                    )
                }
            }
        }

        // Hook recommended seekbar thumbnails quality level.
        StoryboardRendererDecoderRecommendedLevelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val moveOriginalRecommendedValueIndex = it.scanResult.patternScanResult!!.endIndex
                val originalValueRegister =
                    getInstruction<OneRegisterInstruction>(moveOriginalRecommendedValueIndex).registerA

                addInstructions(
                    moveOriginalRecommendedValueIndex + 1, """
                        invoke-static { v$originalValueRegister }, $classDescriptor->getStoryboardRecommendedLevel(I)I
                        move-result v$originalValueRegister
                        """
                )
            }
        }

        // Hook the recommended precise seeking thumbnails quality level.
        PlayerResponseModelStoryboardRecommendedLevelFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val moveOriginalRecommendedValueIndex = it.scanResult.patternScanResult!!.endIndex
                val originalValueRegister =
                    getInstruction<OneRegisterInstruction>(moveOriginalRecommendedValueIndex).registerA

                addInstructions(
                    moveOriginalRecommendedValueIndex, """
                        invoke-static { v$originalValueRegister }, $classDescriptor->getStoryboardRecommendedLevel(I)I
                        move-result v$originalValueRegister
                        """
                )
            }
        }

        StoryboardRendererSpecFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val storyBoardUrlParams = 0

                addInstructionsWithLabels(
                    0, """
                        if-nez p$storyBoardUrlParams, :ignore
                        invoke-static { p$storyBoardUrlParams }, $classDescriptor->getStoryboardRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object p$storyBoardUrlParams
                        """, ExternalLabel("ignore", getInstruction(0))
                )
            }
        }

        // Hook the seekbar thumbnail decoder and use a NULL spec for live streams.
        StoryboardRendererDecoderSpecFingerprint.resultOrThrow().let {
            val storyBoardUrlIndex = it.scanResult.patternScanResult!!.startIndex + 1
            val storyboardUrlRegister =
                it.mutableMethod.getInstruction<OneRegisterInstruction>(storyBoardUrlIndex).registerA

            it.mutableMethod.addInstructions(
                storyBoardUrlIndex + 1, """
                        invoke-static { v$storyboardUrlRegister }, $classDescriptor->getStoryboardDecoderRendererSpec(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$storyboardUrlRegister
                        """
            )
        }
    }
}
