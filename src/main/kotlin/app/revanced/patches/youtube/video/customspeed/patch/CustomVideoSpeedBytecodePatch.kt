package app.revanced.patches.youtube.video.customspeed.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.OptionsContainer
import app.revanced.patcher.patch.PatchOption
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultError
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch.Companion.contexts
import app.revanced.patches.youtube.video.customspeed.fingerprints.SpeedArrayGeneratorFingerprint
import app.revanced.patches.youtube.video.customspeed.fingerprints.SpeedLimiterFingerprint
import app.revanced.patches.youtube.video.customspeed.fingerprints.VideoSpeedEntriesFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH
import app.revanced.util.resources.ResourceHelper.addEntries
import app.revanced.util.resources.ResourceHelper.addEntryValues
import app.revanced.util.resources.ResourceUtils.copyXmlNode
import org.jf.dexlib2.builder.instruction.BuilderArrayPayload
import org.jf.dexlib2.iface.instruction.NarrowLiteralInstruction
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.FieldReference
import org.jf.dexlib2.iface.reference.MethodReference

@Patch
@Name("custom-video-speed")
@Description("Adds more video speed options.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class CustomVideoSpeedPatch : BytecodePatch(
    listOf(
        SpeedArrayGeneratorFingerprint,
        SpeedLimiterFingerprint,
        VideoSpeedEntriesFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        val arrayPath = "res/values/arrays.xml"
        val entriesName = "revanced_custom_video_speed_entry"
        val entryValueName = "revanced_custom_video_speed_entry_value"

        val speed = CustomSpeedArrays
            ?: return PatchResultError("Invalid video speed array.")
        val splits = speed.replace(" ","").split(",")
        if (splits.isEmpty()) throw IllegalArgumentException("Invalid speed elements")

        val videoSpeedsArray = splits.map { it.toFloat().toRawBits() }

        SpeedArrayGeneratorFingerprint.result?.let { result ->
            with (result.mutableMethod) {
                val sizeCallIndex = implementation!!.instructions
                    .indexOfFirst { ((it as? ReferenceInstruction)?.reference as? MethodReference)?.name == "size" }

                if (sizeCallIndex == -1) return PatchResultError("Couldn't find call to size()")

                val sizeCallResultRegister =
                    (implementation!!.instructions.elementAt(sizeCallIndex + 1) as OneRegisterInstruction).registerA

                addInstructionsWithLabels(
                    sizeCallIndex + 2,
                    """
                        invoke-static {}, $VIDEO_PATH/VideoSpeedPatch;->isCustomVideoSpeedEnabled()Z
                        move-result v9
                        if-eqz v9, :defaultspeed
                        const/4 v$sizeCallResultRegister, 0x0
                    """, ExternalLabel("defaultspeed", getInstruction(sizeCallIndex + 2))
                )

                val (arrayLengthConstIndex, arrayLengthConst) = implementation!!.instructions.withIndex()
                    .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 7 }

                val arrayLengthConstDestination = (arrayLengthConst as OneRegisterInstruction).registerA

                val videoSpeedsArrayType = "$VIDEO_PATH/VideoSpeedEntries;->videoSpeed:[F"

                addInstructionsWithLabels(
                    arrayLengthConstIndex + 1,
                    """
                        if-eqz v9, :defaultspeed
                        sget-object v$arrayLengthConstDestination, $videoSpeedsArrayType
                        array-length v$arrayLengthConstDestination, v$arrayLengthConstDestination
                    """, ExternalLabel("defaultspeed", getInstruction(arrayLengthConstIndex + 1))
                )

                val (originalArrayFetchIndex, originalArrayFetch) = implementation!!.instructions.withIndex()
                    .first {
                        val reference = ((it.value as? ReferenceInstruction)?.reference as? FieldReference)
                        reference?.definingClass?.contains("PlayerConfigModel") ?: false &&
                                reference?.type == "[F"
                    }

                val originalArrayFetchDestination = (originalArrayFetch as OneRegisterInstruction).registerA

                addInstructionsWithLabels(
                    originalArrayFetchIndex + 1,
                    """
                        if-eqz v9, :defaultspeed
                        sget-object v$originalArrayFetchDestination, $videoSpeedsArrayType
                    """, ExternalLabel("defaultspeed", getInstruction(originalArrayFetchIndex + 1))
                )
            }
        } ?: return SpeedArrayGeneratorFingerprint.toErrorResult()

        SpeedLimiterFingerprint.result?.let { result ->
            with (result.mutableMethod) {
                val (limiterMinConstIndex, limiterMinConst) = implementation!!.instructions.withIndex()
                    .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 0.25f.toRawBits() }
                val (limiterMaxConstIndex, limiterMaxConst) = implementation!!.instructions.withIndex()
                    .first { (it.value as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits() }

                val limiterMinConstDestination = (limiterMinConst as OneRegisterInstruction).registerA
                val limiterMaxConstDestination = (limiterMaxConst as OneRegisterInstruction).registerA

                fun hexFloat(float: Float): String = "0x%08x".format(float.toRawBits())

                replaceInstruction(
                    limiterMinConstIndex,
                    "const/high16 v$limiterMinConstDestination, ${hexFloat(0.0f)}"
                )
                replaceInstruction(
                    limiterMaxConstIndex,
                    "const/high16 v$limiterMaxConstDestination, ${hexFloat(100.0f)}"
                )
            }
        } ?: return SpeedLimiterFingerprint.toErrorResult()

        VideoSpeedEntriesFingerprint.result?.let {
            with (it.mutableMethod) {
                val arrayPayloadIndex = implementation!!.instructions.size - 1

                replaceInstruction(
                    0,
                    "const/16 v0, ${videoSpeedsArray.size}"
                )

                implementation!!.replaceInstruction(
                    arrayPayloadIndex,
                    BuilderArrayPayload(
                        4,
                        videoSpeedsArray
                    )
                )
            }
        } ?: return VideoSpeedEntriesFingerprint.toErrorResult()


        /**
         * Copy arrays
         */
        contexts.copyXmlNode("youtube/customspeed/host", "values/arrays.xml", "resources")

        val speedElements = splits.map { it }
        for (index in 0 until splits.count()) {
            contexts.addEntries(
                arrayPath, speedElements[index] + "x",
                entriesName
            )
            contexts.addEntryValues(
                arrayPath, speedElements[index],
                entryValueName
            )
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: CUSTOM_VIDEO_SPEED"
            )
        )

        SettingsPatch.updatePatchStatus("custom-video-speed")

        return PatchResultSuccess()
    }

    companion object : OptionsContainer() {
        var CustomSpeedArrays: String? by option(
            PatchOption.StringOption(
                key = "CustomSpeedArrays",
                default = "0.25, 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 5.0",
                title = "Custom Speed Values",
                description = "A list of custom video speeds. Be sure to separate them with commas (,)."
            )
        )
    }
}
