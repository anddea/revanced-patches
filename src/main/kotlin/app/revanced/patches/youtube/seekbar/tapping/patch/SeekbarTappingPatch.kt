package app.revanced.patches.youtube.seekbar.tapping.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.tapping.fingerprints.SeekbarTappingFingerprint
import app.revanced.patches.youtube.seekbar.tapping.fingerprints.SeekbarTappingReferenceFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("enable-seekbar-tapping")
@Description("Enables tap-to-seek on the seekbar of the video player.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class SeekbarTappingPatch : BytecodePatch(
    listOf(
        SeekbarTappingReferenceFingerprint,
        SeekbarTappingFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {
        SeekbarTappingReferenceFingerprint.result?.let {
            it.mutableMethod.apply {
                TappingLabel = """
                    invoke-static {}, $SEEKBAR->enableSeekbarTapping()Z
                    move-result v0
                    if-eqz v0, :disabled
                    invoke-virtual { p0, v2 }, ${getInstruction<ReferenceInstruction>(it.scanResult.patternScanResult!!.startIndex).reference}
                    invoke-virtual { p0, v2 }, ${getInstruction<ReferenceInstruction>(it.scanResult.patternScanResult!!.endIndex - 1).reference}
                    """
            }
        } ?: return SeekbarTappingReferenceFingerprint.toErrorResult()

        SeekbarTappingFingerprint.result?.let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex + 2

                addInstructionsWithLabels(
                    insertIndex,
                    TappingLabel,
                    ExternalLabel("disabled", getInstruction(insertIndex))
                )
            }
        } ?: return SeekbarTappingFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: ENABLE_SEEKBAR_TAPPING"
            )
        )

        SettingsPatch.updatePatchStatus("enable-seekbar-tapping")

        return PatchResultSuccess()
    }

    private companion object {
        lateinit var TappingLabel: String
    }
}