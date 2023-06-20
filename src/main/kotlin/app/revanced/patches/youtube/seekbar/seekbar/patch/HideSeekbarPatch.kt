package app.revanced.patches.youtube.seekbar.seekbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.seekbar.seekbarcolor.patch.SeekbarColorPatch
import app.revanced.patches.youtube.utils.fingerprints.SeekbarFingerprint
import app.revanced.patches.youtube.utils.fingerprints.SeekbarOnDrawFingerprint
import app.revanced.patches.youtube.utils.annotations.YouTubeCompatibility
import app.revanced.patches.youtube.utils.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.SEEKBAR

@Patch
@Name("hide-seekbar")
@Description("Hides the seekbar in video player and video thumbnails.")
@DependsOn(
    [
        SeekbarColorPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class HideSeekbarPatch : BytecodePatch(
    listOf(SeekbarFingerprint)
) {
    override fun execute(context: BytecodeContext): PatchResult {

        SeekbarFingerprint.result?.mutableClass?.let { mutableClass ->
            SeekbarOnDrawFingerprint.also { it.resolve(context, mutableClass) }.result?.let {
                it.mutableMethod.apply {
                    addInstructionsWithLabels(
                        0, """
                            invoke-static {}, $SEEKBAR->hideSeekbar()Z
                            move-result v0
                            if-eqz v0, :show_seekbar
                            return-void
                            """, ExternalLabel("show_seekbar", getInstruction(0))
                    )
                }
            } ?: return SeekbarOnDrawFingerprint.toErrorResult()
        } ?: return SeekbarFingerprint.toErrorResult()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: SEEKBAR_SETTINGS",
                "SETTINGS: HIDE_SEEKBAR",
                "SETTINGS: HIDE_SEEKBAR_THUMBNAIL"
            )
        )

        SettingsPatch.updatePatchStatus("hide-seekbar")

        return PatchResultSuccess()
    }
}
