package app.revanced.patches.youtube.video.hdr.patch

import app.revanced.extensions.exception
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.youtube.video.hdr.fingerprints.HdrCapabilitiesFingerprint
import app.revanced.util.integrations.Constants.VIDEO_PATH

@Patch
@Name("Disable hdr video")
@Description("Disable HDR video.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
class DisableHdrVideoPatch : BytecodePatch(
    listOf(HdrCapabilitiesFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        HdrCapabilitiesFingerprint.result?.let {
            with(
                context
                    .toMethodWalker(it.method)
                    .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                    .getMethod() as MutableMethod
            ) {
                addInstructionsWithLabels(
                    0, """
                        invoke-static {}, $VIDEO_PATH/HDRVideoPatch;->disableHDRVideo()Z
                        move-result v0
                        if-nez v0, :default
                        return v0
                        """, ExternalLabel("default", getInstruction(0))
                )
            }
        } ?: throw HdrCapabilitiesFingerprint.exception

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: VIDEO_SETTINGS",
                "SETTINGS: DISABLE_HDR_VIDEO"
            )
        )

        SettingsPatch.updatePatchStatus("disable-hdr-video")

    }
}
